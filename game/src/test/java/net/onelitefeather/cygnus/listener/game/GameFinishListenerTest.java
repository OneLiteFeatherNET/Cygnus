package net.onelitefeather.cygnus.listener.game;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the round-end cleanup that used to be missing: a player who survived to the round's
 * natural end (as opposed to dying or quitting, which already clear this via
 * {@link net.onelitefeather.cygnus.listener.PlayerDeathListener} and
 * {@link net.onelitefeather.cygnus.listener.PlayerQuitListener}) kept carrying
 * {@link Tags#TEAM_KEY} and the leftover SlenderEye item into the {@code RestartPhase} countdown,
 * which let the item and sprinting keep behaving as if the round were still live.
 */
@Disabled
class GameFinishListenerTest extends CygnusPlayerTestBase {

    @Test
    void testSlenderLosesTeamTagAndItemsOnRoundFinish(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer slender = (CygnusPlayer) env.createPlayer(instance);
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        slender.getInventory().addItemStack(ItemStack.of(Material.ENDER_EYE));

        GameFinishListener listener = new GameFinishListener();
        listener.accept(new GameFinishEvent(GameFinishEvent.Reason.ALL_PAGES_FOUND));

        assertFalse(slender.hasTag(Tags.TEAM_KEY), "the slender role must not survive round end");
        assertTrue(slender.getInventory().getItemStack(0).isAir(), "the leftover SlenderEye must be gone");

        env.destroyInstance(instance, true);
    }

    @Test
    void testSurvivorLosesTeamTagOnRoundFinish(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer survivor = (CygnusPlayer) env.createPlayer(instance);
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);

        GameFinishListener listener = new GameFinishListener();
        listener.accept(new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER));

        assertFalse(survivor.hasTag(Tags.TEAM_KEY), "the survivor role must not survive round end");

        env.destroyInstance(instance, true);
    }

    @Test
    void testSpectatorKeepsItsTagOnRoundFinish(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player spectator = env.createPlayer(instance);
        spectator.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);

        GameFinishListener listener = new GameFinishListener();
        listener.accept(new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER));

        assertTrue(spectator.hasTag(Tags.TEAM_KEY), "spectators are not part of the round-end reset");

        env.destroyInstance(instance, true);
    }
}
