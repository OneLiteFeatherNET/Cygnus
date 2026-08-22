package net.onelitefeather.cygnus.stamina;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the role filters of the {@link SlenderBarHelper} only let survivors take slender
 * damage and only let survivors hear the slender sounds.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.7.0
 */
class SlenderBarHelperTest extends CygnusPlayerTestBase {

    private static final Pos CENTER = new Pos(10, 41, 10);
    private static final float DAMAGE = 0.5F;

    private final SlenderBarHelper helper = new SlenderBarHelper() {
    };

    @Test
    void testSpectatorTakesNoSlenderDamage(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance, CENTER);
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);

        Player spectator = env.createPlayer(instance, CENTER.add(1, 0, 0));
        spectator.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);
        spectator.setGameMode(GameMode.SPECTATOR);

        float healthBefore = spectator.getHealth();
        helper.applyDamage(instance, slender.getUuid(), CENTER, 3, DAMAGE);

        assertEquals(healthBefore, spectator.getHealth(), "a spectator must never take slender damage");

        env.destroyInstance(instance, true);
    }

    @Test
    void testSurvivorTakesSlenderDamage(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance, CENTER);
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);

        Player survivor = env.createPlayer(instance, CENTER.add(1, 0, 0));
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);

        float healthBefore = survivor.getHealth();
        helper.applyDamage(instance, slender.getUuid(), CENTER, 3, DAMAGE);

        assertEquals(healthBefore - DAMAGE, survivor.getHealth(), "a survivor must still take slender damage");

        env.destroyInstance(instance, true);
    }

    @Test
    void testSlenderTakesNoOwnDamage(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance, CENTER);
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);

        Player otherSlender = env.createPlayer(instance, CENTER.add(1, 0, 0));
        otherSlender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);

        float slenderHealth = slender.getHealth();
        float otherHealth = otherSlender.getHealth();
        helper.applyDamage(instance, slender.getUuid(), CENTER, 3, DAMAGE);

        assertEquals(slenderHealth, slender.getHealth(), "the slender must not damage itself");
        assertEquals(otherHealth, otherSlender.getHealth(), "a slender must never take slender damage");

        env.destroyInstance(instance, true);
    }

    @Test
    void testUntaggedPlayerTakesNoDamage(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance, CENTER);
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);

        Player untagged = env.createPlayer(instance, CENTER.add(1, 0, 0));

        float healthBefore = untagged.getHealth();
        helper.applyDamage(instance, slender.getUuid(), CENTER, 3, DAMAGE);

        assertEquals(healthBefore, untagged.getHealth(), "the damage filter must be fail closed for untagged players");

        env.destroyInstance(instance, true);
    }

    @Test
    void testOnlySurvivorsHearTheTeleportSound(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance, CENTER);
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);

        TestConnection spectatorConnection = env.createConnection();
        Player spectator = spectatorConnection.connect(instance, CENTER.add(1, 0, 0));
        spectator.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);
        spectator.setGameMode(GameMode.SPECTATOR);

        TestConnection survivorConnection = env.createConnection();
        Player survivor = survivorConnection.connect(instance, CENTER.add(0, 0, 1));
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);

        Collector<SoundEffectPacket> spectatorSounds = spectatorConnection.trackIncoming(SoundEffectPacket.class);
        Collector<SoundEffectPacket> survivorSounds = survivorConnection.trackIncoming(SoundEffectPacket.class);

        helper.playTeleportSound(instance, CENTER, slender.getUuid());

        assertTrue(spectatorSounds.collect().isEmpty(), "a spectator must not hear when the slender vanishes");
        assertEquals(1, survivorSounds.collect().size(), "a survivor must still hear the teleport sound");

        env.destroyInstance(instance, true);
    }

    @Test
    void testOnlySurvivorsHearTheSpawnSound(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance, CENTER);
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);

        TestConnection spectatorConnection = env.createConnection();
        Player spectator = spectatorConnection.connect(instance, CENTER.add(1, 0, 0));
        spectator.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);
        spectator.setGameMode(GameMode.SPECTATOR);

        TestConnection survivorConnection = env.createConnection();
        Player survivor = survivorConnection.connect(instance, CENTER.add(0, 0, 1));
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);

        Collector<SoundEffectPacket> spectatorSounds = spectatorConnection.trackIncoming(SoundEffectPacket.class);
        Collector<SoundEffectPacket> survivorSounds = survivorConnection.trackIncoming(SoundEffectPacket.class);

        helper.playSpawnSound(instance, CENTER, slender.getUuid());

        assertTrue(spectatorSounds.collect().isEmpty(), "a spectator must not hear the slender spawn sound");
        assertEquals(1, survivorSounds.collect().size(), "a survivor must still hear the spawn sound");

        env.destroyInstance(instance, true);
    }
}
