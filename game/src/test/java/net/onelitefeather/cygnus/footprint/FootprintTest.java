package net.onelitefeather.cygnus.footprint;

import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.team.TeamHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FootprintTest extends CygnusPlayerTestBase {

    private static final BlockVec BLOCK = new BlockVec(0, 40, 0);

    private static Footprint print(Instance instance, int lifetime, int fadeAfter) {
        return new Footprint(instance, BLOCK, FootprintKind.SLENDER, "north", lifetime, fadeAfter,
                TeamHelper::isSurvivorTeam);
    }

    @Test
    @DisplayName("The kinds map to their petal states")
    void states() {
        Block bright = FootprintKind.SLENDER.block("east", false);
        assertEquals("1", bright.getProperty("flower_amount"));
        assertEquals("east", bright.getProperty("facing"));
        assertEquals("2", FootprintKind.SURVIVOR.block("north", false).getProperty("flower_amount"));
        assertEquals("3", FootprintKind.SLENDER.block("north", true).getProperty("flower_amount"));
        assertEquals("4", FootprintKind.SURVIVOR.block("north", true).getProperty("flower_amount"));
    }

    @Test
    @DisplayName("Only players the rule lets through get the fake block")
    void viewerRule(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection survivorConnection = env.createConnection();
        Player survivor = survivorConnection.connect(instance, new Pos(0.5, 40, 3.5));
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        TestConnection spectatorConnection = env.createConnection();
        Player spectator = spectatorConnection.connect(instance, new Pos(1.5, 40, 3.5));
        spectator.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);
        Collector<BlockChangePacket> survivorPackets = survivorConnection.trackIncoming(BlockChangePacket.class);
        Collector<BlockChangePacket> spectatorPackets = spectatorConnection.trackIncoming(BlockChangePacket.class);
        Footprint footprint = print(instance, 12, 8);

        footprint.refresh();

        survivorPackets.assertSingle(packet -> {
            assertEquals(BLOCK, packet.blockPosition());
            assertEquals(footprint.state().stateId(), packet.blockStateId());
        });
        spectatorPackets.assertEmpty();
        assertTrue(footprint.isShownTo(survivor));
        assertFalse(footprint.isShownTo(spectator));
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("A player who no longer matches gets the real block back")
    void refreshHides(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player survivor = connection.connect(instance, new Pos(0.5, 40, 3.5));
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        Footprint footprint = print(instance, 12, 8);
        footprint.refresh();
        Collector<BlockChangePacket> packets = connection.trackIncoming(BlockChangePacket.class);

        survivor.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);
        footprint.refresh();

        packets.assertSingle(packet -> assertEquals(Block.AIR.stateId(), packet.blockStateId()));
        assertFalse(footprint.isShownTo(survivor));
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("The print fades once and then goes, giving the real block back")
    void fadesThenGoes(Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player survivor = connection.connect(instance, new Pos(0.5, 40, 3.5));
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        Footprint footprint = print(instance, 3, 2);
        footprint.refresh();

        Collector<BlockChangePacket> first = connection.trackIncoming(BlockChangePacket.class);
        assertFalse(footprint.second());
        first.assertEmpty();

        Collector<BlockChangePacket> fade = connection.trackIncoming(BlockChangePacket.class);
        assertFalse(footprint.second());
        assertTrue(footprint.faded());
        fade.assertSingle(packet -> assertEquals(FootprintKind.SLENDER.block("north", true).stateId(),
                packet.blockStateId()));

        Collector<BlockChangePacket> gone = connection.trackIncoming(BlockChangePacket.class);
        assertTrue(footprint.second());
        gone.assertSingle(packet -> assertEquals(Block.AIR.stateId(), packet.blockStateId()));
        assertFalse(footprint.isShownTo(survivor));
        env.destroyInstance(instance, true);
    }
}
