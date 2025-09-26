package net.onelitefeather.cygnus;

import dev.derklaro.aerogel.Injector;
import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.modules.bridge.impl.platform.minestom.MinestomBridgeExtension;
import net.minestom.server.MinecraftServer;

public final class CygnusLoader {

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();
        new Cygnus();
        try (InjectionLayer<Injector> layer = InjectionLayer.ext()) {
            layer.instance(MinestomBridgeExtension.class).onLoad();
        }
        server.start("0.0.0.0", 25565);
    }
}
