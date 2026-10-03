package net.onelitefeather.cygnus.adrenaline;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.attribute.AttributeHelper;
import net.onelitefeather.cygnus.common.config.AdrenalineConfig;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.StaminaStateChangeEvent;
import net.onelitefeather.cygnus.stamina.StaminaBar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdrenalineServiceTest extends CygnusPlayerTestBase {

    private static final double EPSILON = 1.0E-6;

    private final AtomicLong clock = new AtomicLong();
    private Set<Player> survivors = Set.of();

    private AdrenalineService service(Env env) {
        AdrenalineService service = new AdrenalineService(AdrenalineConfig.DEFAULT, () -> this.survivors, this.clock::get);
        env.process().eventHandler().addChild(service.node());
        return service;
    }

    private static Player connect(Env env, Instance instance, Pos position) {
        return env.createConnection().connect(instance, position);
    }

    private static void slenderBecomes(Player slender, StaminaBar.State state) {
        EventDispatcher.call(new StaminaStateChangeEvent(slender, state));
    }

    private static boolean rushing(Player player) {
        return player.getAttribute(Attribute.MOVEMENT_SPEED).modifiers().stream()
                .anyMatch(modifier -> modifier.id().equals(AttributeHelper.ADRENALINE_KEY));
    }

    @Test
    @DisplayName("A survivor close to the visible slender gets faster")
    void rushesCloseToVisibleSlender(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = connect(env, instance, new Pos(0, 40, 0));
        Player survivor = connect(env, instance, new Pos(5, 40, 0));
        this.survivors = Set.of(survivor);
        AdrenalineService service = service(env);

        slenderBecomes(slender, StaminaBar.State.DRAINING);
        service.tick();

        assertTrue(rushing(survivor));
        assertEquals(0.12D, survivor.getAttribute(Attribute.MOVEMENT_SPEED).getValue(), EPSILON);
    }

    @Test
    @DisplayName("A hidden slender gives no rush")
    void noRushWhileHidden(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = connect(env, instance, new Pos(0, 40, 0));
        Player survivor = connect(env, instance, new Pos(5, 40, 0));
        this.survivors = Set.of(survivor);
        AdrenalineService service = service(env);

        slenderBecomes(slender, StaminaBar.State.DRAINING);
        slenderBecomes(slender, StaminaBar.State.REGENERATING);
        service.tick();

        assertFalse(rushing(survivor));
    }

    @Test
    @DisplayName("A survivor outside the radius gets no rush")
    void noRushOutOfRange(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = connect(env, instance, new Pos(0, 40, 0));
        Player survivor = connect(env, instance, new Pos(9, 40, 0));
        this.survivors = Set.of(survivor);
        AdrenalineService service = service(env);

        slenderBecomes(slender, StaminaBar.State.DRAINING);
        service.tick();

        assertFalse(rushing(survivor));
    }

    @Test
    @DisplayName("The rush ends after its duration, even while the slender stays close")
    void endsAfterDuration(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = connect(env, instance, new Pos(0, 40, 0));
        Player survivor = connect(env, instance, new Pos(5, 40, 0));
        this.survivors = Set.of(survivor);
        AdrenalineService service = service(env);
        slenderBecomes(slender, StaminaBar.State.DRAINING);
        service.tick();

        this.clock.set(3_999);
        service.tick();
        assertTrue(rushing(survivor));

        this.clock.set(4_000);
        service.tick();
        assertFalse(rushing(survivor));
        assertEquals(0.1D, survivor.getAttribute(Attribute.MOVEMENT_SPEED).getValue(), EPSILON);
    }

    @Test
    @DisplayName("The next rush waits for the cooldown after the last one")
    void waitsForCooldown(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = connect(env, instance, new Pos(0, 40, 0));
        Player survivor = connect(env, instance, new Pos(5, 40, 0));
        this.survivors = Set.of(survivor);
        AdrenalineService service = service(env);
        slenderBecomes(slender, StaminaBar.State.DRAINING);
        service.tick();

        // 4 s rush, then 20 s cooldown
        this.clock.set(23_999);
        service.tick();
        assertFalse(rushing(survivor));

        this.clock.set(24_000);
        service.tick();
        assertTrue(rushing(survivor));
    }

    @Test
    @DisplayName("Dying ends the rush")
    void deathEndsRush(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = connect(env, instance, new Pos(0, 40, 0));
        Player survivor = connect(env, instance, new Pos(5, 40, 0));
        this.survivors = Set.of(survivor);
        AdrenalineService service = service(env);
        slenderBecomes(slender, StaminaBar.State.DRAINING);
        service.tick();

        EventDispatcher.call(new PlayerDeathEvent(survivor, Component.empty(), Component.empty()));

        assertFalse(rushing(survivor));
    }

    @Test
    @DisplayName("The end of the round clears the rush and the cooldown")
    void finishClearsEverything(Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = connect(env, instance, new Pos(0, 40, 0));
        Player survivor = connect(env, instance, new Pos(5, 40, 0));
        this.survivors = Set.of(survivor);
        AdrenalineService service = service(env);
        slenderBecomes(slender, StaminaBar.State.DRAINING);
        service.tick();

        EventDispatcher.call(new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER));

        assertFalse(rushing(survivor));
        assertNull(survivor.getTag(AdrenalineService.RUSH_UNTIL));
        assertNull(survivor.getTag(AdrenalineService.COOLDOWN_UNTIL));
    }
}
