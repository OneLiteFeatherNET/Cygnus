package net.onelitefeather.cygnus.team;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.packet.server.play.PlayerInfoUpdatePacket;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.common.config.GameConfigReader;
import net.minestom.testing.Collector;
import net.minestom.testing.TestConnection;
import net.minestom.server.coordinate.Pos;
import java.nio.file.Paths;
import net.onelitefeather.cygnus.event.SlenderReviveEvent;
import net.onelitefeather.cygnus.listener.game.SlenderReviveListener;
import net.onelitefeather.cygnus.spectator.SpectatorService;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.theevilreaper.xerus.api.team.Team;
import net.theevilreaper.xerus.api.team.TeamService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that every role transition puts the player to the matching place of the tab list.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
class TabOrderTest extends CygnusPlayerTestBase {

    @Test
    void testRolesAreOrderedSlenderBeforeSurvivorBeforeSpectator() {
        assertTrue(TabOrder.SLENDER > TabOrder.SURVIVOR, "the slender has to be listed above the survivors");
        assertTrue(TabOrder.SURVIVOR > TabOrder.SPECTATOR, "the survivors have to be listed above the dead");
    }

    @Test
    void testUnknownRoleIsListedLikeASpectator() {
        assertEquals(TabOrder.SPECTATOR, TabOrder.of(null));
    }

    @Test
    void testUpdateTabListOrdersSlenderAboveSurvivors(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player slender = env.createPlayer(instance);
        Player survivor = env.createPlayer(instance);
        TeamService teamService = TeamService.of();
        new TeamCreator() {
        }.createTeams(new GameConfigReader(Paths.get("")).getConfig().teams(), teamService);
        teamService.getTeam(GameConfig.SLENDER_KEY).orElseThrow().addPlayer(slender);
        teamService.getTeam(GameConfig.SURVIVOR_KEY).orElseThrow().addPlayer(survivor);

        TeamHelper.updateTabList(teamService);

        assertEquals(TabOrder.SLENDER, slender.getListOrder(), "the slender is listed first");
        assertEquals(TabOrder.SURVIVOR, survivor.getListOrder(), "a living survivor is listed after the slender");

        env.destroyInstance(instance, true);
    }

    @Test
    void testDeathMovesSurvivorToTheDeadGroup(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createPlayer(instance);
        survivor.setListOrder(TabOrder.SURVIVOR);
        SpectatorService service = new SpectatorService(Team.of(GameConfig.SPECTATOR_KEY, 5), Team.of(GameConfig.SURVIVOR_KEY, 5));

        service.join(survivor);

        assertEquals(TabOrder.SPECTATOR, survivor.getListOrder(), "a dead player is listed last");

        env.destroyInstance(instance, true);
    }

    @Test
    void testSlenderRevivePromotesTheNewSlenderToTheTop(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createPlayer(instance);
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        survivor.setListOrder(TabOrder.SURVIVOR);
        SlenderReviveListener listener = new SlenderReviveListener(() -> null, new StaminaService(), TeamService.of());

        listener.accept(new SlenderReviveEvent(survivor));

        assertEquals(TabOrder.SLENDER, survivor.getListOrder(), "the new slender is listed first");

        env.destroyInstance(instance, true);
    }

    @Test
    void testChangeIsSentToTheClients(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection connection = env.createConnection();
        Player player = connection.connect(instance, Pos.ZERO);
        player.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        Collector<PlayerInfoUpdatePacket> collector = connection.trackIncoming(PlayerInfoUpdatePacket.class);

        TabOrder.apply(player);

        boolean sent = collector.collect().stream().anyMatch(packet ->
                packet.actions().contains(PlayerInfoUpdatePacket.Action.UPDATE_LIST_ORDER)
                        && packet.entries().stream().anyMatch(entry -> entry.listOrder() == TabOrder.SLENDER));
        assertTrue(sent, "the new list order must reach the clients");

        env.destroyInstance(instance, true);
    }
}
