package net.onelitefeather.cygnus.game;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.entity.metadata.other.InteractionMeta;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.chunk.ChunkUtils;
import net.onelitefeather.cygnus.event.PageEvent;
import net.onelitefeather.cygnus.config.GameConfig;
import net.onelitefeather.cygnus.utils.Helper;
import net.onelitefeather.cygnus.utils.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
@SuppressWarnings("java:S3252")
public final class PageEntity extends Entity {

    private static final Vec HALF_BLOCK = new Vec(0, 0.5, 0);
    private static final long ADDITION_TIME = 1000L;
    private final Entity hitBox;
    private final ItemStack pageItem;
    private int ttlTime;
    private int currentTickTime;
    private long nextTick;
    private boolean send;

    PageEntity(@NotNull Instance instance, @NotNull Pos spawnPos, int pageCount) {
        super(EntityType.ITEM_DISPLAY);
        this.setInstance(instance, spawnPos);
        this.hitBox = new Entity(EntityType.INTERACTION);
        this.pageItem = ItemStack.builder(Material.PAPER).customName(Component.text("Page: " + pageCount)).build();
        this.ttlTime = Helper.calculateOffsetTime(GameConfig.PAGE_TTL_TIME);

        ItemDisplayMeta itemDisplayMeta = (ItemDisplayMeta) this.getEntityMeta();
        itemDisplayMeta.setItemStack(this.pageItem);
        itemDisplayMeta.setDisplayContext(ItemDisplayMeta.DisplayContext.FIXED);
        itemDisplayMeta.setHeight(1);
        itemDisplayMeta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.FIXED);
        itemDisplayMeta.setWidth(0.1f);
        itemDisplayMeta.setHeight(0.1f);

        this.calculateNextTick();
        this.setAutoViewable(true);
        InteractionMeta interactionMeta = (InteractionMeta) this.hitBox.getEntityMeta();
        interactionMeta.setNotifyAboutChanges(false);
        interactionMeta.setHasNoGravity(true);
        interactionMeta.setInvisible(false);
        interactionMeta.setHasGlowingEffect(true);
        interactionMeta.setHeight(1.0f);
        interactionMeta.setWidth(1.0f);
        interactionMeta.setResponse(true);
        interactionMeta.setNotifyAboutChanges(true);
        this.hitBox.setInstance(instance, spawnPos.sub(HALF_BLOCK));
        this.hitBox.setAutoViewable(true);
        this.hitBox.setTag(Tags.PAGE_TAG, this.hitBox.getUuid());
    }

    public void disableInteraction() {
        ItemDisplayMeta itemDisplayMeta = (ItemDisplayMeta) this.getEntityMeta();
        itemDisplayMeta.setItemStack(ItemStack.AIR);
        itemDisplayMeta.setInvisible(true);

        InteractionMeta interactionMeta = (InteractionMeta) this.hitBox.getEntityMeta();
        interactionMeta.setResponse(false);
        interactionMeta.setInvisible(true);

        this.currentTickTime = 0;
        this.nextTick = System.currentTimeMillis() + ADDITION_TIME;
    }

    public void enableInteraction(@Nullable ItemStack stack) {
        ItemDisplayMeta itemDisplayMeta = (ItemDisplayMeta) this.getEntityMeta();
        if (stack != null) {
            itemDisplayMeta.setItemStack(stack);
        }
        itemDisplayMeta.setInvisible(false);

        InteractionMeta interactionMeta = (InteractionMeta) this.hitBox.getEntityMeta();
        interactionMeta.setResponse(true);
        interactionMeta.setInvisible(false);
        this.ttlTime = Helper.calculateOffsetTime(GameConfig.PAGE_TTL_TIME);
        this.send = false;
    }

    @Override
    public void remove() {
        super.remove();
        this.hitBox.remove();
    }

    @Override
    public @NotNull CompletableFuture<Void> teleport(@NotNull Pos position) {
        this.hitBox.teleport(position.sub(HALF_BLOCK));
        return super.teleport(position);
    }

    @Override
    public void tick(long time) {
        if (instance == null || isRemoved() || send || !ChunkUtils.isLoaded(currentChunk)) {
            return;
        }

        if (System.currentTimeMillis() >= nextTick) {
            ++currentTickTime;
            this.calculateNextTick();
        }

        if (currentTickTime >= ttlTime) {
            this.disableInteraction();
            send = true;
            EventDispatcher.call(new PageEvent(this, PageEvent.Reason.TTL));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PageEntity that = (PageEntity) o;

        return super.equals(that);
    }

    /**
     * Calculates the next tick for the entity.
     * The next tick is the current timestamp plus an offset time.
     */
    private void calculateNextTick() {
        this.nextTick = System.currentTimeMillis() + ADDITION_TIME;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public @NotNull ItemStack getPageItem() {
        return pageItem;
    }

    public @NotNull UUID getHitBoxUUID() {
        return this.hitBox.getUuid();
    }
}
