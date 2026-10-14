package net.onelitefeather.cygnus.listener.game;

import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.EntitySoundEffectPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the win and loss sounds each side hears, and the round-end cleanup that is still
 * missing: a player who survived to the round's natural end (as opposed to dying or quitting,
 * which already clear this via {@link net.onelitefeather.cygnus.listener.PlayerDeathListener} and
 * {@link net.onelitefeather.cygnus.listener.PlayerQuitListener}) keeps carrying
 * {@link Tags#TEAM_KEY} and the leftover SlenderEye item into the {@code RestartPhase} countdown,
 * which lets the item and sprinting keep behaving as if the round were still live.
 */
class GameFinishListenerTest extends CygnusPlayerTestBase {

    private static List<Key> sounds(Collector<EntitySoundEffectPacket> packets) {
        return packets.collect().stream().map(packet -> packet.soundEvent().key()).toList();
    }

    @Test
    @DisplayName("When the slender wins, he hears the win and the survivors hear the loss")
    void slenderWins(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection slenderConnection = env.createConnection();
        Player slender = slenderConnection.connect(instance, new Pos(0, 40, 0));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        TestConnection survivorConnection = env.createConnection();
        Player survivor = survivorConnection.connect(instance, new Pos(5, 40, 0));
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        Collector<EntitySoundEffectPacket> slenderSounds = slenderConnection.trackIncoming(EntitySoundEffectPacket.class);
        Collector<EntitySoundEffectPacket> survivorSounds = survivorConnection.trackIncoming(EntitySoundEffectPacket.class);

        new GameFinishListener().accept(new GameFinishEvent(GameFinishEvent.Reason.ALL_SURVIVOR_DEAD, slender));

        assertEquals(List.of(GameFinishListener.WIN_SOUND), sounds(slenderSounds));
        assertEquals(List.of(GameFinishListener.LOSS_SOUND), sounds(survivorSounds));
    }

    @Test
    @DisplayName("When the survivors win, they hear the win and the slender hears the loss")
    void survivorsWin(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection slenderConnection = env.createConnection();
        Player slender = slenderConnection.connect(instance, new Pos(0, 40, 0));
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        TestConnection spectatorConnection = env.createConnection();
        Player spectator = spectatorConnection.connect(instance, new Pos(5, 40, 0));
        spectator.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);
        Collector<EntitySoundEffectPacket> slenderSounds = slenderConnection.trackIncoming(EntitySoundEffectPacket.class);
        Collector<EntitySoundEffectPacket> spectatorSounds = spectatorConnection.trackIncoming(EntitySoundEffectPacket.class);

        new GameFinishListener().accept(new GameFinishEvent(GameFinishEvent.Reason.ALL_PAGES_FOUND));

        assertEquals(List.of(GameFinishListener.LOSS_SOUND), sounds(slenderSounds));
        assertEquals(List.of(GameFinishListener.WIN_SOUND), sounds(spectatorSounds), "a dead survivor wins with them");
    }

    @Disabled("GameFinishListener does not clear the round state yet")
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

    @Disabled("GameFinishListener does not clear the round state yet")
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
