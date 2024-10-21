package net.onelitefeather.cygnus.setup;

import net.minestom.server.MinecraftServer;

public class SetupLoader {

    public static void main(String[] args) {
        MinecraftServer minecraftServer = MinecraftServer.init();
        new SetupExtension();
        minecraftServer.start("localhost", 25565);
    }

    private SetupLoader() {
    }
}
