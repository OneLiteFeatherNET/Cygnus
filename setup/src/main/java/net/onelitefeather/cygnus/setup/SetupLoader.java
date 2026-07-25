package net.onelitefeather.cygnus.setup;

import net.minestom.server.MinecraftServer;
import net.onelitefeather.cygnus.common.bootstrap.ServiceBootstrap;
import net.onelitefeather.cygnus.setup.player.SetupPlayerProvider;

public class SetupLoader {

    static void main() {
        MinecraftServer minecraftServer = MinecraftServer.init();
        new SetupExtension();
        MinecraftServer.getConnectionManager().setPlayerProvider(new SetupPlayerProvider());
        ServiceBootstrap.installShutdownHandling();
        minecraftServer.start(ServiceBootstrap.resolveBindHost(), ServiceBootstrap.resolveBindPort());
    }

    private SetupLoader() {
    }
}
