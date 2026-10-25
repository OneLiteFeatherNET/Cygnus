package net.onelitefeather.cygnus.spectator;

import net.kyori.adventure.text.Component;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.player.event.SpectatorStatsRequestEvent;
import net.onelitefeather.cygnus.player.event.SpectatorTeleportRequestEvent;
import net.onelitefeather.cygnus.utils.HealthDisplay;
import net.theevilreaper.xerus.api.team.Team;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpectatorInventoryTest extends CygnusPlayerTestBase {

    private Instance instance;
    private EventNode<Event> requestNode;
    private List<SpectatorStatsRequestEvent> statsRequests;
    private List<Player> teleportedTo;

    @BeforeEach
    void setUp(Env env) {
        this.instance = env.createFlatInstance();
        // The Env lives for the whole class, so a listener from env.listen would leak into later tests
        this.statsRequests = new ArrayList<>();
        this.teleportedTo = new ArrayList<>();
        this.requestNode = EventNode.all("spectator-requests");
        this.requestNode.addListener(SpectatorStatsRequestEvent.class, statsRequests::add);
        this.requestNode.addListener(SpectatorTeleportRequestEvent.class, event -> teleportedTo.add(event.getTarget()));
        env.process().eventHandler().addChild(this.requestNode);
    }

    @AfterEach
    void tearDown(Env env) {
        env.process().eventHandler().removeChild(this.requestNode);
        env.destroyInstance(this.instance, true);
    }

    @Test
    void testOpenShowsInventoryToPlayer(@NotNull Env env) {
        Player player = env.createPlayer(instance);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);

        SpectatorInventory inventory = new SpectatorInventory(survivorTeam, spectatorTeam());
        inventory.open(player);

        assertEquals(inventory.getInventory(), player.getOpenInventory());
    }

    @Test
    void testHandleClickInvokesTeleportCallbackWithClickedTarget(@NotNull Env env) {
        Player spectator = env.createPlayer(instance);
        Player target = env.createPlayer(instance);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        survivorTeam.addPlayer(target);

        SpectatorInventory inventory = new SpectatorInventory(survivorTeam, spectatorTeam());

        ItemStack headStack = ItemStack.builder(Material.PLAYER_HEAD)
                .set(SpectatorInventory.TARGET_TAG, target.getUuid())
                .build();

        inventory.handleClick(spectator, 10, null, headStack, _ -> {});

        assertEquals(1, teleportedTo.size());
        assertEquals(target, teleportedTo.getFirst());
    }

    @Test
    void testHandleClickIsNoOpWhenTargetIsOffline(@NotNull Env env) {
        Player spectator = env.createPlayer(instance);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);

        SpectatorInventory inventory = new SpectatorInventory(survivorTeam, spectatorTeam());

        ItemStack headStack = ItemStack.builder(Material.PLAYER_HEAD)
                .set(SpectatorInventory.TARGET_TAG, UUID.randomUUID())
                .build();

        inventory.handleClick(spectator, 10, null, headStack, _ -> {});

        assertTrue(teleportedTo.isEmpty());
    }

    @Test
    void testOpenPopulatesPlayerHeadsEvenWhenSkinIsNull(@NotNull Env env) {
        Player spectator = env.createPlayer(instance);
        Player survivor = env.createPlayer(instance);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        survivorTeam.addPlayer(survivor);

        SpectatorInventory inventory = new SpectatorInventory(survivorTeam, spectatorTeam());
        inventory.open(spectator);
        env.tick();

        ItemStack item = inventory.getInventory().getItemStack(9);
        assertEquals(Material.PLAYER_HEAD, item.material());
        assertEquals(survivor.getUuid(), item.getTag(SpectatorInventory.TARGET_TAG));
        assertEquals(Component.text(survivor.getUsername()), item.get(DataComponents.CUSTOM_NAME));
    }

    @Test
    void testShrinkingSurvivorListClearsStaleHeadInFreedSlot(@NotNull Env env) {
        Player spectator = env.createPlayer(instance);
        Player first = env.createPlayer(instance);
        Player second = env.createPlayer(instance);
        Player third = env.createPlayer(instance);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        survivorTeam.addPlayer(first);
        survivorTeam.addPlayer(second);
        survivorTeam.addPlayer(third);

        SpectatorInventory inventory = new SpectatorInventory(survivorTeam, spectatorTeam());
        inventory.open(spectator);
        env.tick();
        assertEquals(3, countHeads(inventory), "all survivors should be listed initially");

        survivorTeam.removePlayer(first);
        inventory.invalidateDataLayout();
        env.tick();

        assertEquals(2, countHeads(inventory), "each remaining survivor must be listed exactly once");
        assertTrue(inventory.getInventory().getItemStack(11).isAir(), "freed slot must be empty");
        assertNull(inventory.getInventory().getItemStack(11).getTag(SpectatorInventory.TARGET_TAG),
                "freed slot must not keep a stale target");
    }

    @Test
    void testGrowingSurvivorListAddsHeads(@NotNull Env env) {
        Player spectator = env.createPlayer(instance);
        Player first = env.createPlayer(instance);
        Player second = env.createPlayer(instance);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        survivorTeam.addPlayer(first);

        SpectatorInventory inventory = new SpectatorInventory(survivorTeam, spectatorTeam());
        inventory.open(spectator);
        env.tick();
        assertEquals(1, countHeads(inventory));

        survivorTeam.addPlayer(second);
        inventory.invalidateDataLayout();
        env.tick();

        assertEquals(2, countHeads(inventory));
    }

    @Test
    void testHandleClickWithoutTargetIsNoOp(@NotNull Env env) {
        Player spectator = env.createPlayer(instance);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        SpectatorInventory inventory = new SpectatorInventory(survivorTeam, spectatorTeam());

        inventory.handleClick(spectator, 11, null, ItemStack.AIR, _ -> {});

        assertTrue(teleportedTo.isEmpty());
    }

    @Test
    void testRightClickOpensStatsInsteadOfTeleporting(@NotNull Env env) {
        Player spectator = env.createPlayer(instance);
        Player target = env.createPlayer(instance);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        survivorTeam.addPlayer(target);
        SpectatorInventory inventory = new SpectatorInventory(survivorTeam, spectatorTeam());

        inventory.handleClick(spectator, 9, new Click.Right(9), headOf(target), _ -> {});

        assertTrue(teleportedTo.isEmpty());
        assertStatsRequested(spectator, target);
    }

    @Test
    void testLeftClickOnSpectatorTeleports(@NotNull Env env) {
        Player viewer = env.createPlayer(instance);
        Player other = env.createPlayer(instance);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        Team spectatorTeam = spectatorTeam();
        spectatorTeam.addPlayer(viewer);
        spectatorTeam.addPlayer(other);
        SpectatorInventory inventory = new SpectatorInventory(survivorTeam, spectatorTeam);

        inventory.handleClick(viewer, 36, new Click.Left(36), headOf(other), _ -> {});

        assertEquals(List.of(other), teleportedTo);
    }

    @Test
    void testLeftClickOnOwnHeadDoesNotTeleport(@NotNull Env env) {
        Player viewer = env.createPlayer(instance);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        Team spectatorTeam = spectatorTeam();
        spectatorTeam.addPlayer(viewer);
        SpectatorInventory inventory = new SpectatorInventory(survivorTeam, spectatorTeam);

        inventory.handleClick(viewer, 36, new Click.Left(36), headOf(viewer), _ -> {});

        assertTrue(teleportedTo.isEmpty());
    }

    @Test
    void testLeftClickOnPlayerOutsideBothTeamsDoesNotTeleport(@NotNull Env env) {
        Player viewer = env.createPlayer(instance);
        Player stranger = env.createPlayer(instance);
        SpectatorInventory inventory = new SpectatorInventory(Team.of(GameConfig.SURVIVOR_KEY, 5), spectatorTeam());

        inventory.handleClick(viewer, 36, new Click.Left(36), headOf(stranger), _ -> {});

        assertTrue(teleportedTo.isEmpty());
    }

    @Test
    void testRightClickOnSpectatorOpensStats(@NotNull Env env) {
        Player viewer = env.createPlayer(instance);
        Player other = env.createPlayer(instance);
        Team spectatorTeam = spectatorTeam();
        spectatorTeam.addPlayer(other);
        SpectatorInventory inventory = new SpectatorInventory(Team.of(GameConfig.SURVIVOR_KEY, 5), spectatorTeam);

        inventory.handleClick(viewer, 36, new Click.Right(36), headOf(other), _ -> {});

        assertTrue(teleportedTo.isEmpty());
        assertStatsRequested(viewer, other);
    }

    @Test
    void testSpectatorHeadShowsTeleportHint(@NotNull Env env) {
        Player viewer = env.createPlayer(instance);
        Player dead = env.createPlayer(instance);
        Team spectatorTeam = spectatorTeam();
        spectatorTeam.addPlayer(dead);

        SpectatorInventory inventory = new SpectatorInventory(Team.of(GameConfig.SURVIVOR_KEY, 5), spectatorTeam);
        inventory.open(viewer);
        env.tick();

        List<Component> lore = inventory.getInventory().getItemStack(SpectatorInventory.SPECTATOR_SLOTS[0])
                .get(DataComponents.LORE);
        assertTrue(lore.contains(SpectatorInventory.TELEPORT_HINT));
    }

    @Test
    void testSpectatorsAreListedBelowTheSurvivors(@NotNull Env env) {
        Player viewer = env.createPlayer(instance);
        Player survivor = env.createPlayer(instance);
        Player dead = env.createPlayer(instance);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        survivorTeam.addPlayer(survivor);
        Team spectatorTeam = spectatorTeam();
        spectatorTeam.addPlayer(dead);

        SpectatorInventory inventory = new SpectatorInventory(survivorTeam, spectatorTeam);
        inventory.open(viewer);
        env.tick();

        assertEquals(survivor.getUuid(), inventory.getInventory().getItemStack(SpectatorInventory.SURVIVOR_SLOTS[0])
                .getTag(SpectatorInventory.TARGET_TAG));
        assertEquals(dead.getUuid(), inventory.getInventory().getItemStack(SpectatorInventory.SPECTATOR_SLOTS[0])
                .getTag(SpectatorInventory.TARGET_TAG));
    }

    @Test
    void testSurvivorHeadShowsHearts(@NotNull Env env) {
        Player viewer = env.createPlayer(instance);
        Player survivor = env.createPlayer(instance);
        survivor.setHealth(7.0F);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        survivorTeam.addPlayer(survivor);

        SpectatorInventory inventory = new SpectatorInventory(survivorTeam, spectatorTeam());
        inventory.open(viewer);
        env.tick();

        List<Component> lore = inventory.getInventory().getItemStack(SpectatorInventory.SURVIVOR_SLOTS[0])
                .get(DataComponents.LORE);
        assertTrue(lore.contains(HealthDisplay.hearts(survivor)));
    }

    @Test
    void testRefreshHealthRebuildsHeadsOnlyAfterAChange(@NotNull Env env) {
        Player viewer = env.createPlayer(instance);
        Player survivor = env.createPlayer(instance);
        Team survivorTeam = Team.of(GameConfig.SURVIVOR_KEY, 5);
        survivorTeam.addPlayer(survivor);
        SpectatorInventory inventory = new SpectatorInventory(survivorTeam, spectatorTeam());
        inventory.open(viewer);
        env.tick();
        ItemStack before = inventory.getInventory().getItemStack(SpectatorInventory.SURVIVOR_SLOTS[0]);

        assertTrue(inventory.refreshHealth());
        assertEquals(before, inventory.getInventory().getItemStack(SpectatorInventory.SURVIVOR_SLOTS[0]));

        survivor.setHealth(4.0F);
        inventory.refreshHealth();
        env.tick();

        assertTrue(inventory.getInventory().getItemStack(SpectatorInventory.SURVIVOR_SLOTS[0])
                .get(DataComponents.LORE).contains(HealthDisplay.hearts(survivor)));
    }

    @Test
    void testRefreshHealthReportsClosedInventory() {
        SpectatorInventory inventory = new SpectatorInventory(Team.of(GameConfig.SURVIVOR_KEY, 5), spectatorTeam());

        assertFalse(inventory.refreshHealth());
    }

    private void assertStatsRequested(Player viewer, Player target) {
        assertEquals(1, statsRequests.size());
        assertEquals(viewer, statsRequests.getFirst().getPlayer());
        assertEquals(target, statsRequests.getFirst().getTarget());
    }

    private static Team spectatorTeam() {
        return Team.of(GameConfig.SPECTATOR_KEY, 5);
    }

    private static ItemStack headOf(Player player) {
        return ItemStack.builder(Material.PLAYER_HEAD)
                .set(SpectatorInventory.TARGET_TAG, player.getUuid())
                .build();
    }

    private static long countHeads(SpectatorInventory inventory) {
        return java.util.Arrays.stream(inventory.getInventory().getItemStacks())
                .filter(stack -> stack.material() == Material.PLAYER_HEAD)
                .count();
    }
}
