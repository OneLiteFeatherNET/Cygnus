package net.onelitefeather.cygnus.command;

import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.tunnelvision.TunnelVisionRenderer;
import net.onelitefeather.cygnus.tunnelvision.TunnelVisionStage;
import net.onelitefeather.cygnus.utils.PlayerState;
import net.onelitefeather.cygnus.utils.RepeatingTask;

import java.time.temporal.ChronoUnit;

/**
 * Puts the tunnel vision on screen without a running round, so the glyph sizes in the resource
 * pack can be judged from the lobby.
 * <p>
 * {@code /tunnelvision stage <0-32>} freezes a single stage, so the drawing of a single overlay
 * texture can be judged on its own. {@code /tunnelvision intensity <0.0-1.0>} runs the same
 * heartbeat the game uses, to judge how the pulse feels. Both are ended by
 * {@code /tunnelvision off}. The upper bound of {@code stage} follows
 * {@link TunnelVisionStage#MAX_STAGE}.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.2.0
 * @since 2.7.0
 */
public final class TunnelVisionCommand extends Command {


    private final TunnelVisionRenderer renderer;
    private final PlayerState<RepeatingTask> previews;

    /**
     * Creates the command.
     *
     * @param renderer the renderer that draws the preview
     */
    public TunnelVisionCommand(TunnelVisionRenderer renderer) {
        super("tunnelvision");
        this.renderer = renderer;
        this.previews = new PlayerState<>();

        var stage = ArgumentType.Integer("level").between(0, TunnelVisionStage.MAX_STAGE);
        var intensity = ArgumentType.Double("amount").between(0.0D, 1.0D);

        this.setDefaultExecutor((sender, context) -> sender.sendMessage(
                Messages.getTunnelVisionUsageMessage(TunnelVisionStage.MAX_STAGE)
        ));

        this.addSyntax((sender, context) -> {
            Player player = CommandSenders.asPlayer(sender, Messages.ONLY_PLAYERS_CAN_PREVIEW);
            if (player == null) return;
            this.stopPreview(player);
            this.renderer.render(player, context.get(stage));
        }, ArgumentType.Literal("stage"), stage);

        this.addSyntax((sender, context) -> {
            Player player = CommandSenders.asPlayer(sender, Messages.ONLY_PLAYERS_CAN_PREVIEW);
            if (player == null) return;
            this.startPreview(player, context.get(intensity));
        }, ArgumentType.Literal("intensity"), intensity);

        this.addSyntax((sender, context) -> {
            Player player = CommandSenders.asPlayer(sender, Messages.ONLY_PLAYERS_CAN_PREVIEW);
            if (player == null) return;
            this.stopPreview(player);
            this.renderer.clear(player);
        }, ArgumentType.Literal("off"));
    }

    /**
     * Draws a constant intensity with its heartbeat running until the preview is stopped.
     *
     * @param player    the player to draw for
     * @param intensity the intensity to hold
     */
    private void startPreview(Player player, double intensity) {
        this.stopPreview(player);

        TunnelVisionStage stage = new TunnelVisionStage();
        // The task alone would only draw from its first repetition onward, so the initial stage is
        // rendered here, the same way BloodSplatterService and SlenderGazeService draw their first
        // frame before ever starting their own repeating task.
        this.renderer.render(player, stage.update(intensity));

        RepeatingTask task = new RepeatingTask(() -> {
            if (!player.isOnline()) {
                this.stopPreview(player);
                return;
            }
            this.renderer.render(player, stage.update(intensity));
        });
        this.previews.put(player, task);
        task.start(TunnelVisionStage.TICK_MILLIS, ChronoUnit.MILLIS);
    }

    /**
     * Ends a running preview, leaving whatever is on screen untouched.
     *
     * @param player the player whose preview to end
     */
    private void stopPreview(Player player) {
        RepeatingTask task = this.previews.remove(player);
        if (task != null) task.stop();
    }
}
