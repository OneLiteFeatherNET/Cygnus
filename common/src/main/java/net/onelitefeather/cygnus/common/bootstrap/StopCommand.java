package net.onelitefeather.cygnus.common.bootstrap;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.util.TriState;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

/**
 * Shuts the service down cleanly. Reserved for non-player senders (the console/CloudNet) and
 * players holding {@value #PERMISSION}, since a service should not be stoppable by regular players.
 *
 * @author TheMeinerLP
 * @version 1.1.0
 * @since 2.6.7
 **/
public final class StopCommand extends Command {

    private static final String PERMISSION = "cygnus.command.stop";

    /**
     * Creates a new instance of the {@link StopCommand} and wires its condition and executor.
     */
    public StopCommand() {
        super("stop");
        setCondition((sender, commandString) -> !(sender instanceof Player) || hasStopPermission(sender));
        setDefaultExecutor((sender, context) -> Thread.ofPlatform().name("cygnus-shutdown").start(() -> {
            MinecraftServer.stopCleanly();
            System.exit(0);
        }));
    }

    /**
     * Checks whether the given sender is allowed to run this command.
     * <p>
     * Reads Adventure's {@link PermissionChecker#POINTER}, which our player implementation backs
     * with LuckPerms (see {@code PermissionAwarePlayer}). A sender without that pointer is denied.
     *
     * @param sender the sender to check
     * @return {@code true} if the sender holds {@value #PERMISSION}, {@code false} otherwise
     */
    private static boolean hasStopPermission(CommandSender sender) {
        return sender.getOrDefault(PermissionChecker.POINTER, PermissionChecker.always(TriState.FALSE))
                .test(PERMISSION);
    }
}
