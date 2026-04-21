package net.onelitefeather.cygnus.setup;

import net.minestom.server.MinecraftServer;

public class SetupLoader {

    static void main() {
        MinecraftServer minecraftServer = MinecraftServer.init();
        new SetupExtension();
        minecraftServer.start("localhost", 25565);
    }

    private SetupLoader() {
    }
}
