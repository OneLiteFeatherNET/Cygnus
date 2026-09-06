package net.onelitefeather.cygnus.spectator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.attribute.Attribute;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.attribute.AttributeHelper;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.theevilreaper.xerus.api.team.Team;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpectatorServiceTest extends CygnusPlayerTestBase {

    @Test
    void testJoinSetsGameModeTeamTagAndItems(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        SpectatorService service = new SpectatorService(spectatorTeam, survivorTeam);

        service.join(player);

        assertEquals(GameMode.SURVIVAL, player.getGameMode());
        assertTrue(spectatorTeam.getPlayers().contains(player));
        assertEquals(GameConfig.SPECTATOR_KEY, player.getTag(Tags.TEAM_KEY));
        assertEquals(Material.COMPASS, player.getInventory().getItemStack(2).material());
        assertEquals(Material.OAK_DOOR, player.getInventory().getItemStack(5).material());

        env.destroyInstance(instance, true);
    }

    @Test
    void testJoinHidesSpectatorFromPlayersStillInTheRound(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player target = env.createPlayer(instance);
        Player other = env.createPlayer(instance);
        other.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);

        for (int i = 0; i < 5; i++) env.tick();
        assertTrue(target.isViewer(other), "the target should be visible to the other player before joining spectator mode");

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        SpectatorService service = new SpectatorService(spectatorTeam, survivorTeam);

        service.join(target);

        assertFalse(target.isViewer(other), "a spectator must not be visible to players who are still in the round");
        assertTrue(other.isViewer(target), "a spectator must keep seeing the players who are still in the round");

        env.destroyInstance(instance, true);
    }

    @Test
    void testJoinKeepsSpectatorsVisibleToEachOther(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player first = env.createPlayer(instance);
        Player second = env.createPlayer(instance);

        for (int i = 0; i < 5; i++) env.tick();

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        SpectatorService service = new SpectatorService(spectatorTeam, survivorTeam);

        service.join(first);
        service.join(second);

        assertTrue(first.isViewer(second), "spectators must see each other");
        assertTrue(second.isViewer(first), "spectators must see each other");

        env.destroyInstance(instance, true);
    }

    @Test
    void testTeleportToMovesSpectatorToTargetPosition(@NotNull Env env) throws Exception {
        Instance instance = env.createFlatInstance();
        Player spectator = env.createPlayer(instance);
        Player target = env.createPlayer(instance);
        target.teleport(new Pos(15, 41, 15)).get();

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        SpectatorService service = new SpectatorService(spectatorTeam, survivorTeam);

        service.teleportTo(spectator, target).get();

        assertEquals(target.getPosition(), spectator.getPosition());

        env.destroyInstance(instance, true);
    }

    @Test
    void testIsSpectatorReflectsTeamTag(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        SpectatorService service = new SpectatorService(spectatorTeam, survivorTeam);

        assertFalse(service.isSpectator(player));
        service.join(player);
        assertTrue(service.isSpectator(player));

        env.destroyInstance(instance, true);
    }

    @Test
    void testOpenOverviewOpensSpectatorInventory(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        SpectatorService service = new SpectatorService(spectatorTeam, survivorTeam);

        service.openOverview(player);

        assertNotNull(player.getOpenInventory());

        env.destroyInstance(instance, true);
    }

    @Test
    void testJoinKeepsTheSpectatorAbleToFlyAfterTouchingTheGround(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        SpectatorService service = new SpectatorService(spectatorTeam, survivorTeam);

        service.join(player);

        assertTrue(player.isFlying(), "a spectator starts in the air");
        assertTrue(player.isAllowFlying(), "without the flight permission the client drops flight on landing and cannot take off again");

        env.destroyInstance(instance, true);
    }

    @Test
    void testJoinClearsTheStaminaHudOfTheFormerSurvivor(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        CygnusPlayer player = (CygnusPlayer) env.createPlayer(instance);
        player.setExp(0.5f);
        player.setBlockedSprinting(true);

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        SpectatorService service = new SpectatorService(spectatorTeam, survivorTeam);

        service.join(player);

        assertEquals(0.0f, player.getExp(), "a spectator must not keep the survivor stamina bar");
        assertFalse(player.hasBlockedSprinting(), "a spectator must not keep the survivor sprint cooldown");

        env.destroyInstance(instance, true);
    }

    @Test
    void testJoinStrikesTheTabListNameThrough(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.setDisplayName(Component.text(player.getUsername(), NamedTextColor.GREEN));

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        SpectatorService service = new SpectatorService(spectatorTeam, survivorTeam);

        service.join(player);

        Component displayName = player.getDisplayName();
        assertNotNull(displayName, "a spectator needs a display name to show up in the tab list");
        assertEquals(
                Component.text(player.getUsername(), NamedTextColor.GRAY, TextDecoration.STRIKETHROUGH),
                displayName,
                "a spectator must be struck through in gray instead of keeping the green survivor name"
        );

        env.destroyInstance(instance, true);
    }

    @Test
    void testJoinHandsTheRoundAttributesBack(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        AttributeHelper.adjustStepHeightAndJump(player);
        AttributeHelper.decreaseSpeed(player);
        AttributeHelper.updateSpeedScale(player, 0.02);
        AttributeHelper.updateHealthScale(player, 6.0f);

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        SpectatorService service = new SpectatorService(spectatorTeam, survivorTeam);

        service.join(player);

        assertEquals(0.42, player.getAttribute(Attribute.JUMP_STRENGTH).getValue(), 0.0001, "a spectator must be able to jump again");
        assertEquals(0.6, player.getAttribute(Attribute.STEP_HEIGHT).getValue(), 0.0001);
        assertEquals(0.1, player.getAttribute(Attribute.MOVEMENT_SPEED).getValue(), 0.0001, "a spectator must not keep the lowered survivor speed");
        assertEquals(20.0, player.getAttribute(Attribute.MAX_HEALTH).getValue(), 0.0001);

        env.destroyInstance(instance, true);
    }
}
