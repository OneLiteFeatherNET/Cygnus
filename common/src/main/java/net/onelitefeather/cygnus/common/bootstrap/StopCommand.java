package net.onelitefeather.cygnus.common.bootstrap;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

/**
 * Shuts the service down cleanly. Reserved for non-player senders (the console/CloudNet) and
 * players holding {@value #PERMISSION}, since a service should not be stoppable by regular players.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.6.7
 **/
public final class StopCommand extends Command {

    private static final String PERMISSION = "cygnus.command.stop";

    /**
     * Creates a new instance of the {@link StopCommand} and wires its condition and executor.
     */
    public StopCommand() {
        super("stop");
        setCondition((sender, commandString) -> !(sender instanceof Player player) || hasStopPermission(player));
        setDefaultExecutor((sender, context) -> Thread.ofPlatform().name("cygnus-shutdown").start(() -> {
            MinecraftServer.stopCleanly();
            System.exit(0);
        }));
    }

    /**
     * Checks whether the given player is allowed to run this command via LuckPerms.
     * <p>
     * Assumes LuckPerms has already been bootstrapped (see {@code MinestomLoader}), which is
     * guaranteed by the time a player can connect and send commands.
     *
     * @param player the player to check
     * @return {@code true} if the player holds {@value #PERMISSION}, {@code false} otherwise
     * (including when LuckPerms has no cached data for the player yet)
     */
    private static boolean hasStopPermission(Player player) {
        User user = LuckPermsProvider.get().getUserManager().getUser(player.getUuid());
        return user != null && user.getCachedData().getPermissionData().checkPermission(PERMISSION).asBoolean();
    }
}
