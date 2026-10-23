package net.onelitefeather.cygnus.lobby;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.PlayerHand;
import net.minestom.server.event.item.ItemDropEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.event.LobbyReturnRequestEvent;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.phase.GamePhase;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.jumpscare.JumpScareManager;
import net.onelitefeather.cygnus.telemetry.TickSections;
import net.onelitefeather.cygnus.utils.Items;
import net.onelitefeather.cygnus.view.GameViewImpl;
import net.theevilreaper.xerus.api.phase.Phase;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the lobby hints, the server item and the item that leads back to the network lobby on the
 * feature's own event node.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.19.0
 */
@ExtendWith(MicrotusExtension.class)
class LobbyHintsFeatureTest {

    private static final String SERVER = "Slender-2";

    private final List<LobbyReturnRequestEvent> requests = new ArrayList<>();
    private Env env;
    private Instance instance;
    private Player player;
    private TestConnection connection;
    private Phase phase;

    @BeforeEach
    void setUp(Env env) {
        this.env = env;
        this.instance = env.createFlatInstance();
        this.connection = env.createConnection();
        this.player = this.connection.connect(this.instance, new Pos(0, 40, 0));
        this.phase = new LobbyPhase(new GameConfig.Round(2, 10, 30, 600));
    }

    private LobbyHintsFeature feature(@Nullable String serverName, Consumer<LobbyReturnRequestEvent> handler) {
        return new LobbyHintsFeature(serverName, () -> this.phase, this.env.process().connection(), event -> {
            this.requests.add(event);
            handler.accept(event);
        });
    }

    private LobbyHintsFeature feature(@Nullable String serverName) {
        return feature(serverName, _ -> {
        });
    }

    private void spawn(LobbyHintsFeature feature) {
        feature.node().call(new PlayerSpawnEvent(this.player, this.instance, true));
    }

    // A collector is single use: it stops tracking once collected, so every check starts its own.
    private Collector<SystemChatPacket> chat() {
        return this.connection.trackIncoming(SystemChatPacket.class);
    }

    private List<String> messages(Collector<SystemChatPacket> chat) {
        this.env.tick();
        return chat.collect().stream()
                .map(packet -> PlainTextComponentSerializer.plainText().serialize(packet.message()))
                .toList();
    }

    private ItemStack serverItem() {
        return this.player.getInventory().getItemStack(Items.SERVER_SLOT);
    }

    private ItemStack lobbyItem() {
        return this.player.getInventory().getItemStack(Items.LOBBY_SLOT);
    }

    private void use(LobbyHintsFeature feature, ItemStack stack) {
        feature.node().call(new PlayerUseItemEvent(this.player, PlayerHand.MAIN, stack, 0L));
    }

    @Test
    @DisplayName("The first spawn in the lobby hands out the server item and the lobby item")
    void lobbySpawnGivesTheItems() {
        spawn(feature(SERVER));

        assertTrue(Items.isServerItem(serverItem()), "the server item belongs in the first slot");
        assertEquals(Material.COMPASS, serverItem().material());
        assertTrue(Items.isLobbyItem(lobbyItem()), "the lobby item belongs in the last slot");
        assertEquals(Material.MAGMA_CREAM, lobbyItem().material());
    }

    @Test
    @DisplayName("The server item shows the server name")
    void serverItemShowsTheName() {
        spawn(feature(SERVER));

        String name = PlainTextComponentSerializer.plainText().serialize(serverItem().get(net.minestom.server.component.DataComponents.CUSTOM_NAME));
        assertTrue(name.contains(SERVER), "the display name must carry the server name but was " + name);
    }

    @Test
    @DisplayName("The first spawn tells the player to turn the game audio on")
    void spawnSendsTheAudioHint() {
        Collector<SystemChatPacket> chat = chat();

        spawn(feature(SERVER));

        List<String> lines = messages(chat);
        assertTrue(lines.stream().anyMatch(line -> line.contains("game audio on")), "the audio hint must be sent but got " + lines);
    }

    @Test
    @DisplayName("The audio hint is sent only once")
    void audioHintIsSentOnce() {
        LobbyHintsFeature feature = feature(SERVER);
        Collector<SystemChatPacket> chat = chat();

        spawn(feature);
        feature.node().call(new PlayerSpawnEvent(this.player, this.instance, false));

        assertEquals(1, messages(chat).stream().filter(line -> line.contains("game audio on")).count());
    }

    @Test
    @DisplayName("The first spawn tells the player which server they are on")
    void spawnSendsTheServerLine() {
        Collector<SystemChatPacket> chat = chat();

        spawn(feature(SERVER));

        assertTrue(messages(chat).stream().anyMatch(line -> line.contains("You are playing on " + SERVER)));
    }

    @Test
    @DisplayName("Without a server name there is no line and no server item")
    void noServerNameSkipsLineAndItem() {
        Collector<SystemChatPacket> chat = chat();

        spawn(feature(null));

        assertTrue(messages(chat).stream().noneMatch(line -> line.contains("You are playing on")), "no line without a name");
        assertFalse(Items.isServerItem(serverItem()), "no server item without a name");
        assertTrue(Items.isLobbyItem(lobbyItem()), "the way back does not depend on the name");
    }

    @Test
    @DisplayName("During the round nobody gets a hint or an item")
    void gamePhaseGivesNothing() {
        this.phase = new GamePhase(new GameViewImpl(), 600, new JumpScareManager(TickSections.NONE), _ -> 0.0D);

        Collector<SystemChatPacket> chat = chat();

        spawn(feature(SERVER));

        assertTrue(messages(chat).isEmpty(), "a late joiner must not get lobby hints");
        assertFalse(Items.isLobbyItem(lobbyItem()));
        assertFalse(Items.isServerItem(serverItem()));
    }

    @Test
    @DisplayName("The game start takes both items away and leaves the others alone")
    void gameStartRemovesTheItems() {
        LobbyHintsFeature feature = feature(SERVER);
        spawn(feature);
        this.player.getInventory().setItemStack(4, ItemStack.of(Material.SUGAR));

        feature.node().call(new GameStartEvent());

        assertFalse(Items.isServerItem(serverItem()), "the server item must be gone");
        assertFalse(Items.isLobbyItem(lobbyItem()), "the lobby item must be gone");
        assertEquals(Material.SUGAR, this.player.getInventory().getItemStack(4).material());
    }

    @Test
    @DisplayName("Using the server item repeats the server line")
    void useServerItemRepeatsTheLine() {
        LobbyHintsFeature feature = feature(SERVER);
        spawn(feature);
        Collector<SystemChatPacket> chat = chat();

        use(feature, serverItem());

        assertTrue(messages(chat).stream().anyMatch(line -> line.contains("You are playing on " + SERVER)));
    }

    @Test
    @DisplayName("Using the lobby item fires a lobby return request for the player")
    void useLobbyItemFiresTheEvent() {
        LobbyHintsFeature feature = feature(SERVER, LobbyReturnRequestEvent::markHandled);
        spawn(feature);

        use(feature, lobbyItem());

        assertEquals(1, this.requests.size(), "one request per use");
        assertEquals(this.player, this.requests.getFirst().player());
        assertTrue(this.player.isOnline(), "a handled request does not kick the player");
    }

    @Test
    @DisplayName("An unhandled request falls back to kicking the player")
    void unhandledRequestKicks() {
        LobbyHintsFeature feature = feature(SERVER);
        spawn(feature);

        use(feature, lobbyItem());

        assertFalse(this.player.isOnline(), "nobody took the request, the proxy must get the player back");
    }

    @Test
    @DisplayName("Using any other item fires nothing")
    void otherItemsFireNothing() {
        LobbyHintsFeature feature = feature(SERVER);
        spawn(feature);

        use(feature, ItemStack.of(Material.STONE));

        assertTrue(this.requests.isEmpty());
        assertTrue(this.player.isOnline());
    }

    @Test
    @DisplayName("The items cannot be dropped")
    void itemsCannotBeDropped() {
        LobbyHintsFeature feature = feature(SERVER);
        spawn(feature);
        ItemDropEvent server = new ItemDropEvent(this.player, serverItem());
        ItemDropEvent lobby = new ItemDropEvent(this.player, lobbyItem());

        feature.node().call(server);
        feature.node().call(lobby);

        assertTrue(server.isCancelled(), "dropping the server item must be cancelled");
        assertTrue(lobby.isCancelled(), "dropping the lobby item must be cancelled");
    }
}
