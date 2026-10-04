package net.onelitefeather.cygnus.listener.game;

import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.EntitySoundEffectPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameFinishSoundTest extends CygnusPlayerTestBase {

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
}
