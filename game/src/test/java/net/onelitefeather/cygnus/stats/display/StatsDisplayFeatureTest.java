package net.onelitefeather.cygnus.stats.display;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.inventory.InventoryPreClickEvent;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerSwapItemEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.camera.WakeUpTransition;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.event.GameFinishEvent;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.phase.WaitingPhase;
import net.onelitefeather.cygnus.stats.FakeStatsRepository;
import net.onelitefeather.cygnus.telemetry.TickSections;
import net.onelitefeather.cygnus.utils.Items;
import net.onelitefeather.cygnus.view.GameViewImpl;
import net.theevilreaper.xerus.api.phase.Phase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static net.onelitefeather.cygnus.stats.display.StatsDisplayTestSupport.year;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the lobby item and the read-only menu on the feature's own event node. The events are
 * dispatched to the node directly, the way {@code GameFeatures} would hang it into the game.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
@ExtendWith(MicrotusExtension.class)
class StatsDisplayFeatureTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneId.of("UTC"));

    private final FakeStatsRepository repository = new FakeStatsRepository();
    private Env env;
    private Instance instance;
    private Player player;
    private PlayerStatsService service;
    private Phase phase;
    private StatsDisplayFeature feature;

    @BeforeEach
    void setUp(Env env) {
        this.env = env;
        this.instance = env.createFlatInstance();
        this.player = env.createConnection().connect(this.instance, new Pos(0, 40, 0));
        this.service = new PlayerStatsService(this.repository, CLOCK, ZoneId.of("UTC"), env.process().connection(), Runnable::run);
        this.phase = new LobbyPhase(new GameConfig.Round(2, 10, 30, 600));
        this.feature = feature(() -> this.phase);
    }

    private StatsDisplayFeature feature(Supplier<Phase> phases) {
        return new StatsDisplayFeature(this.service, phases, this.env.process().connection());
    }

    private void spawn() {
        this.feature.node().call(new PlayerSpawnEvent(this.player, this.instance, true));
    }

    private ItemStack statsItem() {
        return this.player.getInventory().getItemStack(Items.STATS_SLOT);
    }

    private StatsMenu openMenu() {
        this.repository.store(year(this.player.getUuid(), 2026, 4, 2, 0, 0));
        this.service.openOwnMenu(this.player);
        return assertInstanceOf(StatsMenu.class, this.player.getOpenInventory(), "the menu must be open");
    }

    @Test
    @DisplayName("The first spawn in the lobby hands out the statistics item")
    void lobbySpawnGivesTheItem() {
        spawn();

        assertTrue(Items.isStatsItem(statsItem()), "the item belongs in its slot");
        assertEquals(Material.BOOK, statsItem().material());
    }

    @Test
    @DisplayName("The waiting phase still hands out the item")
    void waitingPhaseGivesTheItem() {
        this.phase = new WaitingPhase(new GameViewImpl(), () -> {
        }, () -> {
        }, () -> this.instance, new WakeUpTransition());

        spawn();

        assertTrue(Items.isStatsItem(statsItem()));
    }

    @Test
    @DisplayName("During the round nobody gets the item")
    void gamePhaseGivesNoItem() {
        this.phase = new GamePhase(new GameViewImpl(), 600, new JumpScareManager(TickSections.NONE), _ -> 0.0D);

        spawn();

        assertFalse(Items.hasStatsItem(this.player), "a late joiner must not carry the lobby item into the round");
    }

    @Test
    @DisplayName("A spawn that is not the first one hands out nothing")
    void laterSpawnGivesNoItem() {
        this.feature.node().call(new PlayerSpawnEvent(this.player, this.instance, false));

        assertFalse(Items.hasStatsItem(this.player));
    }

    @Test
    @DisplayName("Disabled statistics give no item and no listener")
    void disabledGivesNoItem() {
        this.repository.enabled = false;

        spawn();

        assertFalse(this.feature.enabled(), "a disabled feature is not hung into the game");
        assertFalse(Items.hasStatsItem(this.player), "not even if its node is called");
    }

    @Test
    @DisplayName("Joining loads the player's current year for the first click")
    void spawnPreloadsTheYear() {
        spawn();

        assertTrue(this.service.cachedView(this.player.getUuid()).isPresent(), "the view must be cached before the first click");
    }

    @Test
    @DisplayName("The game start takes the item away and leaves the other items alone")
    void gameStartRemovesTheItem() {
        spawn();
        this.player.getInventory().setItemStack(Items.HEAL_SLOT, ItemStack.of(Material.SUGAR));

        this.feature.node().call(new GameStartEvent());

        assertFalse(Items.hasStatsItem(this.player), "the item must be gone when the round starts");
        assertEquals(Material.SUGAR, this.player.getInventory().getItemStack(Items.HEAL_SLOT).material(), "the painkillers stay");
    }

    @Test
    @DisplayName("Right click with the item opens the player's own menu")
    void useOpensTheOwnMenu() {
        this.repository.store(year(this.player.getUuid(), 2026, 4, 2, 0, 0));
        spawn();

        this.feature.node().call(new PlayerUseItemEvent(this.player, PlayerHand.MAIN, statsItem(), 0L));

        StatsMenu menu = assertInstanceOf(StatsMenu.class, this.player.getOpenInventory(), "the menu must open");
        assertTrue(StatsDisplayTestSupport.plain(menu.getTitle()).contains(this.player.getUsername()));
    }

    @Test
    @DisplayName("Using any other item opens nothing")
    void otherItemsOpenNothing() {
        spawn();

        this.feature.node().call(new PlayerUseItemEvent(this.player, PlayerHand.MAIN, ItemStack.of(Material.COMPASS), 0L));

        assertNull(this.player.getOpenInventory());
    }

    @Test
    @DisplayName("The item cannot be dropped")
    void itemCannotBeDropped() {
        spawn();
        ItemDropEvent event = new ItemDropEvent(this.player, statsItem());

        this.feature.node().call(event);

        assertTrue(event.isCancelled(), "dropping the item must be cancelled");
    }

    @Test
    @DisplayName("The item cannot be swapped into the off hand")
    void itemCannotBeSwapped() {
        spawn();
        PlayerSwapItemEvent event = new PlayerSwapItemEvent(this.player, statsItem(), ItemStack.AIR);

        this.feature.node().call(event);

        assertTrue(event.isCancelled(), "swapping the item must be cancelled");
    }

    @Test
    @DisplayName("The item cannot be moved in the inventory")
    void itemCannotBeMoved() {
        spawn();
        InventoryPreClickEvent event = new InventoryPreClickEvent(this.player.getInventory(), this.player,
                new Click.Left(Items.STATS_SLOT));

        this.feature.node().call(event);

        assertTrue(event.isCancelled(), "moving the item must be cancelled");
    }

    @Test
    @DisplayName("A click in the menu changes nothing, whatever kind of click it is")
    void menuIsReadOnly() {
        StatsMenu menu = openMenu();
        ItemStack before = menu.getItemStack(StatsMenu.GENERAL_SLOT);
        List<Click> clicks = List.of(new Click.Left(StatsMenu.GENERAL_SLOT), new Click.Right(StatsMenu.GENERAL_SLOT),
                new Click.Middle(StatsMenu.GENERAL_SLOT), new Click.LeftShift(StatsMenu.GENERAL_SLOT),
                new Click.RightShift(StatsMenu.GENERAL_SLOT), new Click.Double(StatsMenu.GENERAL_SLOT),
                new Click.LeftDrag(List.of(0, 1, 2)), new Click.RightDrag(List.of(0, 1, 2)),
                new Click.LeftDropCursor(), new Click.DropSlot(StatsMenu.GENERAL_SLOT, true),
                new Click.HotbarSwap(0, StatsMenu.GENERAL_SLOT), new Click.OffhandSwap(StatsMenu.GENERAL_SLOT));

        for (Click click : clicks) {
            InventoryPreClickEvent event = new InventoryPreClickEvent(menu, this.player, click);
            this.feature.node().call(event);
            assertTrue(event.isCancelled(), click + " in the menu must be cancelled");
        }
        assertEquals(before, menu.getItemStack(StatsMenu.GENERAL_SLOT), "the item is untouched");
    }

    @Test
    @DisplayName("A shift click in the player's own inventory cannot push items into the open menu")
    void menuTakesNoItemsFromTheInventory() {
        openMenu();
        InventoryPreClickEvent event = new InventoryPreClickEvent(this.player.getInventory(), this.player, new Click.LeftShift(9));

        this.feature.node().call(event);

        assertTrue(event.isCancelled(), "the player's inventory is locked while the menu is open");
    }

    @Test
    @DisplayName("Leaving drops the cached view")
    void quitForgetsThePlayer() {
        spawn();

        this.feature.node().call(new PlayerDisconnectEvent(this.player));

        assertEquals(Optional.empty(), this.service.cachedView(this.player.getUuid()));
    }

    @Test
    @DisplayName("The end of a round drops the cached views")
    void finishInvalidatesTheCache() {
        spawn();

        this.feature.node().call(new GameFinishEvent(GameFinishEvent.Reason.TIME_OVER));

        assertEquals(Optional.empty(), this.service.cachedView(this.player.getUuid()));
    }

    @Test
    @DisplayName("The stats command is registered, also while statistics are off, so it can explain that")
    void commandIsAlwaysRegistered() {
        this.repository.enabled = false;

        this.feature.registerCommands(this.env.process().command());

        assertTrue(this.env.process().command().commandExists("stats"), "the command must exist to say it is unavailable");
    }
}
