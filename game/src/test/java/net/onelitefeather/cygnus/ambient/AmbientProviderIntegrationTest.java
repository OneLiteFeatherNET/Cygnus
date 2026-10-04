package net.onelitefeather.cygnus.ambient;

import net.kyori.adventure.key.Key;
import net.minestom.server.network.packet.server.play.SoundEffectPacket;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.TestConnection;
import net.theevilreaper.xerus.api.team.Team;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.random.RandomGenerator;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class AmbientProviderIntegrationTest {

    @Test
    void testNoSoundBeforeTheBlackout(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Team team = Team.of(Key.key("cygnus", "test"));
        team.addPlayer(player);

        // The cave sounds moved to the player tick, paced by each survivor's fear.
        AmbientProvider provider = new AmbientProvider(team, fixedRandom(0));
        Collector<SoundEffectPacket> sounds = connection.trackIncoming(SoundEffectPacket.class);

        for (int i = 0; i < 99; i++) {
            provider.tick();
        }

        sounds.assertEmpty();
        env.destroyInstance(instance, true);
    }

    @Test
    void testBlackoutDoesNotFireBeforeTheRolledInterval(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Team team = Team.of(Key.key("cygnus", "test"));
        team.addPlayer(player);

        AmbientProvider provider = new AmbientProvider(team, fixedRandom(0));
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);

        for (int i = 0; i < 99; i++) {
            provider.tick();
        }

        assertTrue(messages.collect().isEmpty(), "Blackout should not have fired before the rolled 100s interval");
        env.destroyInstance(instance, true);
    }

    @Test
    void testBlackoutFiresAfterTheRolledInterval(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Team team = Team.of(Key.key("cygnus", "test"));
        team.addPlayer(player);

        AmbientProvider provider = new AmbientProvider(team, fixedRandom(0));
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);

        for (int i = 0; i < 100; i++) {
            provider.tick();
        }

        assertFalse(messages.collect().isEmpty(), "Blackout should have fired at the rolled 100s interval");
        env.destroyInstance(instance, true);
    }

    @Test
    void testSecondBlackoutDoesNotFireBeforeItsOwnRolledInterval(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Team team = Team.of(Key.key("cygnus", "test"));
        team.addPlayer(player);

        AmbientProvider provider = new AmbientProvider(team, fixedRandom(0));
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);

        // First blackout fires at tick 100; one tick short of a second 100-tick interval after that.
        for (int i = 0; i < 199; i++) {
            provider.tick();
        }

        assertEquals(1, messages.collect().size(), "Only the first blackout should have fired so far");
        env.destroyInstance(instance, true);
    }

    @Test
    void testSecondBlackoutFiresAfterItsOwnRolledInterval(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance);
        Team team = Team.of(Key.key("cygnus", "test"));
        team.addPlayer(player);

        AmbientProvider provider = new AmbientProvider(team, fixedRandom(0));
        Collector<SystemChatPacket> messages = connection.trackIncoming(SystemChatPacket.class);

        // First blackout fires at tick 100, second at tick 200 once the interval is rerolled.
        for (int i = 0; i < 200; i++) {
            provider.tick();
        }

        assertEquals(2, messages.collect().size(), "Both the first and the rerolled second blackout should have fired");
        env.destroyInstance(instance, true);
    }

    private static RandomGenerator fixedRandom(int nextIntValue) {
        return new RandomGenerator() {
            @Override
            public long nextLong() {
                throw new UnsupportedOperationException("not used by AmbientProvider");
            }

            @Override
            public int nextInt(int bound) {
                return nextIntValue;
            }
        };
    }

    @Test
    void testObserverHearsTheBlackoutWithTheDurationAndTheRolledInterval(Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createConnection().connect(instance);
        Team team = Team.of(Key.key("cygnus", "test"));
        team.addPlayer(player);
        java.util.List<Object[]> heard = new java.util.ArrayList<>();

        AmbientProvider provider = new AmbientProvider(team, fixedRandom(0),
                (affected, duration, next) -> heard.add(new Object[]{affected, duration, next}));

        for (int i = 0; i < 99; i++) {
            provider.tick();
        }
        assertTrue(heard.isEmpty(), "nothing is reported before the blackout");
        provider.tick();

        assertEquals(1, heard.size());
        assertEquals(java.util.List.of(player), heard.getFirst()[0]);
        assertEquals(200, heard.getFirst()[1]);
        assertEquals(100, heard.getFirst()[2], "the interval just rolled until the next blackout");
        env.destroyInstance(instance, true);
    }
}
