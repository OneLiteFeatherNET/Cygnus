package net.onelitefeather.cygnus.tutorial;

import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.dialog.Dialog;
import net.minestom.server.dialog.DialogAction;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.common.ClearDialogPacket;
import net.minestom.server.network.packet.server.common.ShowDialogPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.camera.WakeUpTransition;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.disclaimer.EpilepsyDisclaimer;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.phase.WaitingPhase;
import net.onelitefeather.cygnus.telemetry.TickSections;
import net.onelitefeather.cygnus.utils.Items;
import net.onelitefeather.cygnus.view.GameViewImpl;
import net.theevilreaper.xerus.api.phase.Phase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the tutorial item and the page turns on the feature's own event node. The events are
 * dispatched to the node directly, the way {@code GameFeatures} would hang it into the game.
 */
@ExtendWith(MicrotusExtension.class)
class TutorialFeatureTest {

    private Env env;
    private Instance instance;
    private TestConnection connection;
    private Player player;
    private Phase phase;
    private TutorialFeature feature;

    @BeforeEach
    void setUp(Env env) {
        this.env = env;
        this.instance = env.createFlatInstance();
        this.connection = env.createConnection();
        this.player = this.connection.connect(this.instance, new Pos(0, 40, 0));
        this.phase = new LobbyPhase(new GameConfig.Round(2, 10, 30, 600));
        this.feature = new TutorialFeature(() -> this.phase, env.process().connection());
    }

    private void spawn() {
        this.feature.node().call(new PlayerSpawnEvent(this.player, this.instance, true));
    }

    private void click(Player clicker, Key key) {
        this.feature.node().call(new PlayerCustomClickEvent(clicker, key, null));
    }

    private void startRound() {
        this.phase = new GamePhase(new GameViewImpl(), 600, new JumpScareManager(TickSections.NONE), (_, _) -> 0.0D);
    }

    private static List<Key> buttonKeys(ShowDialogPacket packet) {
        Dialog.MultiAction dialog = assertInstanceOf(Dialog.MultiAction.class, packet.dialog());
        return dialog.actions().stream()
                .map(button -> assertInstanceOf(DialogAction.Custom.class, button.action()).key())
                .toList();
    }

    @Test
    @DisplayName("The first spawn in the lobby hands out the tutorial item")
    void lobbySpawnGivesTheItem() {
        spawn();

        assertTrue(Items.isTutorialItem(this.player.getInventory().getItemStack(Items.TUTORIAL_SLOT)),
                "the item belongs in its slot");
    }

    @Test
    @DisplayName("The waiting phase still hands out the item")
    void waitingPhaseGivesTheItem() {
        this.phase = new WaitingPhase(new GameViewImpl(), () -> {
        }, () -> {
        }, () -> this.instance, new WakeUpTransition());

        spawn();

        assertTrue(Items.hasTutorialItem(this.player));
    }

    @Test
    @DisplayName("During the round nobody gets the item")
    void gamePhaseGivesNoItem() {
        startRound();

        spawn();

        assertFalse(Items.hasTutorialItem(this.player), "a late joiner must not carry the lobby item into the round");
    }

    @Test
    @DisplayName("A spawn that is not the first one hands out nothing")
    void laterSpawnGivesNoItem() {
        this.feature.node().call(new PlayerSpawnEvent(this.player, this.instance, false));

        assertFalse(Items.hasTutorialItem(this.player));
    }

    @Test
    @DisplayName("Right click with the item opens the introduction")
    void useOpensTheIntro() {
        spawn();
        Collector<ShowDialogPacket> dialogs = this.connection.trackIncoming(ShowDialogPacket.class);

        this.feature.node().call(new PlayerUseItemEvent(this.player, PlayerHand.MAIN,
                this.player.getInventory().getItemStack(Items.TUTORIAL_SLOT), 0L));

        dialogs.assertSingle(packet -> assertEquals(
                List.of(TutorialPages.SURVIVOR_RULES_KEY, TutorialPages.SLENDER_RULES_KEY, TutorialPages.CLOSE_KEY),
                buttonKeys(packet)));
    }

    @Test
    @DisplayName("Using any other item opens nothing")
    void otherItemOpensNothing() {
        Items.setStatsItem(this.player);
        Collector<ShowDialogPacket> dialogs = this.connection.trackIncoming(ShowDialogPacket.class);

        this.feature.node().call(new PlayerUseItemEvent(this.player, PlayerHand.MAIN,
                this.player.getInventory().getItemStack(Items.STATS_SLOT), 0L));
        this.feature.node().call(new PlayerUseItemEvent(this.player, PlayerHand.MAIN, ItemStack.of(Material.COMPASS), 0L));

        assertEquals(List.of(), dialogs.collect());
    }

    @Test
    @DisplayName("Each button opens the page it names")
    void eachKeyOpensItsPage() {
        assertOpens(TutorialPages.SURVIVOR_RULES_KEY, List.of(TutorialPages.SURVIVOR_ITEMS_KEY, TutorialPages.INTRO_KEY));
        assertOpens(TutorialPages.SURVIVOR_ITEMS_KEY, List.of(TutorialPages.SURVIVOR_RULES_KEY, TutorialPages.INTRO_KEY));
        assertOpens(TutorialPages.SLENDER_RULES_KEY, List.of(TutorialPages.SLENDER_ITEMS_KEY, TutorialPages.INTRO_KEY));
        assertOpens(TutorialPages.SLENDER_ITEMS_KEY, List.of(TutorialPages.SLENDER_RULES_KEY, TutorialPages.INTRO_KEY));
        assertOpens(TutorialPages.INTRO_KEY,
                List.of(TutorialPages.SURVIVOR_RULES_KEY, TutorialPages.SLENDER_RULES_KEY, TutorialPages.CLOSE_KEY));
    }

    private void assertOpens(Key key, List<Key> expectedButtons) {
        Collector<ShowDialogPacket> dialogs = this.connection.trackIncoming(ShowDialogPacket.class);

        click(this.player, key);

        dialogs.assertSingle(packet -> assertEquals(expectedButtons, buttonKeys(packet), "wrong page for " + key));
    }

    @Test
    @DisplayName("Close closes the tutorial instead of opening a page")
    void closeClosesTheDialog() {
        Collector<ShowDialogPacket> dialogs = this.connection.trackIncoming(ShowDialogPacket.class);
        Collector<ClearDialogPacket> clears = this.connection.trackIncoming(ClearDialogPacket.class);

        click(this.player, TutorialPages.CLOSE_KEY);

        clears.assertSingle();
        assertEquals(List.of(), dialogs.collect());
    }

    @Test
    @DisplayName("A button of another dialog is left alone")
    void foreignKeyOpensNothing() {
        Collector<ShowDialogPacket> dialogs = this.connection.trackIncoming(ShowDialogPacket.class);
        Collector<ClearDialogPacket> clears = this.connection.trackIncoming(ClearDialogPacket.class);

        click(this.player, EpilepsyDisclaimer.ACKNOWLEDGE_KEY);

        assertEquals(List.of(), dialogs.collect(), "the disclaimer's button must not open a tutorial page");
        assertEquals(List.of(), clears.collect(), "the disclaimer's button must not close anything here");
    }

    @Test
    @DisplayName("A click turns the page only for the player who clicked")
    void clickOpensThePageOnlyForTheClicker() {
        TestConnection otherConnection = this.env.createConnection();
        otherConnection.connect(this.instance, new Pos(0, 40, 0));
        Collector<ShowDialogPacket> otherDialogs = otherConnection.trackIncoming(ShowDialogPacket.class);
        Collector<ShowDialogPacket> dialogs = this.connection.trackIncoming(ShowDialogPacket.class);

        click(this.player, TutorialPages.SURVIVOR_RULES_KEY);

        dialogs.assertSingle();
        assertEquals(List.of(), otherDialogs.collect(), "another reader must stay on their own page");
    }

    @Test
    @DisplayName("The round start takes the item away and closes the tutorial")
    void roundStartTakesTheItemAndClosesTheDialog() {
        spawn();
        this.player.getInventory().setItemStack(Items.HEAL_SLOT, ItemStack.of(Material.SUGAR));
        Collector<ClearDialogPacket> clears = this.connection.trackIncoming(ClearDialogPacket.class);

        this.feature.node().call(new GameStartEvent());

        assertFalse(Items.hasTutorialItem(this.player), "the item must be gone when the round starts");
        assertEquals(Material.SUGAR, this.player.getInventory().getItemStack(Items.HEAL_SLOT).material(), "the painkillers stay");
        clears.assertSingle();
    }

    @Test
    @DisplayName("A late click after the round started opens nothing")
    void clickAfterRoundStartOpensNothing() {
        startRound();
        Collector<ShowDialogPacket> dialogs = this.connection.trackIncoming(ShowDialogPacket.class);

        click(this.player, TutorialPages.SURVIVOR_RULES_KEY);

        assertEquals(List.of(), dialogs.collect(), "a closed tutorial must not come back during the round");
    }
}
