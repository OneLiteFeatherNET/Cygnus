package net.onelitefeather.cygnus.setup;

import me.lucko.luckperms.minestom.loader.MinestomLoader;
import net.minestom.server.MinecraftServer;
import net.onelitefeather.cygnus.common.bootstrap.ServiceBootstrap;
import net.onelitefeather.cygnus.setup.player.SetupPlayerProvider;

public class SetupLoader {

    static void main() {
        MinecraftServer minecraftServer = MinecraftServer.init();
        MinestomLoader.get().load().registerShutdownHook().start();
        new SetupExtension();
        MinecraftServer.getConnectionManager().setPlayerProvider(new SetupPlayerProvider());
        ServiceBootstrap.installShutdownHandling();
        minecraftServer.start(ServiceBootstrap.resolveBindHost(), ServiceBootstrap.resolveBindPort());
    }

    private SetupLoader() {
    }
}
