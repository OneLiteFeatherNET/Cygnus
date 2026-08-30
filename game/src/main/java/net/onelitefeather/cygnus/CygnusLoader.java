package net.onelitefeather.cygnus;

import net.hollowcube.minestom.extensions.ExtensionBootstrap;
import net.onelitefeather.cygnus.common.block.BlockHandlers;
import net.onelitefeather.cygnus.common.bootstrap.ServiceBootstrap;
import net.onelitefeather.cygnus.common.permission.LuckPermsSupport;

public final class CygnusLoader {

    public static void main(String[] args) {
        // minestom-extensions loads platform extensions - the CloudNet bridge and our
        // :bridge permission extension among them - from the extensions/ folder. Running
        // standalone simply loads none. This also performs MinecraftServer.init(Auth), which is the
        // only point at which Velocity forwarding can still be turned on.
        ExtensionBootstrap bootstrap = ExtensionBootstrap.init(ServiceBootstrap.resolveAuth());
        LuckPermsSupport.bootstrap();
        BlockHandlers.registerAll();
        new Cygnus();
        ServiceBootstrap.installShutdownHandling();
        bootstrap.start(ServiceBootstrap.resolveBindHost(), ServiceBootstrap.resolveBindPort());
    }

    private CygnusLoader() {
    }
}
