package net.onelitefeather.cygnus.setup.atmosphere;

import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.world.DimensionType;
import net.onelitefeather.cygnus.common.dimension.DimensionFactory;
import net.onelitefeather.cygnus.common.dimension.MapAtmosphere;
import net.onelitefeather.falco.anvil.FalcoAnvilLoader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows a builder what a set of atmosphere values actually looks like on their map.
 *
 * <p>A dimension's environment reaches a client only with the registry data sent during its
 * configuration phase, and an instance's dimension is fixed when the instance is created. Previewing
 * therefore means: register a throwaway dimension, build a throwaway instance on the same world
 * directory, and walk the builder back through a configuration phase into it. Two loading screens
 * per preview, one in and one out.</p>
 *
 * <p>The preview deliberately runs in its own instance rather than rebuilding the setup instance
 * around the new dimension. The setup instance owns live state - an open {@link FalcoAnvilLoader}
 * among it - that a failed swap would corrupt, and a builder judging fog does not need their
 * markers to be present while they do it.</p>
 *
 * <p>Each preview costs one registry entry that cannot be taken back: removing from a
 * {@code DynamicRegistry} is gated behind {@code -Dminestom.registry.unsafe-ops}, which is not worth
 * enabling server-wide to tidy up after a dialog. The entries are a few hundred bytes each, so this
 * only warns once a session has produced an unreasonable number of them.</p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.3
 */
public final class AtmospherePreviewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AtmospherePreviewService.class);

    /** Number of previews after which a session is considered to be churning the registry. */
    private static final int PREVIEW_WARNING_THRESHOLD = 50;

    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    /**
     * Sends the player into a fresh preview of the given atmosphere.
     *
     * <p>Calling this while a preview is already running replaces it: the origin recorded by the
     * first call is kept, so {@link #leave(Player)} still returns the builder to where they
     * started rather than to the previous preview.</p>
     *
     * @param player     the builder to show the preview to
     * @param atmosphere the values to render
     * @param worldRoot  the world root of the map being set up
     */
    public void preview(Player player, MapAtmosphere atmosphere, Path worldRoot) {
        Session previous = this.sessions.get(player.getUuid());
        Instance origin = previous != null ? previous.origin() : player.getInstance();
        Pos originPosition = previous != null ? previous.originPosition() : player.getPosition();
        int counter = previous != null ? previous.counter() + 1 : 1;

        if (origin == null) {
            LOGGER.warn("Cannot preview an atmosphere for {}: the player is in no instance", player.getUsername());
            return;
        }

        if (counter == PREVIEW_WARNING_THRESHOLD) {
            LOGGER.warn(
                    "{} has rendered {} atmosphere previews; each one holds a dimension registry entry "
                            + "for the lifetime of this server",
                    player.getUsername(), counter
            );
        }

        RegistryKey<DimensionType> dimension = DimensionFactory.create(
                Key.key("cygnus", "preview/%s/%d".formatted(player.getUuid().toString().replace("-", ""), counter)),
                atmosphere
        );

        FalcoAnvilLoader loader = new FalcoAnvilLoader(worldRoot, DimensionType.OVERWORLD.key());
        InstanceContainer instance = MinecraftServer.getInstanceManager().createInstanceContainer(dimension);
        instance.setChunkLoader(loader);
        instance.enableAutoChunkLoad(true);
        MinecraftServer.getInstanceManager().registerInstance(instance);

        Session session = new Session(instance, loader, origin, originPosition, counter);
        this.sessions.put(player.getUuid(), session);

        send(player, instance, originPosition);
        release(previous);
    }

    /**
     * Returns the player to the instance they were in before their first preview and tears the
     * preview down. Does nothing if no preview is running.
     *
     * @param player the builder to bring back
     */
    public void leave(Player player) {
        Session session = this.sessions.remove(player.getUuid());
        if (session == null) return;

        send(player, session.origin(), session.originPosition());
        release(session);
    }

    /**
     * Discards a running preview without moving the player, for use when they disconnect.
     *
     * @param player the builder that left
     */
    public void discard(Player player) {
        release(this.sessions.remove(player.getUuid()));
    }

    /**
     * Returns the instance a player should respawn into after their configuration phase.
     *
     * <p>{@code SetupExtension} consults this before falling back to the setup hub, so the preview
     * does not have to compete with that listener.</p>
     *
     * @param player the player finishing a configuration phase
     * @return the preview instance, or {@code null} if the player is not previewing
     */
    public @Nullable Instance pendingInstance(Player player) {
        Session session = this.sessions.get(player.getUuid());
        return session == null ? null : session.instance();
    }

    /**
     * Moves a player into the given instance through a configuration phase, which is the only way
     * the client learns about a dimension it did not receive at login.
     *
     * @param player   the player to move
     * @param instance the instance to move them into
     * @param position where to put them once they arrive
     */
    private void send(Player player, Instance instance, Pos position) {
        player.setRespawnPoint(position);
        player.setPendingOptions(instance, false);
        player.startConfigurationPhase();
    }

    /**
     * Unregisters a preview instance and closes its chunk loader, once nobody is inside it any
     * more.
     *
     * <p>The wait is not optional: Minestom refuses to unregister an instance that still holds
     * players, and moving a player out runs through a configuration phase that does not finish
     * within this call. The task ends by itself once the instance is empty, which also covers the
     * builder simply disconnecting out of the preview.</p>
     *
     * @param session the session to release, or {@code null} for nothing to do
     */
    private void release(@Nullable Session session) {
        if (session == null) return;

        MinecraftServer.getSchedulerManager().submitTask(() -> {
            if (!session.instance().getPlayers().isEmpty()) return TaskSchedule.nextTick();

            MinecraftServer.getInstanceManager().unregisterInstance(session.instance());
            try {
                session.loader().close();
            } catch (IOException exception) {
                MinecraftServer.getExceptionManager().handleException(exception);
            }
            return TaskSchedule.stop();
        });
    }

    /**
     * One builder's running preview.
     *
     * @param instance       the throwaway instance the preview renders in
     * @param loader         the chunk loader of that instance, which holds its region files open
     * @param origin         the instance the builder came from
     * @param originPosition where in that instance they stood
     * @param counter        how many previews this builder has rendered, used to keep registry keys
     *                       unique
     */
    private record Session(
            InstanceContainer instance,
            FalcoAnvilLoader loader,
            Instance origin,
            Pos originPosition,
            int counter
    ) {
    }
}
