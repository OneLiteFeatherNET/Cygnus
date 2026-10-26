package net.onelitefeather.cygnus.setup;

import net.hollowcube.minestom.extensions.ExtensionBootstrap;
import net.minestom.server.MinecraftServer;
import net.onelitefeather.cygnus.common.bootstrap.ServiceBootstrap;
import net.onelitefeather.cygnus.common.permission.LuckPermsSupport;
import net.onelitefeather.cygnus.setup.player.SetupPlayerProvider;

public class SetupLoader {

    static void main() {
        // The atmosphere preview registers dimension types while the server runs, which Minestom
        // refuses once its registries are frozen. ServerFlag reads this once when it loads, so it
        // has to be set before anything touches Minestom. The game server keeps the safe default.
        System.setProperty("minestom.registry.unsafe-ops", "true");
        // minestom-extensions loads platform extensions - the CloudNet bridge and our
        // :bridge permission extension among them - from the extensions/ folder. Running
        // standalone simply loads none. This also performs MinecraftServer.init(Auth), which is the
        // only point at which Velocity forwarding can still be turned on.
        ExtensionBootstrap bootstrap = ExtensionBootstrap.init(ServiceBootstrap.resolveAuth());
        LuckPermsSupport.bootstrap();
        new SetupExtension();
        MinecraftServer.getConnectionManager().setPlayerProvider(new SetupPlayerProvider());
        ServiceBootstrap.installShutdownHandling();
        bootstrap.start(ServiceBootstrap.resolveBindHost(), ServiceBootstrap.resolveBindPort());
    }

    private SetupLoader() {
    }
}
