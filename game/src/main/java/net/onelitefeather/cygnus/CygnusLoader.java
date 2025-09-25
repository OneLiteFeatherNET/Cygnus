package net.onelitefeather.cygnus;

import net.minestom.server.MinecraftServer;

public final class CygnusLoader {

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();
        new Cygnus();
        server.start("0.0.0.0", 25565);
    }
}
