package net.onelitefeather.cygnus.vitality;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.EventFilter;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Collector;
import net.minestom.server.network.packet.server.play.UpdateHealthPacket;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.event.PlayerDamagedEvent;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VitalityTest extends CygnusPlayerTestBase {

    private static final double EPSILON = 1.0E-9;

    private static CygnusPlayer player(Env env) {
        Instance instance = env.createFlatInstance();
        return (CygnusPlayer) env.createConnection().connect(instance, new Pos(0, 40, 0));
    }

    @Test
    @DisplayName("Health shows the points rounded up to the next half heart, 0 only at 0 points and ignores drift")
    void healthRoundsUp() {
        assertEquals(20.0F, Vitality.health(100.0D));
        assertEquals(20.0F, Vitality.health(97.5D));
        assertEquals(1.0F, Vitality.health(0.2D));
        assertEquals(0.0F, Vitality.health(0.0D));
        assertEquals(15.0F, Vitality.health(75.0D + 1.0E-12), "drift just above a multiple of 5 shows no extra half heart");
    }

    @Test
    @DisplayName("Every Cygnus player carries their own vitality")
    void playerCarriesVitality(Env env) {
        CygnusPlayer player = player(env);

        assertSame(player.vitality(), player.vitality());
        assertEquals(100.0D, player.vitality().points(), EPSILON);
    }

    @Test
    @DisplayName("Setting up without a bonus gives 100 points and 10 hearts")
    void setUpWithoutBonus(Env env) {
        CygnusPlayer player = player(env);

        player.vitality().setUp(0);

        assertEquals(100.0D, player.vitality().points(), EPSILON);
        assertEquals(100.0D, player.vitality().max(), EPSILON);
        assertEquals(20.0D, player.getAttributeValue(Attribute.MAX_HEALTH), EPSILON);
        assertEquals(20.0F, player.getHealth());
    }

    @Test
    @DisplayName("A bonus shows as extra hearts")
    void setUpWithBonus(Env env) {
        CygnusPlayer player = player(env);

        player.vitality().setUp(50);

        assertEquals(150.0D, player.vitality().points(), EPSILON);
        assertEquals(30.0D, player.getAttributeValue(Attribute.MAX_HEALTH), EPSILON);
        assertEquals(30.0F, player.getHealth());
    }

    @Test
    @DisplayName("A player who was never set up reads points from the health until the first hit")
    void fallbackReadsHealth(Env env) {
        CygnusPlayer player = player(env);
        player.setHealth(3.0F);

        assertEquals(15.0D, player.vitality().points(), EPSILON);
        assertEquals(100.0D, player.vitality().max(), EPSILON);

        player.vitality().damage(5.0D, player.getPosition());
        assertEquals(10.0D, player.vitality().points(), EPSILON);
        assertEquals(2.0F, player.getHealth());
    }

    @Test
    @DisplayName("Damage lowers the points, the shown health and announces the hit in health")
    void damage(Env env) {
        CygnusPlayer player = player(env);
        player.vitality().setUp(0);
        Pos source = new Pos(1, 40, 0);
        Collector<PlayerDamagedEvent> collector = env.trackEvent(PlayerDamagedEvent.class, EventFilter.PLAYER, player);

        double lost = player.vitality().damage(22.5D, source);

        assertEquals(22.5D, lost, EPSILON);
        assertEquals(77.5D, player.vitality().points(), EPSILON);
        assertEquals(16.0F, player.getHealth());
        collector.assertSingle(event -> {
            assertEquals(source, event.getSource());
            assertEquals(4.5F, event.getAmount());
        });
    }

    @Test
    @DisplayName("A hit is only announced once the shown health drops, so tiny hits do not splatter every tick")
    void tinyHitsAreNotAnnounced(Env env) {
        CygnusPlayer player = player(env);
        player.vitality().setUp(0);
        List<Float> hits = new ArrayList<>();
        env.process().eventHandler().addListener(PlayerDamagedEvent.class, event -> hits.add(event.getAmount()));

        player.vitality().damage(1.0D, player.getPosition());
        player.vitality().damage(3.0D, player.getPosition());
        assertEquals(List.of(), hits, "20 hearts are still shown, nothing to announce");

        player.vitality().damage(1.0D, player.getPosition());
        assertEquals(List.of(0.2F), hits, "the hit that drops the shown health is announced");
        assertEquals(19.0F, player.getHealth());
    }

    @Test
    @DisplayName("The player dies exactly at 0 points and not before")
    void deathAtZero(Env env) {
        CygnusPlayer player = player(env);
        player.vitality().setUp(0);

        player.vitality().damage(99.9D, player.getPosition());
        assertFalse(player.isDead());
        assertEquals(1.0F, player.getHealth());

        player.vitality().damage(0.1D, player.getPosition());
        assertTrue(player.isDead());
        assertEquals(0.0D, player.vitality().points(), EPSILON);
    }

    @Test
    @DisplayName("A rest below the drift tolerance counts as 0 and kills")
    void tinyRestKills(Env env) {
        CygnusPlayer player = player(env);
        player.vitality().setUp(0);

        player.vitality().damage(100.0D - 1.0E-12, player.getPosition());

        assertTrue(player.isDead());
    }

    @Test
    @DisplayName("Healing raises the points up to the maximum and never above")
    void heal(Env env) {
        CygnusPlayer player = player(env);
        player.vitality().setUp(0);
        player.vitality().damage(10.0D, player.getPosition());

        assertEquals(4.0D, player.vitality().heal(4.0D), EPSILON);
        assertEquals(94.0D, player.vitality().points(), EPSILON);
        assertEquals(6.0D, player.vitality().heal(50.0D), EPSILON);
        assertEquals(100.0D, player.vitality().points(), EPSILON);
        assertEquals(20.0F, player.getHealth());
    }

    @Test
    @DisplayName("Only a change of the shown health is sent to the client")
    void sendsOnlyVisibleChanges(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        CygnusPlayer player = (CygnusPlayer) connection.connect(instance, new Pos(0, 40, 0));
        player.vitality().setUp(0);
        player.vitality().damage(12.0D, player.getPosition());
        Collector<UpdateHealthPacket> packets = connection.trackIncoming(UpdateHealthPacket.class);

        player.vitality().heal(0.4D);
        player.vitality().heal(0.4D);
        player.vitality().heal(1.4D);

        packets.assertSingle(packet -> assertEquals(19.0F, packet.health()));
    }

    @Test
    @DisplayName("A dead player is not healed")
    void deadIsNotHealed(Env env) {
        CygnusPlayer player = player(env);
        player.vitality().setUp(0);
        player.vitality().damage(100.0D, player.getPosition());

        assertEquals(0.0D, player.vitality().heal(10.0D), EPSILON);
        assertEquals(0.0D, player.vitality().points(), EPSILON);
    }

    @Test
    @DisplayName("Damage and healing of 0 or less do nothing")
    void nonPositiveAmountsDoNothing(Env env) {
        CygnusPlayer player = player(env);
        player.vitality().setUp(0);
        Collector<PlayerDamagedEvent> collector = env.trackEvent(PlayerDamagedEvent.class, EventFilter.PLAYER, player);

        assertEquals(0.0D, player.vitality().damage(0.0D, player.getPosition()), EPSILON);
        assertEquals(0.0D, player.vitality().heal(-1.0D), EPSILON);

        collector.assertEmpty();
        assertEquals(100.0D, player.vitality().points(), EPSILON);
    }
}
