package net.onelitefeather.cygnus.stamina;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the slender's server-side viewer registration stays in sync with the
 * visual state across all transitions of the {@link SlenderBar}.
 * <p>
 * The viewable rule installed by {@code TeamHelper.assignSlender} reads
 * {@link VisibilityRules#isHidden(Player)} of the <em>slender</em> and ignores the
 * candidate viewer, so a transition only takes effect once someone re-evaluates the
 * rule via {@link VisibilityRules#refresh(Player)}. These tests check that
 * every exit out of {@code DRAINING} does exactly that.
 *
 * @author TheMeinerLP
 * @version 2.0.0
 * @since 2.4.0
 */
class SlenderVisibilityIntegrationTest extends CygnusPlayerTestBase {

    /** Ticks to run the bar dry: MAX_TIME 16 / TIME_STEP 0.5. */
    private static final int TICKS_TO_TIMEOUT = 34;

    /**
     * Ticks to regenerate from empty back to MIN_TIME_TO_REACTIVATE: 10 / TIME_STEP 0.5, plus one
     * for the overshoot DRAINING leaves behind. Below that {@code SlenderBar.changeStatus()}
     * refuses a new attack.
     */
    private static final int TICKS_TO_REACTIVATE = 21;

    /**
     * Builds a survivor team holding the given players, mirroring the team the production
     * code hands to {@link VisibilityRules#refresh(Player)}.
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
     * Replays {@code TeamHelper.assignSlender} followed by
     * {@code GameStartListener.handleSlenderStart} / {@code handleSurvivorStart}.
     * <p>
     * The closing {@code updateViewableRule()} is <em>not</em> part of the production
     * path — production sets the tag without re-evaluating. It is added here so the
     * remaining tests start from the intended state; the missing call itself is covered
     * by {@link #testRoundStartHidesSlender(Env)}.
     *
     * @param slender      the player acting as the slender
     * @param survivorTeam the survivor team
     */
    private void startRound(@NotNull Player slender, @NotNull Team survivorTeam) {
        // The single production path from a stamina state change to a visibility update.
        MinecraftServer.getGlobalEventHandler()
                .addListener(StaminaStateChangeEvent.class, new StaminaStateChangeListener());
        assignSlender(slender);
        survivorTeam.getPlayers().forEach(survivor -> {
            survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
            survivor.setTag(Tags.HIDDEN, SlenderBarHelper.VISIBLE);
        });
        slender.setTag(Tags.HIDDEN, SlenderBarHelper.HIDDEN);
        slender.updateViewableRule();
    }

    /**
     * Replays {@code TeamHelper.assignSlender} verbatim, including the order in which the
     * rule is installed relative to the {@link Tags#HIDDEN} tag.
     *
     * @param slender the player to become the slender
     */
    private void assignSlender(@NotNull Player slender) {
        slender.setTag(Tags.TEAM_KEY, GameConfig.SLENDER_KEY);
        slender.setTag(Tags.HIDDEN, SlenderBarHelper.HIDDEN);
        slender.updateViewableRule(VisibilityRules.slenderRule(slender));
    }

    /**
     * Replays an eye press: {@code SlenderBarTrigger.trigger}, i.e. changeStatus plus
     * changeVisibilityStatus plus the real {@link VisibilityRules#refresh}.
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
     * Sanity check: without this the remaining assertions could pass vacuously.
     * The eye press must make the slender a registered viewer of the survivor.
     */
    @Test
    @DisplayName("Vorbedingung: Augendruck macht den Slender sichtbar")
    void testEyePressMakesSlenderVisible(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection slenderConnection = env.createConnection();
        TestConnection survivorConnection = env.createConnection();

        CygnusPlayer slender = (CygnusPlayer) slenderConnection.connect(instance, new Pos(0, 40, 0));
        Player survivor = survivorConnection.connect(instance, new Pos(5, 40, 5));
        Team survivorTeam = createSurvivorTeam(survivor);
        startRound(slender, survivorTeam);

        assertFalse(slender.isViewer(survivor), "Vor dem Augendruck darf niemand den Slender sehen");

        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(slender);
        slenderBar.start();
        pressEye(slenderBar, slender);
        env.tick();

        assertTrue(slender.isViewer(survivor),
                "Nach dem Augendruck muss der Survivor registrierter Viewer sein");

        slenderBar.stop();
        env.destroyInstance(instance, true);
    }

    /**
     * The round start installs the rule before the HIDDEN tag exists, so the rule
     * evaluates to visible and registers every nearby player. Setting the tag afterwards
     * without re-evaluating leaves that registration in place.
     */
    @Test
    @DisplayName("Rundenstart laesst den Slender nicht registriert zurueck")
    void testRoundStartHidesSlender(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection slenderConnection = env.createConnection();
        TestConnection survivorConnection = env.createConnection();

        CygnusPlayer slender = (CygnusPlayer) slenderConnection.connect(instance, new Pos(0, 40, 0));
        Player survivor = survivorConnection.connect(instance, new Pos(5, 40, 5));

        // Exact production order: TeamHelper.assignSlender, then GameStartListener.
        assignSlender(slender);
        survivor.setTag(Tags.TEAM_KEY, GameConfig.SURVIVOR_KEY);
        slender.setTag(Tags.HIDDEN, SlenderBarHelper.HIDDEN);
        survivor.setTag(Tags.HIDDEN, SlenderBarHelper.VISIBLE);
        env.tick();

        assertFalse(slender.isViewer(survivor),
                "Zu Rundenbeginn darf der Slender fuer keinen Survivor registriert sein - "
                        + "die Regel wird installiert, bevor der HIDDEN-Tag existiert, und "
                        + "danach nicht erneut ausgewertet");

        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Auto-Timeout traegt den Viewer serverseitig aus")
    void testAutoTimeoutUnregistersViewer(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection slenderConnection = env.createConnection();
        TestConnection survivorConnection = env.createConnection();

        CygnusPlayer slender = (CygnusPlayer) slenderConnection.connect(instance, new Pos(0, 40, 0));
        Player survivor = survivorConnection.connect(instance, new Pos(5, 40, 5));
        Team survivorTeam = createSurvivorTeam(survivor);
        startRound(slender, survivorTeam);

        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(slender);
        slenderBar.start();

        pressEye(slenderBar, slender);
        env.tick();
        assertTrue(slender.isViewer(survivor), "Vorbedingung: Slender muss sichtbar sein");

        // Bar auslaufen lassen - der Pfad, der den Trigger umgeht.
        for (int i = 0; i < TICKS_TO_TIMEOUT; i++) {
            slenderBar.consume();
        }
        env.tick();

        assertFalse(slender.isViewer(survivor),
                "Nach dem Auto-Timeout darf der Survivor kein registrierter Viewer mehr sein - "
                        + "sonst wird der Slender beim Wiedereintritt in die View-Distance neu gespawnt");

        slenderBar.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Manueller Doppeldruck bleibt korrekt")
    void testManualToggleStaysCorrect(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection slenderConnection = env.createConnection();
        TestConnection survivorConnection = env.createConnection();

        CygnusPlayer slender = (CygnusPlayer) slenderConnection.connect(instance, new Pos(0, 40, 0));
        Player survivor = survivorConnection.connect(instance, new Pos(5, 40, 5));
        Team survivorTeam = createSurvivorTeam(survivor);
        startRound(slender, survivorTeam);

        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(slender);
        slenderBar.start();

        pressEye(slenderBar, slender);   // READY -> DRAINING
        env.tick();
        pressEye(slenderBar, slender);   // DRAINING -> REGENERATING
        env.tick();

        assertFalse(slender.isViewer(survivor),
                "Nach dem manuellen Zurueckschalten darf kein Viewer registriert bleiben");

        slenderBar.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("Polaritaet bleibt ueber mehrere Zyklen stabil")
    void testPolarityStableAcrossCycles(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection slenderConnection = env.createConnection();
        TestConnection survivorConnection = env.createConnection();

        CygnusPlayer slender = (CygnusPlayer) slenderConnection.connect(instance, new Pos(0, 40, 0));
        Player survivor = survivorConnection.connect(instance, new Pos(5, 40, 5));
        Team survivorTeam = createSurvivorTeam(survivor);
        startRound(slender, survivorTeam);

        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(slender);
        slenderBar.start();

        // Zyklus 1: Angriff, dann auslaufen lassen.
        pressEye(slenderBar, slender);
        for (int i = 0; i < TICKS_TO_TIMEOUT; i++) {
            slenderBar.consume();
        }
        env.tick();

        // Die Bar muss erst wieder ueber MIN_TIME_TO_REACTIVATE steigen, sonst
        // verweigert changeStatus() den zweiten Angriff.
        for (int i = 0; i < TICKS_TO_REACTIVATE; i++) {
            slenderBar.consume();
        }

        // Zyklus 2: erneuter Angriff - der Slender MUSS jetzt sichtbar sein.
        pressEye(slenderBar, slender);
        env.tick();

        assertTrue(slender.isViewer(survivor),
                "Im Angriffsmodus muss der Slender sichtbar sein - "
                        + "ist er es nicht, ist die Sichtbarkeits-Polaritaet verdreht");

        slenderBar.stop();
        env.destroyInstance(instance, true);
    }

    @Test
    @DisplayName("View-Distance-Zyklus spawnt den Slender nicht neu")
    void testViewDistanceCycleDoesNotRespawn(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        TestConnection slenderConnection = env.createConnection();
        TestConnection survivorConnection = env.createConnection();

        CygnusPlayer slender = (CygnusPlayer) slenderConnection.connect(instance, new Pos(0, 40, 0));
        Player survivor = survivorConnection.connect(instance, new Pos(5, 40, 5));
        Team survivorTeam = createSurvivorTeam(survivor);
        startRound(slender, survivorTeam);

        SlenderBar slenderBar = (SlenderBar) StaminaFactory.createSlenderStamina(slender);
        slenderBar.start();

        pressEye(slenderBar, slender);
        for (int i = 0; i < TICKS_TO_TIMEOUT; i++) {
            slenderBar.consume();
        }
        env.tick();

        // Ab hier auf Spawn-Pakete horchen - VOR dem Teleport starten.
        var spawnCollector = survivorConnection.trackIncoming(
                net.minestom.server.network.packet.server.play.SpawnEntityPacket.class);

        // Raus: 6 Chunks weit (> ENTITY_VIEW_DISTANCE = 5).
        survivor.teleport(new Pos(6 * 16, 40, 0)).join();
        env.tick();

        // Und wieder rein.
        survivor.teleport(new Pos(5, 40, 5)).join();
        env.tick();

        assertEquals(0, spawnCollector.collect().size(),
                "Beim Wiedereintritt in die View-Distance darf der unsichtbare Slender "
                        + "nicht neu gespawnt werden");

        slenderBar.stop();
        env.destroyInstance(instance, true);
    }
}
