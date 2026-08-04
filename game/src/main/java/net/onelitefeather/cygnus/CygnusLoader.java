package net.onelitefeather.cygnus;

import net.hollowcube.minestom.extensions.ExtensionBootstrap;
import net.onelitefeather.cygnus.common.bootstrap.ServiceBootstrap;
import net.onelitefeather.cygnus.common.dimension.DimensionFactory;
import net.onelitefeather.cygnus.common.permission.LuckPermsSupport;

public final class CygnusLoader {

    static void main() {
        // minestom-extensions loads platform extensions - the CloudNet bridge and our
        // :bridge permission extension among them - from the extensions/ folder. Running
        // standalone simply loads none. This also performs MinecraftServer.init().
        ExtensionBootstrap bootstrap = ExtensionBootstrap.init();
        LuckPermsSupport.bootstrap();
        String customDimensions = System.getProperty("cygnus.customDimension", "false");
        if (Boolean.parseBoolean(customDimensions)) {
            DimensionFactory.registerAll();
        }
        new Cygnus();
        ServiceBootstrap.installShutdownHandling();
        bootstrap.start(ServiceBootstrap.resolveBindHost(), ServiceBootstrap.resolveBindPort());
    }

    private CygnusLoader() {
    }
}
