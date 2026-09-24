package net.onelitefeather.cygnus.common.page;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.validate.Check;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.page.event.PageDiscoveryCompletedEvent;
import net.onelitefeather.cygnus.common.page.event.PageFoundEvent;
import net.theevilreaper.aves.util.Broadcaster;
import net.theevilreaper.xerus.api.phase.GamePhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Handles the logic to manage and spawn pages during the {@link GamePhase}.
 * <p>
 * The provider owns a pool of free spots. A page that expires hands its spot back to the pool,
 * a page that is found uses its spot up for the rest of the round.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.3.0
 * @since 1.0.0
 **/
@SuppressWarnings("java:S3252")
public final class PageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageProvider.class);

    private final Queue<PageResource> freeSpots = new ConcurrentLinkedQueue<>();
    private final Map<UUID, PageEntity> activePages = new ConcurrentHashMap<>();
    private final AtomicInteger pageNumber = new AtomicInteger(1);
    private final AtomicInteger foundPages = new AtomicInteger();
    private int maxPageAmount;

    /**
     * Loads the required page data from the given set of {@link PageResource}s.
     * The resources are shuffled once so the queue can be drained without picking a random index on every access.
     *
     * @param resources given set of page resources
     */
    public void loadPageData(Set<PageResource> resources) {
        Check.argCondition(!this.freeSpots.isEmpty(), "Can't load pages twice");

        if (resources.isEmpty()) {
            throw new IllegalStateException("Can't load a map without any pages");
        }
        List<PageResource> shuffled = new ArrayList<>(resources);
        Collections.shuffle(shuffled);
        this.freeSpots.addAll(shuffled);
    }

    /**
     * Picks the spots for the pages that are active when a round starts and creates the pages on them.
     * The pages only appear in the world once {@link #spawn(Instance)} is called.
     *
     * @param activePageCount how many pages to keep concurrently active, e.g. from {@link PageCalculation#calculateActivePageAmount()}
     */
    public void collectStartPages(int activePageCount) {
        Check.argCondition(this.freeSpots.size() < activePageCount, "Not enough pages to start the game");
        for (int i = 0; i < activePageCount; i++) {
            PageEntity page = PageFactory.createPage(this.freeSpots.poll(), this.pageNumber.getAndIncrement());
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
        EventDispatcher.call(new PageFoundEvent(player, foundCount, this.maxPageAmount));

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
        // Polled first, so the page can't draw its own spot again; queued last, so the spot only
        // comes back once every other one had its turn.
        PageResource newSpot = this.freeSpots.poll();
        if (newSpot == null && !returnOldSpot) {
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
    }

    private static int respawnDelay() {
        int jitter = GameConfig.PAGE_RESPAWN_DELAY_JITTER;
        return GameConfig.PAGE_RESPAWN_DELAY + ThreadLocalRandom.current().nextInt(-jitter, jitter + 1);
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
        return this.currentFoundedPageCount.get();
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
