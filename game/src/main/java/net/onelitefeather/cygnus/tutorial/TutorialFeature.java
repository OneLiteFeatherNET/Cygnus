package net.onelitefeather.cygnus.tutorial;

import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.PlayerCustomClickEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.event.player.PlayerUseItemEvent;
import net.minestom.server.network.ConnectionManager;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.event.GameStartEvent;
import net.onelitefeather.cygnus.phase.LobbyPhase;
import net.onelitefeather.cygnus.phase.WaitingPhase;
import net.onelitefeather.cygnus.utils.Items;
import net.onelitefeather.pica.dialog.DialogTemplate;
import net.theevilreaper.xerus.api.phase.Phase;

import java.util.function.Supplier;

/**
 * Lets every player in the lobby open a tutorial on both roles and their items.
 *
 * <p>The tutorial hangs on an item in the lobby hotbar, handed out on the first spawn while the
 * lobby or the waiting phase runs, so it can be opened as often as a player likes and is never
 * pushed on anyone. A right click opens the introduction, and each button there and on the
 * following pages names the page it leads to, see {@link TutorialPages}.</p>
 *
 * <p>When the round starts the item is taken away and an open tutorial is closed, so nobody walks
 * into the round with a dialog in front of their eyes. A click that still arrives afterwards opens
 * nothing. Dropping or moving the item is already ruled out by the game's global cancels.</p>
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

    /**
     * Creates the feature, builds the pages and puts its listeners on its node.
     *
     * @param phase       reads the running phase
     * @param connections finds the online players when the round starts
     */
    public TutorialFeature(Supplier<Phase> phase, ConnectionManager connections) {
        this.phase = phase;
        this.connections = connections;
        this.pages = new TutorialPages();
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
        this.node.addListener(GameStartEvent.class, _ -> this.connections.getOnlinePlayers().forEach(player -> {
            Items.removeTutorialItem(player);
            player.closeDialog();
        }));
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
     * the player who clicked. Clicks of other dialogs, and every click once the round runs, are left
     * alone.
     *
     * @param event the click the client sent back
     */
    void handleClick(PlayerCustomClickEvent event) {
        if (!inLobby()) {
            return;
        }
        if (TutorialPages.CLOSE_KEY.equals(event.getKey())) {
            event.getPlayer().closeDialog();
            return;
        }
        open(event.getPlayer(), event.getKey());
    }

    private void open(Player player, Key key) {
        DialogTemplate page = this.pages.page(key);
        if (page != null) {
            page.open(player);
        }
    }
}
