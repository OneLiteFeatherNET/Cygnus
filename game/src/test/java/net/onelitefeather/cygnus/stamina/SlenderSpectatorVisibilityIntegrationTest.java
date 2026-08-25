package net.onelitefeather.cygnus.stamina;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.TestConnection;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.common.Tags;
import net.onelitefeather.cygnus.event.StaminaStateChangeEvent;
import net.onelitefeather.cygnus.listener.stamina.StaminaStateChangeListener;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import net.onelitefeather.cygnus.visibility.VisibilityRules;
import net.theevilreaper.xerus.api.team.Team;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies how the spectator system introduced in 2.7.0 interacts with the slender
 * visibility rule.
 * <p>
 * A dead survivor leaves the survivor team, so
 * {@link VisibilityRules#refresh(Player)} no longer reaches it through the team
 * iteration. It is still covered by the online-player pass and by the slender's own
 * re-evaluation, so a spectator must keep following the slender's visibility in both
 * directions — it must see the slender during an attack and lose sight of it afterwards.
 * <p>
 * Independently, {@code SpectatorService.join} installs {@code _ -> false} on the
 * spectator itself, which must keep it invisible to everybody else.
 *
 * @author TheMeinerLP
 * @version 2.0.0
 * @since 2.7.0
 */
class SlenderSpectatorVisibilityIntegrationTest extends CygnusPlayerTestBase {

    /**
     * Builds a survivor team holding the given players.
     *
     * @param survivors the players to put into the team
     * @return the populated survivor team
     */
    private Team createSurvivorTeam(Player @NotNull ... survivors) {
        Team team = Team.of(GameConfig.SURVIVOR_KEY, 10);
        for (Player survivor : survivors) {
            team.addPlayer(survivor);
        }
        return team;
    }

    /**
     * Replays {@code TeamHelper.assignSlender} plus the round start tagging, ending in the
     * intended state: rule installed, slender hidden.
     *
     * @param slender      the player acting as the slender
     * @param survivorTeam the survivor team
     */
    private void startRound(@NotNull Player slender, @NotNull Team survivorTeam) {
        // The single production path from a stamina state change to a visibility update.
        MinecraftServer.getGlobalEventHandler()
                .addListener(StaminaStateChangeEvent.class, new StaminaStateChangeListener());
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        slender.setTag(Tags.HIDDEN, SlenderBarHelper.HIDDEN);
        slender.updateViewableRule(VisibilityRules.slenderRule(slender));
        survivorTeam.getPlayers().forEach(survivor -> {
            survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
            survivor.setTag(Tags.HIDDEN, SlenderBarHelper.VISIBLE);
        });
        slender.setTag(Tags.HIDDEN, SlenderBarHelper.HIDDEN);
        slender.updateViewableRule();
    }

    /**
     * Replays an eye press: {@code SlenderBarTrigger.trigger} plus the real
     * {@link VisibilityRules#refresh(Player)}.
     *
     * @param bar          the slender bar to toggle
     * @param slender      the player acting as the slender
     */
    private void pressEye(@NotNull SlenderBar bar, @NotNull Player slender) {
        // SlenderBar flips Tags.HIDDEN itself and fires the StaminaStateChangeEvent;
        // StaminaStateChangeListener re-evaluates the viewable rule from there.
        bar.changeStatus();
    }

    /**
     * Replays {@code PlayerDeathListener} followed by {@code SpectatorService.join}: the
     * player leaves the survivor team, loses its team key, and gains the spectator key
     * plus its own view rule.
     *
     * @param player       the dying player
     * @param survivorTeam the team the player leaves
     */
    private void die(@NotNull Player player, @NotNull Team survivorTeam) {
        // PlayerDeathListener
        survivorTeam.removePlayer(player);
        player.removeTag(Tags.TEAM_KEY);
        // SpectatorService.join
        player.setGameMode(GameMode.SPECTATOR);
        player.setTag(Tags.TEAM_KEY, GameConfig.SPECTATOR_KEY);
        player.updateViewableRule(_ -> false);
    }

    /**
     * Dying while the slender is invisible must not lock the spectator out of ever seeing
     * the slender again — otherwise spectating is pointless.
     */
    @Test
    @DisplayName("Spectator sieht den Slender im Angriffsmodus")
    void testSpectatorSeesSlenderDuringAttack(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection slenderConnection = env.createConnection();
        TestConnection deadConnection = env.createConnection();
        TestConnection aliveConnection = env.createConnection();

        CygnusPlayer slender = (CygnusPlayer) slenderConnection.connect(instance, new Pos(0, 40, 0));
        Player dying = deadConnection.connect(instance, new Pos(5, 40, 5));
        Player alive = aliveConnection.connect(instance, new Pos(-5, 40, -5));
        Team survivorTeam = createSurvivorTeam(dying, alive);
        startRound(slender, survivorTeam);

        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(slender);
        slenderBar.start();

        // Der Survivor stirbt, bevor der Slender je sichtbar war.
        die(dying, survivorTeam);
        env.tick();

        // Jetzt der Angriffsmodus - der Spectator muss den Slender sehen.
        pressEye(slenderBar, slender);
        env.tick();

        assertTrue(slender.isViewer(alive),
                "Kontrolle: der lebende Survivor sieht den Slender im Angriffsmodus");
        assertTrue(slender.isViewer(dying),
                "Ein Spectator muss den Slender im Angriffsmodus sehen - "
                        + "sonst schaut er einem unsichtbaren Spiel zu");

        slenderBar.stop();
        env.destroyInstance(instance, true);
    }

    /**
     * Mirror case: a spectator that saw the slender during an attack must lose sight of it
     * again when the slender goes back to hidden, otherwise the slender's position is
     * leaked to a dead player.
     */
    @Test
    @DisplayName("Manueller Doppeldruck traegt auch den Spectator aus")
    void testManualToggleAlsoUnregistersSpectator(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection slenderConnection = env.createConnection();
        TestConnection deadConnection = env.createConnection();
        TestConnection aliveConnection = env.createConnection();

        CygnusPlayer slender = (CygnusPlayer) slenderConnection.connect(instance, new Pos(0, 40, 0));
        Player dying = deadConnection.connect(instance, new Pos(5, 40, 5));
        Player alive = aliveConnection.connect(instance, new Pos(-5, 40, -5));
        Team survivorTeam = createSurvivorTeam(dying, alive);
        startRound(slender, survivorTeam);

        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(slender);
        slenderBar.start();

        // Augendruck: Slender wird sichtbar.
        pressEye(slenderBar, slender);
        env.tick();
        assertTrue(slender.isViewer(dying), "Vorbedingung: Slender muss sichtbar sein");

        // Der Survivor stirbt, waehrend der Slender sichtbar ist.
        die(dying, survivorTeam);
        env.tick();

        // Manueller Doppeldruck zurueck in den Regenerationsmodus.
        pressEye(slenderBar, slender);
        env.tick();

        assertFalse(slender.isViewer(alive),
                "Kontrolle: fuer den lebenden Survivor raeumt der manuelle Pfad korrekt auf");
        assertFalse(slender.isViewer(dying),
                "Auch der Spectator darf nach dem Zurueckschalten kein Viewer mehr sein - "
                        + "sonst verraet der unsichtbare Slender einem Toten seine Position");

        slenderBar.stop();
        env.destroyInstance(instance, true);
    }

    /**
     * {@code SpectatorService.join} installs {@code _ -> false} on the spectator, so nobody
     * may keep it as a registered viewer.
     */
    @Test
    @DisplayName("Spectator ist fuer andere unsichtbar")
    void testSpectatorIsHiddenFromOthers(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection slenderConnection = env.createConnection();
        TestConnection deadConnection = env.createConnection();
        TestConnection aliveConnection = env.createConnection();

        CygnusPlayer slender = (CygnusPlayer) slenderConnection.connect(instance, new Pos(0, 40, 0));
        Player dying = deadConnection.connect(instance, new Pos(5, 40, 5));
        Player alive = aliveConnection.connect(instance, new Pos(-5, 40, -5));
        Team survivorTeam = createSurvivorTeam(dying, alive);
        startRound(slender, survivorTeam);
        env.tick();

        assertTrue(dying.isViewer(alive), "Vorbedingung: lebende Survivor sehen sich gegenseitig");

        die(dying, survivorTeam);
        env.tick();

        assertFalse(dying.isViewer(alive),
                "Nach dem Wechsel in den Spectator-Modus darf ihn kein Survivor mehr sehen");
        assertFalse(dying.isViewer(slender),
                "Auch der Slender darf den Spectator nicht mehr sehen");

        env.destroyInstance(instance, true);
    }
}
