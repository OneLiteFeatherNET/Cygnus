package net.onelitefeather.cygnus.stamina;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.event.PlayerDamagedEvent;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that damage dealt by the slender is announced, since setting health directly never
 * raises Minestom's own damage event.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class SlenderBarHelperDamageTest extends CygnusPlayerTestBase {

    /** One health point, what the slender's drain takes per tick, enough to drop the shown health. */
    private static final float DAMAGE = 1.0F;
    private static final int RANGE = 3;

    private final SlenderBarHelper helper = new SlenderBarHelper() {
    };

    @Test
    @DisplayName("A damaged player is announced together with where the hit came from")
    void damageIsAnnounced(Env env) {
        Instance instance = env.createFlatInstance();
        Player attacker = env.createConnection().connect(instance, new Pos(0, 40, 0));
        attacker.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        Player victim = env.createConnection().connect(instance, new Pos(1, 40, 0));
        victim.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        Pos center = new Pos(0, 40, 0);
        Collector<PlayerDamagedEvent> collector =
                env.trackEvent(PlayerDamagedEvent.class, EventFilter.PLAYER, victim);

        this.helper.applyDamage(instance, attacker.getUuid(), center, RANGE, DAMAGE);

        collector.assertSingle(event -> {
            assertEquals(victim, event.getPlayer(), "the victim has to be the one that was hit");
            assertEquals(center, event.getSource(), "the source is what aims the splatter");
        });
    }

    @Test
    @DisplayName("The player dealing the damage is left out")
    void attackerIsNotAnnounced(Env env) {
        Instance instance = env.createFlatInstance();
        Player attacker = env.createConnection().connect(instance, new Pos(0, 40, 0));
        attacker.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        Collector<PlayerDamagedEvent> collector =
                env.trackEvent(PlayerDamagedEvent.class, EventFilter.PLAYER, attacker);

        this.helper.applyDamage(instance, attacker.getUuid(), new Pos(0, 40, 0), RANGE, DAMAGE);

        collector.assertEmpty();
    }

    @Test
    @DisplayName("A hit takes points from a survivor who has a bonus, not raw health")
    void damageGoesThroughThePoints(Env env) {
        Instance instance = env.createFlatInstance();
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
