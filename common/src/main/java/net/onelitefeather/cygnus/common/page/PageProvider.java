package net.onelitefeather.cygnus.common.page;

import de.icevizion.aves.util.Broadcaster;
import de.icevizion.xerus.api.phase.LinearPhaseSeries;
import de.icevizion.xerus.api.phase.TimedPhase;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.validate.Check;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.util.DirectionFaceHelper;
import net.onelitefeather.cygnus.common.util.Helper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static net.onelitefeather.cygnus.common.config.GameConfig.MIN_PAGE_COUNT;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
@SuppressWarnings("java:S3252")
public final class PageProvider {

    private static final Lock CACHE_LOCK = new ReentrantLock();
    private static final Lock PAGE_LOCK = new ReentrantLock();
    private final LinearPhaseSeries<TimedPhase> phaseSeries;
    private final List<PageResource> globalCache;
    private final Map<UUID, PageEntity> activePages;
    private final Map<UUID, PageResource> usedResources;
    private int maxPageAmount;
    private int currentPageCount;

    private Component pageStatus = Component.empty();

    public PageProvider(@NotNull LinearPhaseSeries<TimedPhase> phaseSeries) {
        this.phaseSeries = phaseSeries;
        this.globalCache = new ArrayList<>();
        this.usedResources = new HashMap<>();
        this.activePages = new HashMap<>();
        this.maxPageAmount = 0;
        this.currentPageCount = 0;
    }

    public void loadPageData(@NotNull Set<PageResource> positions) {
        Check.argCondition(!globalCache.isEmpty(), "Can't load pages twice");

        if (positions.isEmpty()) {
            throw new IllegalStateException(("Can't load a map without any pages"));
        }
        this.globalCache.addAll(positions);
    }

    // Find start pages
    public void collectStartPages(@NotNull Instance instance) {
        Check.argCondition(this.globalCache.size() < MIN_PAGE_COUNT, "Not enough pages to start the game");
        var counter = 0;

        Set<Integer> candidateHashes = new HashSet<>();

        while (counter != MIN_PAGE_COUNT) {
            var randomIndex = Helper.getRandomInt(this.globalCache.size());
            var page = this.globalCache.remove(randomIndex);

            if (candidateHashes.add(page.hashCode())) {
                var direction = DirectionFaceHelper.parseDirection(page.face());
                var position = Helper.updatePosition(Pos.fromPoint(page.position()), direction);
                PageEntity entity = PageFactory.createPage(instance, position, direction, counter + 1);
                this.activePages.put(entity.getHitBoxUUID(), entity);
                this.usedResources.put(entity.getHitBoxUUID(), page);
                counter++;
                continue;
            }
            this.globalCache.add(page);
        }
    }

    public void spawn() {
        if (this.maxPageAmount == 0) {
            this.maxPageAmount = (MinecraftServer.getConnectionManager().getOnlinePlayers().size()) * 2;
            this.updatePageDisplay();
        }
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
        if (!this.activePages.isEmpty()) {
            for (PageEntity value : this.activePages.values()) {
                value.remove();
            }
            this.activePages.clear();
        }
    }

    public void triggerTTLHandling(@NotNull UUID uuid) {
        if (this.globalCache.isEmpty()) {
            this.activePages.get(uuid).enableInteraction(null);
            return;
        }
        PageEntity pageEntity = this.removeEntity(uuid);
        try {
            CACHE_LOCK.lock();
            var newPos = this.globalCache.remove(Helper.getRandomInt(this.globalCache.size()));
            pageEntity.teleport(Helper.updatePosition(Pos.fromPoint(newPos.position()), DirectionFaceHelper.parseDirection(newPos.face())));
            setPageItem(pageEntity);
            activePages.put(pageEntity.getHitBoxUUID(), pageEntity);
            var resource = this.usedResources.remove(pageEntity.getHitBoxUUID());
            this.globalCache.add(resource);
            this.usedResources.put(pageEntity.getHitBoxUUID(), newPos);
        } finally {
            CACHE_LOCK.unlock();
        }
    }

    public void triggerPageFound(@NotNull Player player, @NotNull UUID uuid) {
        var pageEntity = removeEntity(uuid);
        player.getInventory().addItemStack(pageEntity.getPageItem());
        Broadcaster.broadcast(Messages.getPageFoundComponent(player));
        ++this.currentPageCount;
        this.updatePageDisplay();

        updatePageData(pageEntity);/*
        if (this.currentPageCount >= maxPageAmount) {
            var gamePhase = (GamePhase) this.phaseSeries.getCurrentPhase();
            gamePhase.setFinishEvent(new GameFinishEvent(GameFinishEvent.Reason.ALL_PAGES_FOUND));
            gamePhase.finish();
            return;
        }*/
    }

    private void updatePageData(@NotNull PageEntity entity) {
        PageResource resource;
        try {
            CACHE_LOCK.lock();
            resource = this.globalCache.remove(Helper.getRandomInt(this.globalCache.size()));
        } finally {
            CACHE_LOCK.unlock();
        }
        entity.teleport(Helper.updatePosition(Pos.fromPoint(resource.position()), DirectionFaceHelper.parseDirection(resource.face())));
        setPageItem(entity);
        try {
            PAGE_LOCK.lock();
            this.activePages.put(entity.getHitBoxUUID(), entity);
        } finally {
            PAGE_LOCK.unlock();
        }
    }

    private void updatePageDisplay() {
        this.pageStatus = Component.text( this.currentPageCount + "/" + this.maxPageAmount);
    }

    private @NotNull PageEntity removeEntity(@NotNull UUID uuid) {
        try {
            PAGE_LOCK.lock();
            return this.activePages.remove(uuid);
        } finally {
            PAGE_LOCK.unlock();
        }
    }

    private void setPageItem(@NotNull PageEntity pageEntity) {
        var stack = ItemStack.builder(Material.PAPER).customName(Component.text("Page: " + currentPageCount + 1)).build();
        pageEntity.enableInteraction(stack);
    }

    public @NotNull Component getPageStatus() {
        return pageStatus;
    }

    public int getMaxPageAmount() {
        return maxPageAmount;
    }
}
