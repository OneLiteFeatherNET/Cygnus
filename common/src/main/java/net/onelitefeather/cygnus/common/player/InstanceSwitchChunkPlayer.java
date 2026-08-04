package net.onelitefeather.cygnus.common.player;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.play.UnloadChunkPacket;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * A {@link Player} which keeps chunks from disappearing when it is moved to another instance.
 *
 * <p>Since 26.2 a client which processes an {@code UnloadChunkPacket} and the {@code ChunkDataPacket}
 * of the same chunk within one frame renders that chunk invisible until it is unloaded and loaded
 * again (<a href="https://bugs.mojang.com/browse/MC/issues/MC-310041">MC-310041</a>). An instance
 * switch does exactly that for every chunk the old and the new view have in common, which is all of
 * them when both instances are entered at the same position.</p>
 *
 * <p>Minestom fixes this in
 * <a href="https://github.com/Minestom/Minestom/pull/3308">PR #3308</a> by not sending the unload
 * packet for those chunks. The fix sits in {@code Player#spawnPlayer}, which is private, so it
 * cannot be overridden — but the unload packets travel through {@link #sendPacket(SendablePacket)},
 * which can. This class therefore drops the same packets the upstream fix never sends: while an
 * instance switch is in flight, an unload for a chunk inside the target view is discarded.</p>
 *
 * <p>The {@code PlayerChunkUnloadEvent} is still dispatched for those chunks — the server side of
 * the unload is untouched, exactly as upstream. What this cannot reproduce is the second half of the
 * fix: chunks the client holds around its <em>old</em> position which fall outside the new view are
 * not unloaded, because the unpatched server never sends an unload for them in the first place.
 * Those are the chunks upstream describes as briefly visible after the switch.</p>
 *
 * <p><b>Remove this class</b> once the server runs a Minestom build that contains PR #3308 — as of
 * {@code 2026.07.22-26.2} the newest published build predates the merge. Upstream itself intends to
 * revert the workaround for 26.3, where the client bug is fixed. When that happens, let the
 * subclasses extend {@link PermissionAwarePlayer} directly — the permission pointer it installs is
 * unrelated to this workaround and must survive its removal.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.6.7
 */
public abstract class InstanceSwitchChunkPlayer extends PermissionAwarePlayer {

    private volatile @Nullable TargetView targetView;

    /**
     * Creates a new player for the given connection.
     *
     * @param playerConnection the connection of the player
     * @param gameProfile      the profile of the player
     */
    protected InstanceSwitchChunkPlayer(PlayerConnection playerConnection, GameProfile gameProfile) {
        super(playerConnection, gameProfile);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Remembers which chunks the player is about to see, so {@link #sendPacket(SendablePacket)}
     * can tell the unload packets of the switch apart from the ones a moving player produces.</p>
     */
    @Override
    public CompletableFuture<Void> setInstance(Instance instance, Pos spawnPosition) {
        this.targetView = new TargetView(
                spawnPosition.chunkX(),
                spawnPosition.chunkZ(),
                targetViewDistance(instance)
        );

        CompletableFuture<Void> future;
        try {
            future = super.setInstance(instance, spawnPosition);
        } catch (RuntimeException exception) {
            // A rejected switch never sends a packet, so the filter has to go right back off
            this.targetView = null;
            throw exception;
        }

        // The original future is returned rather than the one whenComplete creates, because its
        // join() carries the deadlock guard Minestom puts on it
        future.whenComplete((unused, throwable) -> this.targetView = null);
        return future;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Drops the unload packets of an ongoing instance switch for chunks the player keeps seeing.</p>
     */
    @Override
    public void sendPacket(SendablePacket packet) {
        TargetView view = this.targetView;
        if (view != null
                && packet instanceof UnloadChunkPacket unloadChunkPacket
                && view.contains(unloadChunkPacket.chunkX(), unloadChunkPacket.chunkZ())) {
            return;
        }
        super.sendPacket(packet);
    }

    /**
     * Returns the view distance the player will have in the given instance.
     *
     * <p>This is what {@code Player#effectiveViewDistance} computes, except that it asks the
     * instance the player is moving to instead of the one it is still in.</p>
     *
     * @param instance the instance the player is moving to
     * @return the effective chunk view distance in that instance
     */
    private int targetViewDistance(Instance instance) {
        return Math.min(getSettings().viewDistance(), instance.viewDistance()) + 1;
    }

    /**
     * The chunk area a player is about to see, as a centre and a radius in chunks.
     *
     * @param chunkX       the chunk x of the target position
     * @param chunkZ       the chunk z of the target position
     * @param viewDistance the effective view distance in the target instance
     */
    private record TargetView(int chunkX, int chunkZ, int viewDistance) {

        /**
         * Returns whether the given chunk lies inside this view.
         *
         * @param x the chunk x to check
         * @param z the chunk z to check
         * @return true if the chunk is inside the view
         */
        boolean contains(int x, int z) {
            return Math.abs(x - this.chunkX) <= this.viewDistance
                    && Math.abs(z - this.chunkZ) <= this.viewDistance;
        }
    }
}
