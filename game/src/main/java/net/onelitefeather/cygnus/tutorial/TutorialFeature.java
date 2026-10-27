package net.onelitefeather.cygnus.tutorial;

import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.event.player.PlayerDisconnectEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.network.ConnectionManager;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.map.event.GamePrepareEvent;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.phase.WaitingPhase;
import net.onelitefeather.cygnus.utils.Items;
import net.onelitefeather.pica.dialog.DialogTemplate;
import net.theevilreaper.xerus.api.phase.Phase;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Lets every player in the lobby open a tutorial on both roles and their items.
 *
 * <p>The tutorial hangs on an item in the lobby hotbar, handed out on the first spawn while the
 * lobby or the waiting phase runs, so it can be opened as often as a player likes and is never
 * pushed on anyone. A right click opens the introduction, and each button there and on the
 * following pages names the page it leads to, see {@link TutorialPages}.</p>
 *
 * <p>Right before the players are teleported onto the map, the item is taken away and an open
 * tutorial is closed, so nobody arrives in the round with a dialog in front of their eyes. Only the
 * players who opened the tutorial and did not close it with its own button are sent the close: a
 * player who never opened it may still have the epilepsy warning in front of them, and that one has
 * to be answered, not swept away. Escape closes the tutorial on the client alone, so such a reader is
 * still sent the close; by then no other dialog can be open, since the warning has to be answered
 * before the tutorial can be opened at all. Buttons
 * only answer a player who still holds the item, so a click that arrives after that opens nothing.
 * Dropping or moving the item is already ruled out by the game's global cancels.</p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.21.0
 */
public final class TutorialFeature implements GameFeature {

    private final EventNode<Event> node = EventNode.all("tutorial");
    private final Supplier<Phase> phase;
    private final ConnectionManager connections;
    private final TutorialPages pages;
    private final Set<UUID> readers;

    /**
     * Creates the feature, builds the pages and puts its listeners on its node.
     *
     * @param phase       reads the running phase
     * @param connections finds the online players right before the teleport
     */
    public TutorialFeature(Supplier<Phase> phase, ConnectionManager connections) {
        this.phase = phase;
        this.connections = connections;
        this.pages = new TutorialPages();
        this.readers = ConcurrentHashMap.newKeySet();
        registerListeners();
    }

    @Override
    public EventNode<Event> node() {
        return this.node;
    }

    private void registerListeners() {
        this.node.addListener(PlayerSpawnEvent.class, this::onSpawn);
        this.node.addListener(PlayerUseItemEvent.class, event -> {
            if (Items.isTutorialItem(event.getItemStack())) {
                open(event.getPlayer(), TutorialPages.INTRO_KEY);
            }
        });
        this.node.addListener(PlayerCustomClickEvent.class, this::handleClick);
        this.node.addListener(GamePrepareEvent.class, _ -> this.connections.getOnlinePlayers().forEach(player -> {
            Items.removeTutorialItem(player);
            if (this.readers.remove(player.getUuid())) {
                player.closeDialog();
            }
        }));
        this.node.addListener(PlayerDisconnectEvent.class, event -> this.readers.remove(event.getPlayer().getUuid()));
    }

    private boolean inLobby() {
        Phase running = this.phase.get();
        return running instanceof LobbyPhase || running instanceof WaitingPhase;
    }

    private void onSpawn(PlayerSpawnEvent event) {
        if (!event.isFirstSpawn() || !inLobby()) {
            return;
        }
        Items.setTutorialItem(event.getPlayer());
    }

    /**
     * Answers a tutorial button: close shuts the dialog, every other tutorial key opens its page for
     * the player who clicked. Clicks of other dialogs, and every click of a player who no longer holds
     * the item, are left alone.
     *
     * @param event the click the client sent back
     */
    void handleClick(PlayerCustomClickEvent event) {
        if (!Items.hasTutorialItem(event.getPlayer())) {
            return;
        }
        if (TutorialPages.CLOSE_KEY.equals(event.getKey())) {
            this.readers.remove(event.getPlayer().getUuid());
            event.getPlayer().closeDialog();
            return;
        }
        open(event.getPlayer(), event.getKey());
    }

    private void open(Player player, Key key) {
        DialogTemplate page = this.pages.page(key);
        if (page != null) {
            this.readers.add(player.getUuid());
            page.open(player);
        }
    }
}
