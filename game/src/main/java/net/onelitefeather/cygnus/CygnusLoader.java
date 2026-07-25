package net.onelitefeather.cygnus;

import dev.derklaro.aerogel.Injector;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.modules.bridge.impl.platform.minestom.MinestomBridgeExtension;
import me.lucko.luckperms.minestom.loader.MinestomLoader;
import net.minestom.server.MinecraftServer;
import net.onelitefeather.cygnus.common.bootstrap.ServiceBootstrap;
import net.onelitefeather.cygnus.common.dimension.DimensionFactory;

public final class CygnusLoader {

    static void main() {
        MinecraftServer server = MinecraftServer.init();
        MinestomLoader.get().load().registerShutdownHook().start();
        String customDimensions = System.getProperty("cygnus.customDimension", "false");
        if (Boolean.parseBoolean(customDimensions)) {
            DimensionFactory.registerAll();
        }
        new Cygnus();
        try (InjectionLayer<Injector> layer = InjectionLayer.ext()) {
            layer.instance(MinestomBridgeExtension.class).onLoad();
        }
        ServiceBootstrap.installShutdownHandling();
        server.start(ServiceBootstrap.resolveBindHost(), ServiceBootstrap.resolveBindPort());
    }
}
