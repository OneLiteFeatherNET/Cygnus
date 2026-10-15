package net.onelitefeather.cygnus.clientguard;

import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerPacketEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.client.play.ClientUpdateSignPacket;
import net.minestom.server.network.packet.server.common.DisconnectPacket;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.network.packet.server.play.BlockEntityDataPacket;
import net.minestom.server.network.packet.server.play.CloseWindowPacket;
import net.minestom.server.network.packet.server.play.OpenSignEditorPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.common.config.ClientGuardConfig;
import net.onelitefeather.cygnus.common.config.ShaderMod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGeneratorFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shader probe inside a running server. The client's answers are fed the way the packet listener would
 * deliver them, so the tests cover the probe's sign, its answers and its timeout without a real client.
 * <p>
 * The sign editor is opened and closed by the server without any player input, and the probe never changes the
 * real instance: only the packets to the one player carry the sign.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
class ShaderModProbeTest extends CygnusPlayerTestBase {

    private static final Pos SPAWN = new Pos(8.5, 40, 8.5);
    private static final String IRIS_APPLY = "options.iris.apply";
    private static final String SODIUM_PAGE = "sodium.options.pages.general";
    private static final String TRANSLATED = "Translated text";

    private Env installedIn;
    private ClientGuard installed;

    /** The event handler outlives a test, so a guard left on it would see the next test's players. */
    @AfterEach
    void uninstall() {
        if (this.installed != null) {
            this.installedIn.process().eventHandler().removeChild(this.installed.node());
        }
    }

    private static ClientGuardConfig config(ClientGuardConfig.Mode mode, boolean probe, int timeoutTicks) {
        return new ClientGuardConfig(mode, ClientGuardConfig.DEFAULT_BLOCKED_BRANDS,
                ClientGuardConfig.DEFAULT_BLOCKED_CHANNELS, List.of(), probe,
                ClientGuardConfig.DEFAULT_INCOMPATIBLE_MODS, ClientGuardConfig.DEFAULT_LOGGED_MODS, timeoutTicks);
    }

    private ClientGuard install(Env env, ClientGuardConfig config) {
        ClientGuard guard = new ClientGuard(config, RandomGeneratorFactory.of("L64X128MixRandom").create(7L));
        env.process().eventHandler().addChild(guard.node());
        this.installedIn = env;
        this.installed = guard;
        return guard;
    }

    /** The lines of the probe sign: each one is a translatable text with its key and fallback. */
    private static List<CompoundBinaryTag> messagesOf(BlockEntityDataPacket sign) {
        ListBinaryTag messages = sign.data().getCompound("front_text").getList("messages");
        List<CompoundBinaryTag> lines = new ArrayList<>();
        for (int index = 0; index < messages.size(); index++) {
            lines.add(messages.getCompound(index));
        }
        return lines;
    }

    /**
     * Answers the way a client does: a line whose key the client knows shows translated text, every other line
     * shows the fallback it was sent with.
     */
    private static List<String> answerFor(BlockEntityDataPacket sign, Set<String> knownKeys) {
        List<String> answer = new ArrayList<>();
        for (CompoundBinaryTag line : messagesOf(sign)) {
            answer.add(knownKeys.contains(line.getString("translate")) ? TRANSLATED : line.getString("fallback"));
        }
        return answer;
    }

    private static PlayerPacketEvent answer(Player player, Point position, List<String> lines) {
        PlayerPacketEvent event = new PlayerPacketEvent(player, new ClientUpdateSignPacket(position, true, lines));
        EventDispatcher.call(event);
        return event;
    }

    private static BlockEntityDataPacket onlySign(Collector<BlockEntityDataPacket> signs) {
        List<BlockEntityDataPacket> collected = signs.collect();
        assertEquals(1, collected.size(), "one probe sign must be sent per batch, got " + collected.size());
        return collected.getFirst();
    }

    @Test
    @DisplayName("The first spawn opens the sign editor at the position of the probe sign")
    void firstSpawnOpensSignEditor(Env env) {
        Instance instance = env.createFlatInstance();
        install(env, config(ClientGuardConfig.Mode.KICK, true, 100));
        TestConnection connection = env.createConnection();
        Collector<OpenSignEditorPacket> opened = connection.trackIncoming(OpenSignEditorPacket.class);
        Collector<BlockChangePacket> changes = connection.trackIncoming(BlockChangePacket.class);

        connection.connect(instance, SPAWN);

        List<OpenSignEditorPacket> editors = opened.collect();
        assertEquals(1, editors.size(), "the first spawn must open exactly one sign editor");
        Point position = editors.getFirst().position();
        assertTrue(changes.collect().stream().anyMatch(change -> change.blockPosition().equals(position)
                        && change.blockStateId() == Block.OAK_SIGN.stateId()),
                "the probe must place an oak sign at the position the editor opens on");
    }

    @Test
    @DisplayName("The probe sign carries the first four keys of the configured mods as translatable lines")
    void probeSignCarriesTranslatableLines(Env env) {
        Instance instance = env.createFlatInstance();
        install(env, config(ClientGuardConfig.Mode.KICK, true, 100));
        TestConnection connection = env.createConnection();
        Collector<BlockEntityDataPacket> signs = connection.trackIncoming(BlockEntityDataPacket.class);

        connection.connect(instance, SPAWN);

        List<CompoundBinaryTag> lines = messagesOf(onlySign(signs));
        assertEquals(4, lines.size(), "a sign has four lines");
        assertEquals(List.of("options.iris.apply", "options.iris.shaderPackSettings", "of.options.capeOF.title",
                "info.canvas.reloading"), lines.stream().map(line -> line.getString("translate")).toList(),
                "the first sign must ask for the keys of iris, optifine and canvas in that order");
    }

    @Test
    @DisplayName("The probe closes the sign editor right after opening it")
    void probeClosesSignEditor(Env env) {
        Instance instance = env.createFlatInstance();
        install(env, config(ClientGuardConfig.Mode.KICK, true, 100));
        TestConnection connection = env.createConnection();
        Collector<CloseWindowPacket> closed = connection.trackIncoming(CloseWindowPacket.class);

        connection.connect(instance, SPAWN);

        assertEquals(List.of(0), closed.collect().stream().map(CloseWindowPacket::windowId).toList(),
                "the probe must close the sign editor of the inventory window 0 right after opening it");
    }

    @Test
    @DisplayName("A resolved iris key kicks the player with the incompatible mods message")
    void resolvedIrisKeyKicksPlayer(Env env) {
        Instance instance = env.createFlatInstance();
        install(env, config(ClientGuardConfig.Mode.KICK, true, 100));
        TestConnection connection = env.createConnection();
        Collector<BlockEntityDataPacket> signs = connection.trackIncoming(BlockEntityDataPacket.class);
        Player player = connection.connect(instance, SPAWN);
        BlockEntityDataPacket sign = onlySign(signs);
        Collector<DisconnectPacket> disconnects = connection.trackIncoming(DisconnectPacket.class);

        answer(player, sign.blockPosition(), answerFor(sign, Set.of(IRIS_APPLY)));

        assertFalse(player.isOnline(), "a resolved iris key must disconnect the player");
        disconnects.assertSingle(packet -> assertEquals(Messages.INCOMPATIBLE_MODS, packet.message(),
                "the kick must carry the incompatible mods message"));
    }

    @Test
    @DisplayName("The probe answers are swallowed so no other listener sees them as sign edits")
    void probeAnswerIsSwallowed(Env env) {
        Instance instance = env.createFlatInstance();
        install(env, config(ClientGuardConfig.Mode.KICK, true, 100));
        TestConnection connection = env.createConnection();
        Collector<BlockEntityDataPacket> signs = connection.trackIncoming(BlockEntityDataPacket.class);
        Player player = connection.connect(instance, SPAWN);
        BlockEntityDataPacket sign = onlySign(signs);

        PlayerPacketEvent event = answer(player, sign.blockPosition(), answerFor(sign, Set.of()));

        assertTrue(event.isCancelled(), "an answer for the probe position must not reach the sign edit listeners");
    }

    @Test
    @DisplayName("An update for a position the probe did not open is left alone")
    void unrelatedSignUpdateIsNotSwallowed(Env env) {
        Instance instance = env.createFlatInstance();
        install(env, config(ClientGuardConfig.Mode.KICK, true, 100));
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);

        PlayerPacketEvent event = answer(player, new Pos(0, 0, 0), List.of("a", "b", "c", "d"));

        assertFalse(event.isCancelled(), "a sign update for another position is the real sign listener's business");
    }

    @Test
    @DisplayName("Sodium resolving does not kick and is recorded on the profile")
    void resolvedSodiumKeyIsRecordedNotKicked(Env env) {
        Instance instance = env.createFlatInstance();
        ClientGuard guard = install(env, config(ClientGuardConfig.Mode.KICK, true, 100));
        TestConnection connection = env.createConnection();
        Collector<BlockEntityDataPacket> firstSign = connection.trackIncoming(BlockEntityDataPacket.class);
        Player player = connection.connect(instance, SPAWN);
        BlockEntityDataPacket first = onlySign(firstSign);
        Collector<BlockEntityDataPacket> secondSign = connection.trackIncoming(BlockEntityDataPacket.class);
        answer(player, first.blockPosition(), answerFor(first, Set.of()));

        BlockEntityDataPacket second = onlySign(secondSign);
        answer(player, second.blockPosition(), answerFor(second, Set.of(SODIUM_PAGE)));

        assertTrue(player.isOnline(), "a logged mod must never disconnect the player");
        assertEquals(Set.of(ShaderMod.SODIUM), guard.profile(player.getUuid()).orElseThrow().detectedMods(),
                "the detected sodium must be recorded on the profile");
    }

    @Test
    @DisplayName("Fallbacks only keep the player connected and record no mod")
    void allFallbacksDetectNothing(Env env) {
        Instance instance = env.createFlatInstance();
        ClientGuard guard = install(env, config(ClientGuardConfig.Mode.KICK, true, 100));
        TestConnection connection = env.createConnection();
        Collector<BlockEntityDataPacket> firstSign = connection.trackIncoming(BlockEntityDataPacket.class);
        Player player = connection.connect(instance, SPAWN);
        BlockEntityDataPacket first = onlySign(firstSign);
        Collector<BlockEntityDataPacket> secondSign = connection.trackIncoming(BlockEntityDataPacket.class);
        answer(player, first.blockPosition(), answerFor(first, Set.of()));

        BlockEntityDataPacket second = onlySign(secondSign);
        answer(player, second.blockPosition(), answerFor(second, Set.of()));

        assertTrue(player.isOnline(), "a client that knows none of the keys must stay connected");
        assertTrue(guard.profile(player.getUuid()).map(ClientProfile::detectedMods).orElse(Set.of()).isEmpty(),
                "no key resolved, so no mod may be recorded");
    }

    @Test
    @DisplayName("The log mode logs an incompatible mod but keeps the player connected")
    void logModeKeepsPlayerConnected(Env env) {
        Instance instance = env.createFlatInstance();
        ClientGuard guard = install(env, config(ClientGuardConfig.Mode.LOG, true, 100));
        TestConnection connection = env.createConnection();
        Collector<BlockEntityDataPacket> signs = connection.trackIncoming(BlockEntityDataPacket.class);
        Player player = connection.connect(instance, SPAWN);
        BlockEntityDataPacket sign = onlySign(signs);

        answer(player, sign.blockPosition(), answerFor(sign, Set.of(IRIS_APPLY)));

        assertTrue(player.isOnline(), "the log mode must not disconnect anybody");
        assertEquals(Set.of(ShaderMod.IRIS), guard.profile(player.getUuid()).orElseThrow().detectedMods(),
                "the log mode still records the incompatible mod");
    }

    @Test
    @DisplayName("A client that does not answer in time stays connected")
    void silentClientIsNotKicked(Env env) {
        Instance instance = env.createFlatInstance();
        install(env, config(ClientGuardConfig.Mode.KICK, true, 10));
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, SPAWN);

        tickFor(env, 11);

        assertTrue(player.isOnline(), "an unanswered probe is ambiguous and must not kick the player");
    }

    @Test
    @DisplayName("A probe that times out restores the real block at its position")
    void timeoutRestoresRealBlock(Env env) {
        Instance instance = env.createFlatInstance();
        install(env, config(ClientGuardConfig.Mode.KICK, true, 10));
        TestConnection connection = env.createConnection();
        Collector<OpenSignEditorPacket> opened = connection.trackIncoming(OpenSignEditorPacket.class);
        Player player = connection.connect(instance, SPAWN);
        Point position = opened.collect().getFirst().position();
        int realState = instance.getBlock(position).stateId();
        Collector<BlockChangePacket> changes = connection.trackIncoming(BlockChangePacket.class);

        tickFor(env, 11);

        assertTrue(changes.collect().stream().anyMatch(change -> change.blockPosition().equals(position)
                        && change.blockStateId() == realState),
                "the timed out probe must put the instance's real block back");
    }

    @Test
    @DisplayName("An answer that arrives after the timeout does not kick the player")
    void lateAnswerDoesNotKick(Env env) {
        Instance instance = env.createFlatInstance();
        install(env, config(ClientGuardConfig.Mode.KICK, true, 10));
        TestConnection connection = env.createConnection();
        Collector<BlockEntityDataPacket> signs = connection.trackIncoming(BlockEntityDataPacket.class);
        Player player = connection.connect(instance, SPAWN);
        BlockEntityDataPacket sign = onlySign(signs);
        tickFor(env, 11);

        PlayerPacketEvent event = answer(player, sign.blockPosition(), answerFor(sign, Set.of(IRIS_APPLY)));

        assertTrue(player.isOnline(), "a late answer is no longer part of the probe and must not kick");
        assertTrue(event.isCancelled(), "a late answer for the probe position is still swallowed");
    }

    @Test
    @DisplayName("With the shader probe off no sign is sent to the client")
    void probeOffSendsNoSign(Env env) {
        Instance instance = env.createFlatInstance();
        install(env, config(ClientGuardConfig.Mode.KICK, false, 100));
        TestConnection connection = env.createConnection();
        Collector<OpenSignEditorPacket> opened = connection.trackIncoming(OpenSignEditorPacket.class);

        connection.connect(instance, SPAWN);

        opened.assertEmpty();
    }

    private static void tickFor(Env env, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            env.tick();
        }
    }
}
