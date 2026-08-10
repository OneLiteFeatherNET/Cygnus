package net.onelitefeather.cygnus.command;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import net.onelitefeather.cygnus.common.Messages;
import net.onelitefeather.cygnus.tunnelvision.TunnelVisionRenderer;
import net.onelitefeather.cygnus.tunnelvision.TunnelVisionStage;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Puts the tunnel vision on screen without a running round, so the glyph sizes in the resource
 * pack can be judged from the lobby.
 * <p>
 * {@code /tunnelvision stage <0-16>} freezes a single stage, which is what the font's
 * {@code height} and {@code ascent} are calibrated against. {@code /tunnelvision intensity
 * <0.0-1.0>} runs the same heartbeat the game uses, to judge how the pulse feels. Both are ended
 * by {@code /tunnelvision off}.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public final class TunnelVisionCommand extends Command {

    private final TunnelVisionRenderer renderer;
    private final Map<UUID, Task> previews;

    /**
     * Creates the command.
     *
     * @param renderer the renderer that draws the preview
     */
    public TunnelVisionCommand(TunnelVisionRenderer renderer) {
        super("tunnelvision");
        this.renderer = renderer;
        this.previews = new ConcurrentHashMap<>();

        var stage = ArgumentType.Integer("level").between(0, TunnelVisionStage.MAX_STAGE);
        var intensity = ArgumentType.Double("amount").between(0.0D, 1.0D);

        this.setDefaultExecutor((sender, context) -> sender.sendMessage(
                Messages.withMiniPrefix("<gray>Usage: <yellow>/tunnelvision stage <0-16> | intensity <0.0-1.0> | off")
        ));

        this.addSyntax((sender, context) -> {
            Player player = asPlayer(sender);
            if (player == null) return;
            this.stopPreview(player);
            this.renderer.render(player, context.get(stage));
        }, ArgumentType.Literal("stage"), stage);

        this.addSyntax((sender, context) -> {
            Player player = asPlayer(sender);
            if (player == null) return;
            this.startPreview(player, context.get(intensity));
        }, ArgumentType.Literal("intensity"), intensity);

        this.addSyntax((sender, context) -> {
            Player player = asPlayer(sender);
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
        // submitTask runs its first pass immediately, which is the initial draw.
        Task task = MinecraftServer.getSchedulerManager().submitTask(() -> {
            if (!player.isOnline()) return TaskSchedule.stop();
            this.renderer.render(player, stage.update(intensity));
            return TaskSchedule.millis(TunnelVisionStage.TICK_MILLIS);
        });
        this.previews.put(player.getUuid(), task);
    }

    /**
     * Ends a running preview, leaving whatever is on screen untouched.
     *
     * @param player the player whose preview to end
     */
    private void stopPreview(Player player) {
        Task running = this.previews.remove(player.getUuid());
        if (running != null) running.cancel();
    }

    /**
     * Narrows a sender down to a player, since the preview needs a screen to draw on.
     *
     * @param sender the sender to narrow
     * @return the player, or {@code null} if the sender has no screen
     */
    private static @Nullable Player asPlayer(CommandSender sender) {
        if (sender instanceof Player player) return player;
        sender.sendMessage(Messages.withMiniPrefix("<red>Only players can preview the tunnel vision."));
        return null;
    }
}
