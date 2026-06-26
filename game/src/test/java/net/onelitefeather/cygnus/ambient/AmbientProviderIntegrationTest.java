package net.onelitefeather.cygnus.ambient;

import net.kyori.adventure.key.Key;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.TestConnection;
import net.theevilreaper.xerus.api.team.Team;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class AmbientProviderIntegrationTest {

    @Test
    void testCaveSoundAtTickZero(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Team team = Team.of(Key.key("cygnus", "test"));
        team.addPlayer(player);

        AmbientProvider provider = new AmbientProvider(team);
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        provider.tick();
        assertFalse(sounds.collect().isEmpty(), "Cave sound should have been played at tick 0");
        env.destroyInstance(instance, true);
    }
}