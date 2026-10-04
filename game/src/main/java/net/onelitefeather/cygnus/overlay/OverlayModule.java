package net.onelitefeather.cygnus.overlay;

import net.onelitefeather.cygnus.telemetry.TickSections;
import net.minestom.server.command.CommandManager;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.onelitefeather.cygnus.GameFeature;
import net.onelitefeather.cygnus.blood.BloodSplatterService;
import net.onelitefeather.cygnus.command.GlitchCommand;
import net.onelitefeather.cygnus.common.config.GameConfig;
import net.onelitefeather.cygnus.gaze.BossBarGazeSignal;
import net.onelitefeather.cygnus.gaze.SlenderGaze;
import net.onelitefeather.cygnus.gaze.SlenderGazeService;
import net.onelitefeather.cygnus.glitch.PageGlitchService;
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.visibility.VisibilityRules;
import net.onelitefeather.cygnus.tunnelvision.OverlayTunnelVisionRenderer;
import net.onelitefeather.cygnus.tunnelvision.TunnelVisionService;
import net.onelitefeather.cygnus.utils.StaminaHelper;
import net.theevilreaper.xerus.api.team.TeamService;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Wires the full-screen effects: the slender gaze, the page glitch, the blood splatter and the tunnel vision.
 * <p>
 * The effects are drawn as {@code camera_overlay} textures and are gated by
 * {@link OverlayProperties} alone - deliberately not by whether this server hands out a resource
 * pack. One gate for all of them, so that {@code cygnus.overlays} means what its name says and
 * an effect cannot end up outside it by being wired in somewhere else.
 * </p>
 */
public final class OverlayModule implements GameFeature {

    private final EventNode<Event> node = EventNode.all("overlay");

    private final BossBarGazeSignal gazeSignal;
    private final SlenderGazeService slenderGazeService;
    private final PageGlitchService pageGlitchService;
    private final BloodSplatterService bloodSplatterService;
    private final TunnelVisionService tunnelVisionService;

    /**
     * Builds the effects.
     *
     * @param glitch         the thresholds of the slender gaze
     * @param pageGlitch     how the slender's own screen tears as pages are found
     * @param teamService    the teams of the round
     * @param staminaService the stamina the tunnel vision follows
     */
    public OverlayModule(GameConfig.Glitch glitch, GameConfig.PageGlitch pageGlitch, TeamService teamService, StaminaService staminaService) {
        this(glitch, pageGlitch, teamService, staminaService, TickSections.NONE);
    }

    /**
     * Builds the effects, with the ones that update on a timer measured for the slow tick report.
     *
     * @param glitch         the thresholds of the slender gaze
     * @param pageGlitch     how the slender's own screen tears as pages are found
     * @param teamService    the teams of the round
     * @param staminaService the stamina the tunnel vision follows
     * @param sections       measures how long the gaze and the tunnel vision updates take
     * @since 2.15.0
     */
    public OverlayModule(GameConfig.Glitch glitch, GameConfig.PageGlitch pageGlitch, TeamService teamService,
                         StaminaService staminaService, TickSections sections) {
        ScreenOverlay screenOverlay = new EquipmentScreenOverlay();
        this.gazeSignal = new BossBarGazeSignal();
        this.slenderGazeService = new SlenderGazeService(
                this.gazeSignal,
                new SlenderGaze(
                        glitch.range(),
                        glitch.closeRange(),
                        glitch.viewAngle()),
                () -> TeamHelper.slenderOf(teamService),
                () -> TeamHelper.survivorsOf(teamService),
                sections);
        // The gaze's signal, shared rather than a second one: a player has one carrier, and two
        // would draw two full-screen quads over each other. Which of the two services addresses a
        // player follows their team, and a hand-over moves them from one to the other.
        this.pageGlitchService = new PageGlitchService(
                pageGlitch,
                this.gazeSignal,
                () -> TeamHelper.slenderOf(teamService),
                VisibilityRules::isHidden);
        this.bloodSplatterService = new BloodSplatterService(
                screenOverlay,
                bound -> ThreadLocalRandom.current().nextInt(bound)
        );
        this.tunnelVisionService = new TunnelVisionService(
                new OverlayTunnelVisionRenderer(screenOverlay),
                player -> StaminaHelper.remainingShare(staminaService, player),
                () -> TeamHelper.survivorsOf(teamService),
                sections);
        this.registerListeners();
    }

    @Override
    public boolean enabled() {
        return OverlayProperties.enabled();
    }

    @Override
    public EventNode<Event> node() {
        return this.node;
    }

    /**
     * Hangs the node of every effect below the module's own, so they share its gate.
     */
    private void registerListeners() {
        this.node.addChild(this.slenderGazeService.node());
        this.node.addChild(this.pageGlitchService.node());
        this.node.addChild(this.bloodSplatterService.node());
        this.node.addChild(this.tunnelVisionService.node());
    }

    @Override
    public void registerCommands(CommandManager manager) {
        // Outside the overlay gate: the command drives the signal directly, see GlitchCommand.
        manager.register(new GlitchCommand(this.gazeSignal));
    }
}
