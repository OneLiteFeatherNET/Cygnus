package net.onelitefeather.cygnus.common.page;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.validate.Check;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.config.PageSettings;
import net.onelitefeather.cygnus.common.page.event.PageDiscoveryCompletedEvent;
import net.onelitefeather.cygnus.common.page.event.PageFoundEvent;
import net.onelitefeather.cygnus.common.page.event.PageSpawnedEvent;
import net.theevilreaper.aves.util.Broadcaster;
import net.theevilreaper.xerus.api.phase.GamePhase;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

/**
 * Handles the logic to manage and spawn pages during the {@link GamePhase}.
 * <p>
 * The provider owns a pool of free spots. A page that expires hands its spot back to the pool,
 * a page that is found uses its spot up for the rest of the round.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.5.0
 * @since 1.0.0
 **/
@SuppressWarnings("java:S3252")
public final class PageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageProvider.class);

    /** How close a survivor may be to a spot before a moving page looks for another one, in blocks. */
    static final double SPAWN_DISTANCE = 16.0D;

    /** How many spots a moving page tries before it takes the next one, watched or not. */
    static final int SPAWN_ATTEMPTS = 5;

    private final Queue<PageResource> freeSpots = new ConcurrentLinkedQueue<>();
    private final Map<UUID, PageEntity> activePages = new ConcurrentHashMap<>();
    private final AtomicInteger pageNumber = new AtomicInteger(1);
    private final AtomicInteger foundPages = new AtomicInteger();
    private int maxPageAmount;
    private final Supplier<? extends Collection<? extends Point>> survivors;
    private final PageSettings settings;

    /**
     * Creates a provider that does not know where the survivors are, so pages move on to any spot.
     */
    public PageProvider() {
        this(List::of, PageSettings.DEFAULT);
    }

    /**
     * Creates a provider that keeps a moving page away from the survivors.
     *
     * @param survivors supplies where the living survivors stand right now
     */
    public PageProvider(Supplier<? extends Collection<? extends Point>> survivors) {
        this(survivors, PageSettings.DEFAULT);
    }

    /**
     * Creates a provider that keeps a moving page away from the survivors and uses the page values
     * the map sets for itself.
     *
     * @param survivors supplies where the living survivors stand right now
     * @param settings  the page values of the map
     */
    public PageProvider(Supplier<? extends Collection<? extends Point>> survivors, PageSettings settings) {
        this.survivors = survivors;
        this.settings = settings;
    }

    /**
     * Returns the page values of the map this provider places pages on.
     *
     * @return the page settings
     */
    public PageSettings settings() {
        return this.settings;
    }

    /**
     * Loads the required page data from the given set of {@link PageResource}s.
     * The resources are shuffled once so the queue can be drained without picking a random index on every access.
     * Spots in crowded parts of the map tend to come later, see {@link #spreadOrder(Collection, RandomGenerator)}.
     *
     * @param resources given set of page resources
     */
    public void loadPageData(Set<PageResource> resources) {
        Check.argCondition(!this.freeSpots.isEmpty(), "Can't load pages twice");

        if (resources.isEmpty()) {
            throw new IllegalStateException("Can't load a map without any pages");
        }
        this.freeSpots.addAll(spreadOrder(resources, ThreadLocalRandom.current()));
    }

    /**
     * Shuffles the spots so that crowded parts of the map do not get more pages than the rest.
     * <p>
     * An even shuffle gives every spot the same chance, so a part of the map with many spots draws
     * pages over and over while the pages keep moving on. Here a spot comes earlier the more it
     * weighs, see {@link #weights(Collection)}. Every spot still comes exactly once.
     * </p>
     *
     * @param spots  the spots of the map
     * @param random the random source
     * @return the spots in the order they are handed out
     */
    static List<PageResource> spreadOrder(Collection<PageResource> spots, RandomGenerator random) {
        Map<PageResource, Double> weights = PageCrowding.weights(spots);
        Map<PageResource, Double> keys = new HashMap<>();
        for (PageResource spot : spots) {
            // A weighted random order: the heavier a spot, the closer its key gets to 1.
            keys.put(spot, Math.pow(random.nextDouble(), 1.0D / weights.get(spot)));
        }
        List<PageResource> order = new ArrayList<>(spots);
        order.sort(Comparator.comparingDouble((PageResource spot) -> keys.get(spot)).reversed());
        return order;
    }

    /**
     * Picks the spots for the pages that are active when a round starts and creates the pages on them.
     * The pages only appear in the world once {@link #spawn(Instance)} is called.
     *
     * @param activePageCount how many pages to keep concurrently active, e.g. from {@link PageCalculation#calculateActivePageAmount(int)}
     */
    public void collectStartPages(int activePageCount) {
        Check.argCondition(this.freeSpots.size() < activePageCount, "Not enough pages to start the game");
        for (int i = 0; i < activePageCount; i++) {
            PageEntity page = PageFactory.createPage(this.freeSpots.poll(), this.pageNumber.getAndIncrement(), this.settings.ttlSeconds());
            this.activePages.put(page.getHitBoxUUID(), page);
        }
        LOGGER.info("Collected {} start pages", activePageCount);
    }

    /**
     * Places every collected page into the given instance.
     *
     * @param instance the instance the round is played in
     */
    public void spawn(Instance instance) {
        for (PageEntity page : this.activePages.values()) {
            page.place(instance);
            EventDispatcher.call(new PageSpawnedEvent(page.getHitBoxUUID(), page.spot(), false));
        }
    }

    /**
     * Sets the max page amount.
     *
     * @param maxPageAmount to set
     */
    public void setMaxPageAmount(int maxPageAmount) {
        if (this.maxPageAmount != 0) {
            throw new IllegalStateException("The max page amount can't be set twice");
        }
        this.maxPageAmount = maxPageAmount;
    }

    public void cleanUp() {
        for (UUID uuid : List.copyOf(this.activePages.keySet())) {
            PageEntity page = this.activePages.remove(uuid);
            if (page == null) continue;
            page.disableInteraction();
            page.remove();
        }
    }

    public void triggerTTLHandling(UUID uuid) {
        PageEntity page = this.activePages.remove(uuid);
        if (page == null) {
            LOGGER.debug("Page {} was already claimed when its TTL expired, ignoring", uuid);
            return;
        }
        this.relocate(page, true);
    }

    public boolean triggerPageFound(Player player, UUID uuid) {
        PageEntity page = this.activePages.get(uuid);
        // A hidden page still has a hit box, so a click on it must not count
        if (page == null || !page.isInteractable() || !this.activePages.remove(uuid, page)) {
            LOGGER.debug("Page {} was already claimed or is hidden when {} interacted, ignoring", uuid, player.getUsername());
            return false;
        }
        player.getInventory().addItemStack(page.getPageItem());
        Broadcaster.broadcast(Messages.getPageFoundComponent(player));
        int foundCount = this.foundPages.incrementAndGet();
        EventDispatcher.call(new PageFoundEvent(player, foundCount, this.maxPageAmount, uuid));

        if (foundCount >= this.maxPageAmount) {
            EventDispatcher.call(new PageDiscoveryCompletedEvent());
        }

        page.updateItemStack(this.pageNumber.incrementAndGet());
        // Re-inserting the page makes it discoverable again, so this must happen last:
        // doing it earlier reopens a window where a concurrent call for the same uuid
        // legitimately re-claims it and double-credits the find.
        this.relocate(page, false);
        return true;
    }

    /**
     * Moves a page that was taken out of play to a free spot and puts it back into play.
     * Without a free spot the page stays where it is; a found page is hidden for a while first.
     *
     * @param page          the page to move
     * @param returnOldSpot whether the spot the page leaves goes back into the pool
     */
    private void relocate(PageEntity page, boolean returnOldSpot) {
        PageResource oldSpot = page.getResource();
        // Picked first, so the page can't draw its own spot again; queued last, so the spot only
        // comes back once every other one had its turn.
        SpotPick pick = pickSpot(this.freeSpots, List.copyOf(this.survivors.get()));
        PageResource newSpot = pick.spot();
        boolean hidden = newSpot == null && !returnOldSpot;
        if (hidden) {
            // Found with nowhere else to go: the spot has to be reused, but not right away
            page.hideFor(respawnDelay());
        } else {
            if (newSpot != null) {
                page.moveTo(newSpot);
                if (returnOldSpot) {
                    this.freeSpots.add(oldSpot);
                }
            }
            // Shows the current item and restarts the TTL: on its spot the page counts as a fresh one
            page.enableInteraction();
        }
        this.activePages.put(page.getHitBoxUUID(), page);
        // A hidden page is announced when it shows up again, see PageEntity#reappear
        if (!hidden) {
            EventDispatcher.call(new PageSpawnedEvent(page.getHitBoxUUID(), page.spot(), true, pick.skipped(), pick.watched()));
        }
    }

    /**
     * Takes the next free spot that no survivor stands next to. A page that pops up beside a survivor
     * is found without a search, and seen appearing out of nowhere. A spot that is too close goes to
     * the end of the queue. After {@link #SPAWN_ATTEMPTS} tries the next spot is taken anyway, so a
     * page never stays away because the survivors stand all over the map.
     * <p>
     * It runs only when a page moves, and costs a few distance checks against a handful of survivors.
     * </p>
     *
     * @param free      the free spots, in the order they are handed out
     * @param survivors where the living survivors stand
     * @return the spot, without one if no spot is free, and how the pick went for the telemetry
     */
    static SpotPick pickSpot(Queue<PageResource> free, List<? extends Point> survivors) {
        double reach = SPAWN_DISTANCE * SPAWN_DISTANCE;
        for (int attempt = 1; attempt <= SPAWN_ATTEMPTS; attempt++) {
            PageResource spot = free.poll();
            if (spot == null) return new SpotPick(null, attempt - 1, false);
            boolean watched = isWatched(spot, survivors, reach);
            if (!watched || attempt == SPAWN_ATTEMPTS) return new SpotPick(spot, attempt - 1, watched);
            free.add(spot);
        }
        return new SpotPick(null, SPAWN_ATTEMPTS, false);
    }

    /**
     * The spot a moving page takes.
     *
     * @param spot    the spot, or {@code null} if no spot is free
     * @param skipped how many spots were passed over because a survivor stood next to them
     * @param watched whether a survivor stands next to the spot anyway
     */
    record SpotPick(@Nullable PageResource spot, int skipped, boolean watched) {
    }

    private static boolean isWatched(PageResource spot, List<? extends Point> survivors, double reach) {
        for (Point survivor : survivors) {
            if (spot.position().distanceSquared(survivor) < reach) return true;
        }
        return false;
    }

    private int respawnDelay() {
        int jitter = GameConfig.PAGE_RESPAWN_DELAY_JITTER;
        return this.settings.respawnDelaySeconds() + ThreadLocalRandom.current().nextInt(-jitter, jitter + 1);
    }

    /**
     * Returns the list of every page entity a player could currently walk up to and collect.
     *
     * @return the list of collectible page entities
     * @since 2.15.0
     */
    public List<PageEntity> interactablePages() {
        List<PageEntity> pages = new ArrayList<>(this.activePages.size());
        for (PageEntity page : this.activePages.values()) {
            if (page.isInteractable()) {
                pages.add(page);
            }
        }
        return pages;
    }

    /**
     * Returns the {@link Component} which contains a textual representation of the current page status.
     *
     * @return the current page status
     */
    public Component getPageStatus() {
        // Built on every call: a cached copy written by concurrent finds could end up with a stale count
        return Component.text(this.foundPages.get(), NamedTextColor.GREEN)
                .append(Component.space())
                .append(Component.text("/", NamedTextColor.GRAY))
                .append(Component.space())
                .append(Component.text(this.maxPageAmount, NamedTextColor.RED));
    }

    /**
     * Returns the number of pages found in this round.
     *
     * @return the number of found pages, never negative
     */
    public int foundPageCount() {
        return this.foundPages.get();
    }

    /**
     * Returns how much of all pages has been found in this round.
     *
     * @return the found part, between {@code 0} and {@code 1}; {@code 0} while the max page amount is unset
     */
    public double foundShare() {
        if (this.maxPageAmount <= 0) return 0.0D;
        return Math.clamp((double) this.foundPages.get() / this.maxPageAmount, 0.0D, 1.0D);
    }

    /**
     * Returns the max page amount.
     *
     * @return max page amount
     */
    public int getMaxPageAmount() {
        return maxPageAmount;
    }
}
