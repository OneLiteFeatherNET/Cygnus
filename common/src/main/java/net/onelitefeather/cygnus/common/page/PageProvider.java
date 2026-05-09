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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    private static final Lock CACHE_LOCK = new ReentrantLock();
    private static final Lock PAGE_LOCK = new ReentrantLock();
    private final List<PageResource> globalCache;
    private final Map<UUID, PageEntity> activePages;
    private final Map<UUID, PageResource> usedResources;
    private int maxPageAmount;
    private int currentPageCount;
    private int currentFoundedPageCount;

    private Component pageStatus = Component.empty();

    public PageProvider() {
        this.globalCache = new ArrayList<>();
        this.usedResources = new HashMap<>();
        this.activePages = new HashMap<>();
        this.maxPageAmount = 0;
        this.currentFoundedPageCount = 0;
        this.currentPageCount = 1;
    }

    /**
     * Loads the required page data from the given set of {@link PageResource}s.
     *
     * @param resources given set of page resources
     */
    public void loadPageData(Set<PageResource> resources) {
        Check.argCondition(!globalCache.isEmpty(), "Can't load pages twice");

        if (resources.isEmpty()) {
            throw new IllegalStateException("Can't load a map without any pages");
        }
        this.globalCache.addAll(resources);
    }

    public void collectStartPages(Instance instance) {
        Check.argCondition(this.globalCache.size() < MIN_ACTIVE_PAGE_COUNT, "Not enough pages to start the game");
        var counter = 0;

        Set<Integer> candidateHashes = new HashSet<>();

        while (counter != MIN_ACTIVE_PAGE_COUNT) {
            var randomIndex = Helper.getRandomInt(this.globalCache.size());
            var page = this.globalCache.remove(randomIndex);

            if (candidateHashes.add(page.hashCode())) {
                Direction direction = page.face();
                var position = Helper.updatePosition(page.position().asPos(), direction);
                PageEntity entity = PageFactory.createPage(instance, position, direction, this.currentPageCount++);
                this.activePages.put(entity.getHitBoxUUID(), entity);
                this.usedResources.put(entity.getHitBoxUUID(), page);
                counter++;
                continue;
            }
            this.globalCache.add(page);
        }
        LOGGER.info("This current page count is {}", currentPageCount);
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
        try {
            PAGE_LOCK.lock();
            for (Map.Entry<UUID, PageEntity> pointPageEntityEntry : this.activePages.entrySet()) {
                pointPageEntityEntry.getValue().spawn();
            }
        } finally {
            PAGE_LOCK.unlock();
        }
    }

    public void cleanUp() {
        if (this.activePages.isEmpty()) return;
        for (PageEntity value : this.activePages.values()) {
            value.disableInteraction();
            value.remove();
        }
        this.activePages.clear();
    }

    public void triggerTTLHandling(UUID uuid) {
        if (this.globalCache.isEmpty()) {
            this.activePages.get(uuid).enableInteraction();
            return;
        }

        PageEntity pageEntity = this.removeEntity(uuid);

        PageResource newPos;
        try {
            CACHE_LOCK.lock();
            newPos = this.globalCache.remove(Helper.getRandomInt(this.globalCache.size()));
            var resource = this.usedResources.remove(pageEntity.getHitBoxUUID());
            this.usedResources.put(pageEntity.getHitBoxUUID(), newPos);
            this.globalCache.add(resource);
        } finally {
            CACHE_LOCK.unlock();
        }

        pageEntity.teleport(Helper.updatePosition(newPos.position().asPos(), newPos.face()));

        try {
            PAGE_LOCK.lock();
            this.activePages.put(pageEntity.getHitBoxUUID(), pageEntity);
        } finally {
            PAGE_LOCK.unlock();
        }

        pageEntity.enableInteraction();
    }

    public void triggerPageFound(Player player, UUID uuid) {
        var pageEntity = removeEntity(uuid);
        player.getInventory().addItemStack(pageEntity.getPageItem());
        Broadcaster.broadcast(Messages.getPageFoundComponent(player));
        ++this.currentFoundedPageCount;
        this.updatePageDisplay();

        updatePageData(pageEntity);

        if (this.currentFoundedPageCount >= maxPageAmount) {
            EventDispatcher.call(new PageDiscoveryCompletedEvent());
        }
    }

    private void updatePageData(PageEntity entity) {
        PageResource resource;
        try {
            CACHE_LOCK.lock();
            resource = this.globalCache.remove(Helper.getRandomInt(this.globalCache.size()));
        } finally {
            CACHE_LOCK.unlock();
        }
        entity.teleport(Helper.updatePosition(resource.position().asPos(), resource.face()));
        entity.updateItemStack(++this.currentPageCount);
        try {
            PAGE_LOCK.lock();
            this.activePages.put(entity.getHitBoxUUID(), entity);
        } finally {
            PAGE_LOCK.unlock();
        }
    }

    private void updatePageDisplay() {
        this.pageStatus = Component.text(this.currentFoundedPageCount, NamedTextColor.GREEN)
                .append(Component.space())
                .append(Component.text("/", NamedTextColor.GRAY))
                .append(Component.space())
                .append(Component.text(this.maxPageAmount, NamedTextColor.RED));
    }

    private PageEntity removeEntity(UUID uuid) {
        try {
            PAGE_LOCK.lock();
            return this.activePages.remove(uuid);
        } finally {
            PAGE_LOCK.unlock();
        }
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