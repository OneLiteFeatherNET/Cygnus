package net.onelitefeather.cygnus.setup;

import me.lucko.luckperms.minestom.loader.MinestomLoader;
import net.hollowcube.minestom.extensions.ExtensionBootstrap;
import net.minestom.server.MinecraftServer;
import net.onelitefeather.cygnus.common.bootstrap.ServiceBootstrap;
import net.onelitefeather.cygnus.setup.player.SetupPlayerProvider;

public class SetupLoader {

    static void main() {
        // minestom-extensions loads platform extensions - the CloudNet bridge and our
        // :bridge permission extension among them - from the extensions/ folder. Running
        // standalone simply loads none. This also performs MinecraftServer.init().
        ExtensionBootstrap bootstrap = ExtensionBootstrap.init();
        MinestomLoader.get().load().registerShutdownHook().start();
        new SetupExtension();
        MinecraftServer.getConnectionManager().setPlayerProvider(new SetupPlayerProvider());
        ServiceBootstrap.installShutdownHandling();
        bootstrap.start(ServiceBootstrap.resolveBindHost(), ServiceBootstrap.resolveBindPort());
    }

    private SetupLoader() {
    }
}
