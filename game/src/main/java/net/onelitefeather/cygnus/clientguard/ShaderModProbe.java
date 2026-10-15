package net.onelitefeather.cygnus.clientguard;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockEntityType;
import net.minestom.server.network.packet.client.play.ClientUpdateSignPacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.network.packet.server.play.BlockEntityDataPacket;
import net.minestom.server.network.packet.server.play.CloseWindowPacket;
import net.minestom.server.network.packet.server.play.OpenSignEditorPacket;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.block.BlockUtils;
import net.onelitefeather.cygnus.common.config.ClientGuardConfig;
import net.onelitefeather.cygnus.common.config.ShaderMod;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;

/**
 * Finds out which shader mods a client has, by asking it to translate keys that only those mods add.
 * <p>
 * These mods register no network channel and no brand, so the packets of the client say nothing about them. A
 * translatable text shows its translation when the client has the key, and its fallback when it has not. The
 * probe makes the client show such texts on a sign, and reads the answer back from the sign update the client
 * sends when the sign editor closes:
 * </p>
 * <ol>
 * <li>On the first spawn the probe sends a sign to the player only, four keys per sign, in the order of the mods.
 * The real instance is not changed. The sign is placed {@value #SIGN_DEPTH} blocks below the player's feet.</li>
 * <li>The probe opens the sign editor on it and closes it again right away, without any input from the player.
 * The client then sends an update with the four lines it shows.</li>
 * <li>A line that differs from its fallback means its key resolved, so the mod that owns it is present, see
 * {@link ShaderProbePlan}. The real block is sent back, and the next sign follows.</li>
 * <li>An update for the probe's position is swallowed, so no other listener takes it for a real sign edit.</li>
 * <li>A client that does not answer within the configured number of ticks is not judged: the probe is ambiguous,
 * the block is put back and no more signs are sent. The timeout runs on the player's scheduler, so it counts
 * server ticks and never wall clock time.</li>
 * </ol>
 * <p>
 * What the probe can and cannot tell:
 * </p>
 * <ul>
 * <li>Blocker mods such as ExploitPreventer and OpSec answer the probe with the fallbacks, so a client without any
 * detected mod is not proof that the client is vanilla. Nothing detected only means nothing was seen.</li>
 * <li>Iris is kicked even without an active shader pack. The server cannot see whether a shader pack is active,
 * so the presence of the mod is what counts.</li>
 * <li>The sign editor is opened and closed without any player input. A player who is busy with the screen at
 * that moment may see it flicker for a tick.</li>
 * <li>The close of the editor is what makes the client send the answer. This follows the vanilla client, where
 * closing the screen sends the sign update. It was not checked against a live client when this was written.</li>
 * </ul>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public final class ShaderModProbe {

    /** How many blocks below the player's feet the probe sign is placed. */
    static final int SIGN_DEPTH = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(ShaderModProbe.class);

    /** The block entity type of the oak sign the probe places, the sign type of the vanilla registry. */
    private static final BlockEntityType SIGN_TYPE = Objects.requireNonNull(Block.OAK_SIGN.blockEntityType(),
            "the oak sign has a block entity type");

    /**
     * Receives the mods a client answered for. It is called once per answered sign, and only with mods whose keys
     * resolved, so a call always means something was seen.
     */
    @FunctionalInterface
    public interface Listener {

        /**
         * Called when the answer to one probe sign showed at least one mod.
         *
         * @param player the player whose client answered
         * @param mods   the mods whose keys resolved on the client
         */
        void resolved(Player player, Set<ShaderMod> mods);
    }

    private final EventNode<Event> node = EventNode.all("shader-mod-probe");
    private final List<ShaderMod> mods;
    private final int timeoutTicks;
    private final RandomGenerator random;
    private final Listener listener;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    /**
     * Creates the probe for the mods of the config: the incompatible ones first, then the logged ones.
     *
     * @param config   the client guard settings, which name the mods and the timeout
     * @param random   the source of the fallback tokens. A seeded source makes the probe repeatable.
     * @param listener receives the mods each answer showed
     */
    public ShaderModProbe(ClientGuardConfig config, RandomGenerator random, Listener listener) {
        Set<ShaderMod> probed = new LinkedHashSet<>(config.incompatibleMods());
        probed.addAll(config.loggedMods());
        this.mods = List.copyOf(probed);
        this.timeoutTicks = config.shaderProbeTimeoutTicks();
        this.random = random;
        this.listener = listener;
        this.node.addListener(PlayerSpawnEvent.class, this::onSpawn);
        this.node.addListener(PlayerPacketEvent.class, this::onPacket);
        this.node.addListener(PlayerDisconnectEvent.class, event -> forget(event.getPlayer().getUuid()));
    }

    /**
     * Returns the node the probe listens on. It has to be part of the event tree for the probe to run.
     *
     * @return the probe's event node
     */
    public EventNode<Event> node() {
        return this.node;
    }

    private void onSpawn(PlayerSpawnEvent event) {
        if (!event.isFirstSpawn() || this.mods.isEmpty()) {
            return;
        }
        Player player = event.getPlayer();
        if (this.sessions.containsKey(player.getUuid())) {
            return;
        }
        Instance instance = event.getInstance();
        Point position = probePosition(instance, player.getPosition());
        if (position == null) {
            LOGGER.debug("Skipping the shader probe of {}: the chunk of the probe sign is not loaded", player.getUsername());
            return;
        }
        Session session = new Session(player, instance, position,
                new ArrayDeque<>(ShaderProbePlan.plan(this.mods, this.random)));
        this.sessions.put(player.getUuid(), session);
        sendNext(session);
    }

    private void onPacket(PlayerPacketEvent event) {
        if (!(event.getPacket() instanceof ClientUpdateSignPacket sign)) {
            return;
        }
        Session session = this.sessions.get(event.getPlayer().getUuid());
        if (session == null || !samePosition(session.position, sign.blockPosition())) {
            return;
        }
        event.setCancelled(true);
        synchronized (session) {
            List<ShaderProbePlan.ProbeLine> batch = session.pending;
            if (batch == null || !sign.isFrontText()) {
                LOGGER.debug("Ignoring an answer to the shader probe of {} that the probe does not wait for",
                        session.player.getUsername());
                return;
            }
            session.pending = null;
            cancelTimeout(session);
            restore(session);
            Set<ShaderMod> resolved = ShaderProbePlan.resolved(batch, sign.lines());
            if (!resolved.isEmpty()) {
                this.listener.resolved(session.player, resolved);
            }
            sendNext(session);
        }
    }

    private void sendNext(Session session) {
        synchronized (session) {
            Player player = session.player;
            if (!player.isOnline()) {
                cancelTimeout(session);
                session.remaining.clear();
                return;
            }
            List<ShaderProbePlan.ProbeLine> batch = session.remaining.poll();
            if (batch == null) {
                LOGGER.debug("The shader probe of {} is finished", player.getUsername());
                return;
            }
            session.pending = batch;
            Point position = session.position;
            player.sendPacket(new BlockChangePacket(position, Block.OAK_SIGN.stateId()));
            player.sendPacket(new BlockEntityDataPacket(position, SIGN_TYPE, probeSignData(batch)));
            player.sendPacket(new OpenSignEditorPacket(position, true));
            player.sendPacket(new CloseWindowPacket(0));
            session.timeout = player.scheduler()
                    .buildTask(() -> onTimeout(session))
                    .delay(TaskSchedule.tick(this.timeoutTicks))
                    .schedule();
        }
    }

    private void onTimeout(Session session) {
        synchronized (session) {
            session.timeout = null;
            if (session.pending == null) {
                return;
            }
            session.pending = null;
            session.remaining.clear();
            restore(session);
            LOGGER.debug("No answer to the shader probe of {} within {} ticks. The probe is ambiguous, so nothing is decided.",
                    session.player.getUsername(), this.timeoutTicks);
        }
    }

    private void restore(Session session) {
        Point position = session.position;
        Block real = session.instance.getBlock(position);
        session.player.sendPacket(new BlockChangePacket(position, real.stateId()));
        BlockEntityType type = real.blockEntityType();
        if (type != null) {
            session.player.sendPacket(new BlockEntityDataPacket(position, type, BlockUtils.extractClientNbt(real)));
        }
    }

    private void forget(UUID playerId) {
        Session session = this.sessions.remove(playerId);
        if (session != null) {
            synchronized (session) {
                cancelTimeout(session);
                session.pending = null;
                session.remaining.clear();
            }
        }
    }

    private static void cancelTimeout(Session session) {
        if (session.timeout != null) {
            session.timeout.cancel();
            session.timeout = null;
        }
    }

    /**
     * Builds the sign data: four lines, each a translatable text with its key and fallback. A batch of fewer than
     * four keys is padded with empty lines, which carry no key and so never resolve.
     */
    static CompoundBinaryTag probeSignData(List<ShaderProbePlan.ProbeLine> batch) {
        List<BinaryTag> lines = new ArrayList<>();
        for (ShaderProbePlan.ProbeLine line : batch) {
            lines.add(CompoundBinaryTag.builder()
                    .putString("translate", line.key())
                    .putString("fallback", line.fallback())
                    .build());
        }
        while (lines.size() < ShaderProbePlan.LINES_PER_SIGN) {
            lines.add(CompoundBinaryTag.builder().putString("text", "").build());
        }
        CompoundBinaryTag frontText = CompoundBinaryTag.builder()
                .put("messages", ListBinaryTag.from(lines))
                .putString("color", "black")
                .putByte("has_glowing_text", (byte) 0)
                .build();
        return CompoundBinaryTag.builder().put("front_text", frontText).build();
    }

    /**
     * Picks the block for the probe sign: the column of the player, {@value #SIGN_DEPTH} blocks below the feet and
     * not below the bottom of the world. Returns {@code null} if the chunk is not loaded.
     */
    private static @Nullable Point probePosition(Instance instance, Point feet) {
        int x = feet.blockX();
        int z = feet.blockZ();
        if (instance.getChunkAt(x, z) == null) {
            return null;
        }
        int y = Math.max(instance.getCachedDimensionType().minY(), feet.blockY() - SIGN_DEPTH);
        return new BlockVec(x, y, z);
    }

    private static boolean samePosition(Point expected, Point actual) {
        return expected.blockX() == actual.blockX()
                && expected.blockY() == actual.blockY()
                && expected.blockZ() == actual.blockZ();
    }

    /** What the probe knows about one player: the sign it places and the batches still to ask. */
    private static final class Session {

        private final Player player;
        private final Instance instance;
        private final Point position;
        private final Deque<List<ShaderProbePlan.ProbeLine>> remaining;
        private @Nullable List<ShaderProbePlan.ProbeLine> pending;
        private @Nullable Task timeout;

        private Session(Player player, Instance instance, Point position, Deque<List<ShaderProbePlan.ProbeLine>> remaining) {
            this.player = player;
            this.instance = instance;
            this.position = position;
            this.remaining = remaining;
        }
    }
}
