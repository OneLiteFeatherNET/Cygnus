package net.onelitefeather.cygnus;

import net.minestom.server.MinecraftServer;
import net.onelitefeather.agones.AgonesAPI;

public final class CygnusLoader {

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();
        MinecraftServer.getSchedulerManager().buildShutdownTask(AgonesAPI.instance()::shutdown);
        new Cygnus();
        server.start("0.0.0.0", 25565);
    }
}
