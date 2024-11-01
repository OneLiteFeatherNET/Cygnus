package net.onelitefeather.cygnus;

import net.minestom.server.MinecraftServer;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.onelitefeather.agones.AgonesAPI;
import net.onelitefeather.cygnus.common.CygnusFlag;

public final class CygnusLoader {

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();
        CygnusFlag.VELOCITY_SECRET.ifPresent(VelocityProxy::enable);
        if (CygnusFlag.AGONES_SUPPORT.isPresent())
        MinecraftServer.getSchedulerManager().buildShutdownTask(AgonesAPI.instance()::shutdown);
        new Cygnus();
        server.start("0.0.0.0", 25565);
    }
}
