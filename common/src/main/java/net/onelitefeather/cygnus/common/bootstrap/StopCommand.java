package net.onelitefeather.cygnus.common.bootstrap;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

/**
 * Shuts the service down cleanly. Reserved for non-player senders (the console/CloudNet) and
 * players holding {@value #PERMISSION}, since a service should not be stoppable by regular players.
 */
public final class StopCommand extends Command {

    private static final String PERMISSION = "cygnus.command.stop";

    public StopCommand() {
        super("stop");
        setCondition((sender, commandString) -> !(sender instanceof Player player) || hasStopPermission(player));
        setDefaultExecutor((sender, context) -> Thread.ofPlatform().name("cygnus-shutdown").start(() -> {
            MinecraftServer.stopCleanly();
            System.exit(0);
        }));
    }

    private static boolean hasStopPermission(Player player) {
        User user = LuckPermsProvider.get().getUserManager().getUser(player.getUuid());
        return user != null && user.getCachedData().getPermissionData().checkPermission(PERMISSION).asBoolean();
    }
}
