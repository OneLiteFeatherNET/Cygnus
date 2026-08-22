package net.onelitefeather.cygnus.listener;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.message.ChatPosition;
import net.minestom.server.message.Messenger;
import net.minestom.server.network.packet.server.play.SystemChatPacket;
import net.minestom.testing.Collector;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.theevilreaper.xerus.api.team.Team;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the chat part of the role visibility matrix: a survivor and the slender talk to everyone,
 * a spectator talks to spectators only.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class PlayerChatListenerTest extends CygnusPlayerTestBase {

    /**
     * The historical implementation only isolated spectator chat while a {@code GamePhase} was
     * running. This is the case that used to work.
     */
    @Test
    void testSpectatorMessageDuringTheGamePhaseOnlyReachesSpectators(@NotNull Env env) {
        assertSpectatorChatStaysIsolated(env);
    }

    /**
     * After the game phase finished, the {@code RestartPhase} runs for another 15 seconds. It is no
     * {@code GamePhase}, so the old phase bound check stopped filtering and spectator chat leaked
     * back to survivors and the slender.
     */
    @Test
    void testSpectatorMessageDuringTheRestartPhaseOnlyReachesSpectators(@NotNull Env env) {
        assertSpectatorChatStaysIsolated(env);
    }

    /**
     * The phase series reports {@code null} whenever no phase is active. {@code null instanceof
     * GamePhase} is {@code false}, so the old check was fail-open and broadcast spectator chat to
     * everyone. The filter must fall back to "nobody outside the spectator team", never to "all".
     */
    @Test
    void testSpectatorMessageWithoutAnyActivePhaseOnlyReachesSpectators(@NotNull Env env) {
        assertSpectatorChatStaysIsolated(env);
    }

    @Test
    void testSurvivorMessageReachesEveryone(@NotNull Env env) {
        assertChatReachesEveryone(env, GameConfig.SURVIVOR_KEY);
    }

    @Test
    void testSlenderMessageReachesEveryone(@NotNull Env env) {
        assertChatReachesEveryone(env, GameConfig.SLENDER_KEY);
    }

    /**
     * Lets a spectator write a message and asserts that it only shows up on the spectator clients.
     *
     * @param env the test environment
     */
    private void assertSpectatorChatStaysIsolated(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        TestConnection spectatorConnection = env.createConnection();
        Player spectator = spectatorConnection.connect(instance);
        TestConnection secondSpectatorConnection = env.createConnection();
        Player secondSpectator = secondSpectatorConnection.connect(instance);
        TestConnection survivorConnection = env.createConnection();
        Player survivor = survivorConnection.connect(instance);
        TestConnection slenderConnection = env.createConnection();
        Player slender = slenderConnection.connect(instance);

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        joinSpectatorTeam(spectatorTeam, spectator);
        joinSpectatorTeam(spectatorTeam, secondSpectator);
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        useUsernameAsDisplayName(spectator, secondSpectator, survivor, slender);

        Collector<SystemChatPacket> spectatorMessages = spectatorConnection.trackIncoming(SystemChatPacket.class);
        Collector<SystemChatPacket> secondSpectatorMessages = secondSpectatorConnection.trackIncoming(SystemChatPacket.class);
        Collector<SystemChatPacket> survivorMessages = survivorConnection.trackIncoming(SystemChatPacket.class);
        Collector<SystemChatPacket> slenderMessages = slenderConnection.trackIncoming(SystemChatPacket.class);

        PlayerChatEvent event = new PlayerChatEvent(spectator, List.of(spectator, secondSpectator, survivor, slender), "hi");
        new PlayerChatListener(spectatorTeam).accept(event);
        deliverToRecipients(event);

        assertTrue(event.getRecipients().isEmpty(), "The default broadcast recipients must be cleared for a spectator sender.");
        assertEquals(1, spectatorMessages.collect().size(), "The sending spectator must see the own message.");
        assertEquals(1, secondSpectatorMessages.collect().size(), "Another spectator must receive the spectator message.");
        assertTrue(survivorMessages.collect().isEmpty(), "A survivor must never receive a spectator message.");
        assertTrue(slenderMessages.collect().isEmpty(), "The slender must never receive a spectator message.");

        env.destroyInstance(instance, true);
    }

    /**
     * Lets a non spectator write a message and asserts that every online player receives it.
     *
     * @param env     the test environment
     * @param teamKey the team key of the sending player
     */
    private void assertChatReachesEveryone(@NotNull Env env, @NotNull Key teamKey) {
        Instance instance = env.createFlatInstance();

        TestConnection senderConnection = env.createConnection();
        Player sender = senderConnection.connect(instance);
        TestConnection survivorConnection = env.createConnection();
        Player survivor = survivorConnection.connect(instance);
        TestConnection spectatorConnection = env.createConnection();
        Player spectator = spectatorConnection.connect(instance);

        Team spectatorTeam = Team.of(GameConfig.SPECTATOR_KEY, 5);
        joinSpectatorTeam(spectatorTeam, spectator);
        sender.setTag(Tags.TEAM_KEY, teamKey);
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        useUsernameAsDisplayName(sender, survivor, spectator);

        Collector<SystemChatPacket> senderMessages = senderConnection.trackIncoming(SystemChatPacket.class);
        Collector<SystemChatPacket> survivorMessages = survivorConnection.trackIncoming(SystemChatPacket.class);
        Collector<SystemChatPacket> spectatorMessages = spectatorConnection.trackIncoming(SystemChatPacket.class);

        PlayerChatEvent event = new PlayerChatEvent(sender, List.of(sender, survivor, spectator), "hi");
        new PlayerChatListener(spectatorTeam).accept(event);
        deliverToRecipients(event);

        assertEquals(3, event.getRecipients().size(), "A non spectator sender must keep the default recipient list.");
        assertEquals(1, senderMessages.collect().size(), "The sender must see the own message.");
        assertEquals(1, survivorMessages.collect().size(), "A survivor must receive the message.");
        assertEquals(1, spectatorMessages.collect().size(), "A spectator is allowed to read survivor and slender chat.");

        env.destroyInstance(instance, true);
    }

    /**
     * Mirrors the delivery which Minestom performs after the {@link PlayerChatEvent} was handled.
     *
     * @param event the handled chat event
     */
    private void deliverToRecipients(@NotNull PlayerChatEvent event) {
        if (event.getRecipients().isEmpty()) return;
        Messenger.sendMessage(event.getRecipients(), event.getFormattedMessage(), ChatPosition.CHAT);
    }

    /**
     * Adds the given player to the spectator team and marks it with the matching team tag.
     *
     * @param spectatorTeam the spectator team
     * @param player        the player to convert
     */
    private void joinSpectatorTeam(@NotNull Team spectatorTeam, @NotNull Player player) {
        spectatorTeam.addPlayer(player);
        player.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);
    }

    /**
     * The chat layout builds on the display name, which is null for a freshly connected test player.
     *
     * @param players the players to name
     */
    private void useUsernameAsDisplayName(@NotNull Player @NotNull ... players) {
        for (Player player : players) {
            player.setDisplayName(Component.text(player.getUsername()));
        }
    }
}
