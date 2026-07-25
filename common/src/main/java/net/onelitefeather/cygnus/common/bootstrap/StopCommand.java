package net.onelitefeather.cygnus.common.bootstrap;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;

/**
 * Shuts the service down cleanly. Reserved for non-player senders (the console/CloudNet), since
 * a service should not be stoppable by regular players.
 */
public final class StopCommand extends Command {

    public StopCommand() {
        super("stop");
        setCondition((sender, commandString) -> !(sender instanceof Player));
        setDefaultExecutor((sender, context) -> Thread.ofPlatform().name("cygnus-shutdown").start(() -> {
            MinecraftServer.stopCleanly();
            System.exit(0);
        }));
    }
}
