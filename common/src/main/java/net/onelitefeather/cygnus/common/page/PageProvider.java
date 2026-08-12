package net.onelitefeather.cygnus.common.page;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.Direction;
import net.minestom.server.utils.validate.Check;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.page.event.PageDiscoveryCompletedEvent;
import net.onelitefeather.cygnus.common.util.Helper;
import net.theevilreaper.aves.util.Broadcaster;
import net.theevilreaper.xerus.api.phase.GamePhase;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static net.onelitefeather.cygnus.common.config.GameConfig.MIN_ACTIVE_PAGE_COUNT;

/**
 * Handles the logic to manage and spawn pages during the {@link GamePhase}.
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 1.0.0
 **/
@SuppressWarnings("java:S3252")
public final class PageProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PageProvider.class);

    private final Queue<PageResource> globalCache;
    private final Map<UUID, PageEntity> activePages;
    private final AtomicInteger currentPageCount;
    private final AtomicInteger currentFoundedPageCount;

    private Component pageStatus = Component.empty();
    private int maxPageAmount;

    public PageProvider() {
        this.globalCache = new ConcurrentLinkedQueue<>();
        this.activePages = new ConcurrentHashMap<>();
        this.maxPageAmount = 0;
        this.currentFoundedPageCount = new AtomicInteger(0);
        this.currentPageCount = new AtomicInteger(1);
    }

    /**
     * Loads the required page data from the given set of {@link PageResource}s.
     * The resources are shuffled once so the queue can be drained without picking a random index on every access.
     *
     * @param resources given set of page resources
     */
    public void loadPageData(Set<PageResource> resources) {
        Check.argCondition(!globalCache.isEmpty(), "Can't load pages twice");

        if (resources.isEmpty()) {
            throw new IllegalStateException("Can't load a map without any pages");
        }
        List<PageResource> shuffled = new ArrayList<>(resources);
        Collections.shuffle(shuffled);
        this.globalCache.addAll(shuffled);
    }

    public void collectStartPages(Instance instance) {
        Check.argCondition(this.globalCache.size() < MIN_ACTIVE_PAGE_COUNT, "Not enough pages to start the game");
        var counter = 0;

        Set<Integer> candidateHashes = new HashSet<>();

        while (counter != MIN_ACTIVE_PAGE_COUNT) {
            var page = this.globalCache.poll();

            if (candidateHashes.add(page.hashCode())) {
                Direction direction = page.face();
                var position = Helper.updatePosition(page.position().asPos(), direction);
                PageEntity entity = PageFactory.createPage(instance, position, direction, this.currentPageCount.getAndIncrement());
                this.activePages.put(entity.getHitBoxUUID(), entity);
                counter++;
                continue;
            }
            this.globalCache.add(page);
        }
        LOGGER.info("This current page count is {}", currentPageCount.get());
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

    /**
     * Spawns all pages that are currently in the active page map.
     */
    public void spawn() {
        this.updatePageDisplay();
        for (Map.Entry<UUID, PageEntity> pointPageEntityEntry : this.activePages.entrySet()) {
            pointPageEntityEntry.getValue().spawn();
        }
    }

    public void cleanUp() {
        if (this.activePages.isEmpty()) return;
        for (UUID uuid : List.copyOf(this.activePages.keySet())) {
            PageEntity value = this.activePages.remove(uuid);
            if (value == null) continue;
            value.disableInteraction();
            value.remove();
        }
    }

    public void triggerTTLHandling(UUID uuid) {
        if (this.globalCache.isEmpty()) {
            PageEntity page = this.activePages.computeIfPresent(uuid, (key, value) -> {
                value.enableInteraction();
                return value;
            });
            if (page == null) {
                LOGGER.debug("Page {} was already claimed when its TTL expired, ignoring", uuid);
            }
            return;
        }

        PageEntity pageEntity = this.removeEntity(uuid);
        if (pageEntity == null) {
            LOGGER.debug("Page {} was already claimed when its TTL expired, ignoring", uuid);
            return;
        }

        PageResource newPos = this.globalCache.poll();
        if (newPos != null) {
            pageEntity.teleport(Helper.updatePosition(newPos.position().asPos(), newPos.face()));
        }
        this.activePages.put(pageEntity.getHitBoxUUID(), pageEntity);
        pageEntity.enableInteraction();
    }

    public void triggerPageFound(Player player, UUID uuid, Consumer<Player> playerUpdater) {
        PageEntity pageEntity = removeEntity(uuid);
        if (pageEntity == null) {
            LOGGER.debug("Page {} was already claimed when {} interacted, ignoring", uuid, player.getUsername());
            return;
        }
        player.getInventory().addItemStack(pageEntity.getPageItem());
        playerUpdater.accept(player);
        Broadcaster.broadcast(Messages.getPageFoundComponent(player));
        int foundCount = this.currentFoundedPageCount.incrementAndGet();
        this.updatePageDisplay();

        if (foundCount >= maxPageAmount) {
            EventDispatcher.call(new PageDiscoveryCompletedEvent());
        }

        // Re-inserting the entity makes it discoverable again, so this must happen last:
        // doing it earlier reopens a window where a concurrent call for the same uuid
        // legitimately re-claims it and double-credits the find.
        updatePageData(pageEntity);
    }

    private void updatePageData(PageEntity entity) {
        PageResource resource = this.globalCache.poll();
        if (resource != null) {
            entity.teleport(Helper.updatePosition(resource.position().asPos(), resource.face()));
        }
        entity.updateItemStack(this.currentPageCount.incrementAndGet());
        this.activePages.put(entity.getHitBoxUUID(), entity);
    }

    private void updatePageDisplay() {
        this.pageStatus = Component.text(this.currentFoundedPageCount.get(), NamedTextColor.GREEN)
                .append(Component.space())
                .append(Component.text("/", NamedTextColor.GRAY))
                .append(Component.space())
                .append(Component.text(this.maxPageAmount, NamedTextColor.RED));
    }

    /**
     * Returns a {@link PageEntity} that matches wit the given id
     *
     * @param uuid pf the entity
     * @return the fetched reference or null
     */
    private @Nullable PageEntity removeEntity(UUID uuid) {
        return this.activePages.remove(uuid);
    }

    /**
     * Returns the {@link Component} which contains a textual representation of the current page status.
     *
     * @return the current page status
     */
    public Component getPageStatus() {
        return pageStatus;
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