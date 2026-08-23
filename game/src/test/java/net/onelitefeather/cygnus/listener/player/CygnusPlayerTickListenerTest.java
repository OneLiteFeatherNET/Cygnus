package net.onelitefeather.cygnus.listener.player;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerTickEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.entity.DeadPlayerMannequin;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the {@link CygnusPlayerTickListener} only hands survivors to the jump scare
 * detection. The scare blinds its victim for 40 ticks, which must never hit the slender or a
 * spectator.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.7.0
 */
class CygnusPlayerTickListenerTest extends CygnusPlayerTestBase {

    private static final Pos CORPSE_POS = new Pos(10, 41, 10);
    private static final Pos VICTIM_POS = new Pos(14, 41, 10);
    private static final int TURN_AROUNDS = 100;

    @Test
    void testSpectatorNeverReceivesAJumpScare(@NotNull Env env) {
        assertNoJumpScareFor(env, GameConfig.SPECTATOR_KEY, true, "a spectator");
    }

    @Test
    void testSlenderNeverReceivesAJumpScare(@NotNull Env env) {
        assertNoJumpScareFor(env, GameConfig.SLENDER_KEY, false, "the slender");
    }

    @Test
    void testUntaggedPlayerNeverReceivesAJumpScare(@NotNull Env env) {
        assertNoJumpScareFor(env, null, false, "an untagged player");
    }

    @Test
    void testSurvivorStillReceivesAJumpScare(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player corpseOwner = env.createPlayer(instance, CORPSE_POS);

        TestConnection victimConnection = env.createConnection();
        Player victim = victimConnection.connect(instance, VICTIM_POS);
        victim.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);

        JumpScareManager manager = new JumpScareManager();
        DeadPlayerMannequin corpse = registerCorpse(env, manager, instance, corpseOwner);

        Collector<SpawnEntityPacket> spawns = victimConnection.trackIncoming(SpawnEntityPacket.class);
        driveTurnArounds(new CygnusPlayerTickListener(manager), victim);

        List<SpawnEntityPacket> packets = spawns.collect();
        assertTrue(packets.stream().anyMatch(packet -> packet.type() == EntityType.MANNEQUIN),
                "a survivor must still receive a jump scare phantom");
        assertFalse(corpse.isViewer(victim), "the real corpse gets hidden while the phantom is up");

        env.destroyInstance(instance, true);
    }

    /**
     * Drives a full set of turn arounds against a player of the given team and asserts that no
     * phantom ever reached them.
     *
     * @param env         the test environment
     * @param teamKey     the team tag to apply, or {@code null} to leave the player untagged
     * @param spectatorMode whether the player should also be put into the spectator game mode
     * @param description the role description used in the assertion messages
     */
    private void assertNoJumpScareFor(@NotNull Env env, Key teamKey, boolean spectatorMode, String description) {
        Instance instance = env.createFlatInstance();
        Player corpseOwner = env.createPlayer(instance, CORPSE_POS);

        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, VICTIM_POS);
        if (teamKey != null) {
            player.setTag(Tags.TEAM_KEY, teamKey);
        }
        if (spectatorMode) {
            player.setGameMode(GameMode.SPECTATOR);
        }

        JumpScareManager manager = new JumpScareManager();
        DeadPlayerMannequin corpse = registerCorpse(env, manager, instance, corpseOwner);

        Collector<SpawnEntityPacket> spawns = connection.trackIncoming(SpawnEntityPacket.class);
        driveTurnArounds(new CygnusPlayerTickListener(manager), player);

        assertTrue(spawns.collect().isEmpty(), description + " must never receive a jump scare phantom");
        assertTrue(corpse.isViewer(player), description + " must keep the untouched view on the corpse");

        env.destroyInstance(instance, true);
    }

    /**
     * Spawns a corpse, registers it and lets the view state settle.
     *
     * @param env         the test environment
     * @param manager     the manager to register the corpse at
     * @param instance    the instance to spawn the corpse in
     * @param corpseOwner the player the corpse belongs to
     * @return the spawned corpse
     */
    private DeadPlayerMannequin registerCorpse(@NotNull Env env, JumpScareManager manager, Instance instance, Player corpseOwner) {
        DeadPlayerMannequin corpse = DeadPlayerMannequin.sleeping(corpseOwner);
        corpse.setInstance(instance, CORPSE_POS);
        manager.register(corpse);

        // Let the automatic viewability settle so the corpse spawn packet is out of the way.
        for (int i = 0; i < 5; i++) env.tick();
        return corpse;
    }

    /**
     * Fires enough alternating 180 degree turn arounds that the 35 percent trigger chance is
     * practically guaranteed to hit at least once for an eligible player.
     *
     * @param listener the listener under test
     * @param player   the player to rotate
     */
    private void driveTurnArounds(CygnusPlayerTickListener listener, Player player) {
        for (int i = 0; i < TURN_AROUNDS; i++) {
            player.setView(i % 2 == 0 ? 0F : 180F, 0F);
            listener.accept(new PlayerTickEvent(player));
        }
    }
}
