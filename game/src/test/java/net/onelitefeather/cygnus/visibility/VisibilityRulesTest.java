package net.onelitefeather.cygnus.visibility;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.stamina.SlenderBarHelper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the role visibility matrix implemented by {@link VisibilityRules}.
 * <p>
 * Every test asserts through {@code target.isViewer(viewer)}, which answers "is {@code viewer} able to see
 * {@code target}" - exactly the cell of the matrix under test.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.8.0
 */
class VisibilityRulesTest extends CygnusPlayerTestBase {

    private static final int SETTLE_TICKS = 5;

    @Test
    void testSpectatorSeesOtherSpectator(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player first = env.createPlayer(instance);
        Player second = env.createPlayer(instance);
        settle(env);

        makeSpectator(first);
        makeSpectator(second);

        assertTrue(first.isViewer(second), "a spectator must stay visible for other spectators");
        assertTrue(second.isViewer(first), "a spectator must stay visible for other spectators");

        env.destroyInstance(instance, true);
    }

    @Test
    void testSpectatorSeesSurvivor(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createPlayer(instance);
        Player spectator = env.createPlayer(instance);
        settle(env);

        makeSurvivor(survivor);
        makeSpectator(spectator);

        assertTrue(survivor.isViewer(spectator), "survivors carry no rule and must remain visible for spectators");

        env.destroyInstance(instance, true);
    }

    @Test
    void testSurvivorDoesNotSeeSpectator(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createPlayer(instance);
        Player spectator = env.createPlayer(instance);
        settle(env);

        makeSurvivor(survivor);
        makeSpectator(spectator);

        assertFalse(spectator.isViewer(survivor), "a player still in the round must not see spectators");

        env.destroyInstance(instance, true);
    }

    @Test
    void testSlenderDoesNotSeeSpectator(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance);
        Player spectator = env.createPlayer(instance);
        settle(env);

        makeSlender(slender);
        makeSpectator(spectator);

        assertFalse(spectator.isViewer(slender), "the slender must not see spectators either");

        env.destroyInstance(instance, true);
    }

    @Test
    void testSpectatorDoesNotSeeHiddenSlender(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance);
        Player spectator = env.createPlayer(instance);
        settle(env);

        makeSlender(slender);
        makeSpectator(spectator);

        assertTrue(VisibilityRules.isHidden(slender), "the slender starts the round hidden");
        assertFalse(slender.isViewer(spectator), "spectators share the survivor view and must not see a hidden slender");

        env.destroyInstance(instance, true);
    }

    @Test
    void testSpectatorSeesRevealedSlender(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance);
        Player spectator = env.createPlayer(instance);
        settle(env);

        makeSlender(slender);
        makeSpectator(spectator);
        assertFalse(slender.isViewer(spectator), "precondition: the slender is hidden");

        slender.setTag(Tags.HIDDEN, SlenderBarHelper.VISIBLE);
        VisibilityRules.refresh(slender);

        assertTrue(slender.isViewer(spectator), "spectators must see the slender while the slender is revealed");

        env.destroyInstance(instance, true);
    }

    private static void settle(@NotNull Env env) {
        for (int i = 0; i < SETTLE_TICKS; i++) {
            env.tick();
        }
    }

    private static void makeSlender(@NotNull Player player) {
        player.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        player.setTag(Tags.HIDDEN, SlenderBarHelper.HIDDEN);
        player.updateViewableRule(VisibilityRules.slenderRule(player));
        VisibilityRules.refresh(player);
    }

    private static void makeSurvivor(@NotNull Player player) {
        player.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        VisibilityRules.refresh(player);
    }

    private static void makeSpectator(@NotNull Player player) {
        player.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);
        player.updateViewableRule(VisibilityRules.spectatorRule());
        VisibilityRules.refresh(player);
    }
}
