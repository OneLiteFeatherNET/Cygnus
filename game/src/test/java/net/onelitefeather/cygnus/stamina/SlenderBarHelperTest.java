package net.onelitefeather.cygnus.stamina;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.event.PlayerDamagedEvent;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.vitality.Vitality;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the role filters of the {@link SlenderBarHelper} only let survivors take slender
 * damage and only let survivors hear the slender sounds. It also verifies that the damage is
 * announced, since setting health directly never raises Minestom's own damage event.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.7.0
 */
class SlenderBarHelperTest extends CygnusPlayerTestBase {

    private static final Pos CENTER = new Pos(10, 41, 10);
    private static final float DAMAGE = 0.5F;
    /** One health point, what the slender's drain takes per tick, enough to drop the shown health. */
    private static final float SHOWN_DAMAGE = 1.0F;
    private static final int RANGE = 3;

    private final SlenderBarHelper helper = new SlenderBarHelper() {
    };

    private Instance instance;

    @BeforeEach
    void setUp(Env env) {
        this.instance = env.createFlatInstance();
    }

    @AfterEach
    void tearDown(Env env) {
        env.destroyInstance(this.instance, true);
    }

    @Test
    void testSpectatorTakesNoSlenderDamage(@NotNull Env env) {
        Player slender = env.createPlayer(instance, CENTER);
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);

        Player spectator = env.createPlayer(instance, CENTER.add(1, 0, 0));
        spectator.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);
        spectator.setGameMode(GameMode.SPECTATOR);

        float healthBefore = spectator.getHealth();
        helper.applyDamage(instance, slender.getUuid(), CENTER, 3, DAMAGE);

        assertEquals(healthBefore, spectator.getHealth(), "a spectator must never take slender damage");
    }

    @Test
    void testSurvivorTakesSlenderDamage(@NotNull Env env) {
        Player slender = env.createPlayer(instance, CENTER);
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);

        CygnusPlayer survivor = (CygnusPlayer) env.createPlayer(instance, CENTER.add(1, 0, 0));
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);

        double pointsBefore = survivor.vitality().points();
        helper.applyDamage(instance, slender.getUuid(), CENTER, 3, DAMAGE);

        assertEquals(pointsBefore - DAMAGE * Vitality.POINTS_PER_HEALTH, survivor.vitality().points(), 1.0E-9,
                "a survivor must still take slender damage");
    }

    @Test
    void testSlenderTakesNoOwnDamage(@NotNull Env env) {
        Player slender = env.createPlayer(instance, CENTER);
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);

        Player otherSlender = env.createPlayer(instance, CENTER.add(1, 0, 0));
        otherSlender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);

        float slenderHealth = slender.getHealth();
        float otherHealth = otherSlender.getHealth();
        helper.applyDamage(instance, slender.getUuid(), CENTER, 3, DAMAGE);

        assertEquals(slenderHealth, slender.getHealth(), "the slender must not damage itself");
        assertEquals(otherHealth, otherSlender.getHealth(), "a slender must never take slender damage");
    }

    @Test
    void testUntaggedPlayerTakesNoDamage(@NotNull Env env) {
        Player slender = env.createPlayer(instance, CENTER);
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);

        Player untagged = env.createPlayer(instance, CENTER.add(1, 0, 0));

        float healthBefore = untagged.getHealth();
        helper.applyDamage(instance, slender.getUuid(), CENTER, 3, DAMAGE);

        assertEquals(healthBefore, untagged.getHealth(), "the damage filter must be fail closed for untagged players");
    }

    @Test
    void testOnlySurvivorsHearTheTeleportSound(@NotNull Env env) {
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
    }

    @Test
    void testOnlySurvivorsHearTheSpawnSound(@NotNull Env env) {
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
    }

    @Test
    @DisplayName("A damaged player is announced together with where the hit came from")
    void damageIsAnnounced(Env env) {
        Player attacker = env.createConnection().connect(instance, new Pos(0, 40, 0));
        attacker.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        Player victim = env.createConnection().connect(instance, new Pos(1, 40, 0));
        victim.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        Pos center = new Pos(0, 40, 0);
        Collector<PlayerDamagedEvent> collector =
                env.trackEvent(PlayerDamagedEvent.class, EventFilter.PLAYER, victim);

        this.helper.applyDamage(instance, attacker.getUuid(), center, RANGE, SHOWN_DAMAGE);

        collector.assertSingle(event -> {
            assertEquals(victim, event.getPlayer(), "the victim has to be the one that was hit");
            assertEquals(center, event.getSource(), "the source is what aims the splatter");
        });
    }

    @Test
    @DisplayName("The player dealing the damage is left out")
    void attackerIsNotAnnounced(Env env) {
        Player attacker = env.createConnection().connect(instance, new Pos(0, 40, 0));
        attacker.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        Collector<PlayerDamagedEvent> collector =
                env.trackEvent(PlayerDamagedEvent.class, EventFilter.PLAYER, attacker);

        this.helper.applyDamage(instance, attacker.getUuid(), new Pos(0, 40, 0), RANGE, SHOWN_DAMAGE);

        collector.assertEmpty();
    }

    @Test
    @DisplayName("A hit takes points from a survivor who has a bonus, not raw health")
    void damageGoesThroughThePoints(Env env) {
        Player attacker = env.createConnection().connect(instance, new Pos(0, 40, 0));
        attacker.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        CygnusPlayer victim = (CygnusPlayer) env.createConnection().connect(instance, new Pos(1, 40, 0));
        victim.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        victim.vitality().setUp(50);

        this.helper.applyDamage(instance, attacker.getUuid(), new Pos(0, 40, 0), RANGE, 1.0F);

        assertEquals(145.0D, victim.vitality().points(), 1.0E-9);
        assertEquals(29.0F, victim.getHealth());
    }
}
