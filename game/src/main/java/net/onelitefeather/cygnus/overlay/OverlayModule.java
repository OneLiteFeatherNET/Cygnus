package net.onelitefeather.cygnus.overlay;

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
import net.onelitefeather.cygnus.stamina.StaminaService;
import net.onelitefeather.cygnus.team.TeamHelper;
import net.onelitefeather.cygnus.tunnelvision.OverlayTunnelVisionRenderer;
import net.onelitefeather.cygnus.tunnelvision.TunnelVisionService;
import net.onelitefeather.cygnus.utils.StaminaHelper;
import net.theevilreaper.xerus.api.team.TeamService;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Wires the full-screen effects: the slender gaze, the blood splatter and the tunnel vision.
 * <p>
 * The effects are drawn as {@code camera_overlay} textures and are gated by
 * {@link OverlayProperties} alone - deliberately not by whether this server hands out a resource
 * pack. One gate for all of them, so that {@code cygnus.overlays} means what its name says and
 * an effect cannot end up outside it by being wired in somewhere else.
 * </p>
 */
public final class OverlayModule implements GameFeature {

    private final BossBarGazeSignal gazeSignal;
    private final SlenderGazeService slenderGazeService;
    private final BloodSplatterService bloodSplatterService;
    private final TunnelVisionService tunnelVisionService;

    /**
     * Builds the effects.
     *
     * @param glitch         the thresholds of the slender gaze
     * @param teamService    the teams of the round
     * @param staminaService the stamina the tunnel vision follows
     */
    public OverlayModule(GameConfig.Glitch glitch, TeamService teamService, StaminaService staminaService) {
        ScreenOverlay screenOverlay = new EquipmentScreenOverlay();
        this.gazeSignal = new BossBarGazeSignal();
        this.slenderGazeService = new SlenderGazeService(
                this.gazeSignal,
                new SlenderGaze(
                        glitch.range(),
                        glitch.closeRange(),
                        glitch.viewAngle()),
                () -> TeamHelper.slenderOf(teamService),
                () -> TeamHelper.survivorsOf(teamService));
        this.bloodSplatterService = new BloodSplatterService(
                screenOverlay,
                bound -> ThreadLocalRandom.current().nextInt(bound)
        );
        this.tunnelVisionService = new TunnelVisionService(
                new OverlayTunnelVisionRenderer(screenOverlay),
                player -> StaminaHelper.remainingShare(staminaService, player),
                () -> TeamHelper.survivorsOf(teamService));
    }

    @Override
    public void registerListener(EventNode<Event> node) {
        if (!OverlayProperties.enabled()) return;
        this.slenderGazeService.registerListener(node);
        this.bloodSplatterService.registerListener(node);
        this.tunnelVisionService.registerListener(node);
    }

    @Override
    public void registerCommands(CommandManager manager) {
        // Outside the overlay gate: the command drives the signal directly, see GlitchCommand.
        manager.register(new GlitchCommand(this.gazeSignal));
    }
}
