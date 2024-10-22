package net.onelitefeather.cygnus;

import agones.dev.sdk.Sdk;
import io.grpc.stub.StreamObserver;
import net.infumia.agones4j.Agones;
import net.minestom.server.MinecraftServer;

public final class CygnusLoader {

    private final static String EXTERNAL_SERVER_NAME = "%s.games.onelitefeather.dev";

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();
        try(Agones agones = Agones.builder().withAddress().build()) {
            MinecraftServer.getSchedulerManager().buildShutdownTask(agones::shutdown);
            new Cygnus(agones);
            StreamObserver<Sdk.Empty> emptyStreamObserver = agones.healthCheckStream();
            emptyStreamObserver.onNext(Sdk.Empty.getDefaultInstance());
            agones.setLabel("game", "cygnus");
            agones.setLabel("server", "microtus");
            agones.setLabel("type", "game");
            agones.getGameServerFuture().thenAccept(gameServer -> {
                agones.setAnnotation("mc-router.itzg.me/externalServerName", EXTERNAL_SERVER_NAME.formatted(gameServer.getObjectMeta().getName()));

            });
            server.start("0.0.0.0", 25565);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
