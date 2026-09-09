package net.onelitefeather.cygnus.spectator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.AbstractInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.theevilreaper.xerus.api.team.Team;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.UUID;

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

    /**
     * A survivor can die before anyone has ever opened the overview - the player who just died
     * is the one whose death triggers {@link SpectatorService#updateInventory()}, and they can't
     * possibly have the overview open at that exact moment; it only opens later via the spectate
     * item. This must still self-heal correctly the first time someone opens it.
     */
    @Test
    void testOverviewDropsSurvivorsWhoDiedBeforeAnyoneEverOpenedIt(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player survivorA = env.createPlayer(instance);
        Player survivorB = env.createPlayer(instance);
        Player survivorC = env.createPlayer(instance);
        Player spectator = env.createPlayer(instance);

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        survivorTeam.addPlayer(survivorA);
        survivorTeam.addPlayer(survivorB);
        survivorTeam.addPlayer(survivorC);

        SpectatorService service = new SpectatorService(spectatorTeam, survivorTeam);

        // First death: nobody has the overview open yet.
        survivorTeam.removePlayer(survivorA);
        service.updateInventory();

        // Second death, still before anyone has opened the overview.
        survivorTeam.removePlayer(survivorB);
        service.updateInventory();

        // Now a spectator finally opens the overview for the first time.
        service.openOverview(spectator);
        for (int i = 0; i < 5; i++) env.tick();

        AbstractInventory opened = spectator.getOpenInventory();
        assertNotNull(opened, "the overview must actually open");

        assertTrue(containsTarget(opened, survivorC.getUuid()),
                "the still-alive survivor must be selectable in the overview");
        assertFalse(containsTarget(opened, survivorA.getUuid()),
                "a survivor who died before the overview was ever opened must not linger in it");
        assertFalse(containsTarget(opened, survivorB.getUuid()),
                "a survivor who died before the overview was ever opened must not linger in it");

        env.destroyInstance(instance, true);
    }

    /**
     * Reproduces the report that "after the first invalidate, nothing happens anymore": a survivor
     * who dies while a spectator already has the overview open must disappear from it too, not
     * linger forever. Root cause: {@code GlobalInventoryBuilder.applyDataLayout()} in Aves only
     * calls {@code inventory.setItemStack(i, stack)} for non-air stacks, so a blanked slot (the
     * layout function writes {@code null} for it) never actually overwrites the stale item already
     * sitting in the real inventory.
     */
    @Test
    void testOverviewDropsASurvivorWhoDiesWhileAlreadyOpen(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player survivor = env.createPlayer(instance);
        Player spectator = env.createPlayer(instance);

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        survivorTeam.addPlayer(survivor);

        SpectatorService service = new SpectatorService(spectatorTeam, survivorTeam);

        service.openOverview(spectator);
        for (int i = 0; i < 5; i++) env.tick();

        AbstractInventory opened = spectator.getOpenInventory();
        assertTrue(containsTarget(opened, survivor.getUuid()),
                "precondition: the survivor must be selectable before they die");

        // The survivor dies while the spectator already has the overview open.
        survivorTeam.removePlayer(survivor);
        service.updateInventory();
        for (int i = 0; i < 5; i++) env.tick();

        assertFalse(containsTarget(opened, survivor.getUuid()),
                "a survivor who dies while the overview is already open must be removed from it too");

        env.destroyInstance(instance, true);
    }

    private static boolean containsTarget(AbstractInventory inventory, UUID target) {
        for (ItemStack stack : inventory.getItemStacks()) {
            if (target.equals(stack.getTag(SpectatorInventory.TARGET_TAG))) {
                return true;
            }
        }
        return false;
    }
}
