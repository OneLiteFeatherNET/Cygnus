package net.onelitefeather.agones;

import agones.dev.sdk.Sdk;
import agones.dev.sdk.beta.Beta;
import io.grpc.stub.StreamObserver;
import net.infumia.agones4j.Agones;
import net.infumia.agones4j.AgonesCounter;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

final class AgonesAPIImpl implements AgonesAPI {

    private static final AgonesAPIImpl INSTANCE = new AgonesAPIImpl();
    private static final String CURRENT_PLAYERS_COUNTERS_KEY = "current_players";
    private static final AgonesCounterStreamObserver COUNTER_STREAM_OBSERVER = new AgonesCounterStreamObserver();

    private final Agones agones;
    private StreamObserver<Sdk.Empty> healthCheckStream;

    private AgonesAPIImpl() {
        this.agones = Agones.builder().withAddress().build();
        if (null != agones) {
            healthCheckStream = this.agones.healthCheckStream();
        }
    }

    @Override
    public void increaseCurrentPlayerCount() {
        this.agones.increaseCounter(CURRENT_PLAYERS_COUNTERS_KEY, 1, COUNTER_STREAM_OBSERVER);
    }

    @Override
    public void decreaseCurrentPlayerCount() {
        this.agones.decreaseCounter(CURRENT_PLAYERS_COUNTERS_KEY, 1, COUNTER_STREAM_OBSERVER);
    }

    @Override
    public void setReady() {
        this.agones.ready();
    }

    @Override
    public void shutdown() {
        this.agones.shutdown();
    }

    @Override
    public void alive() {
        if (null != healthCheckStream) {
            healthCheckStream.onNext(Sdk.Empty.getDefaultInstance());
        }
    }

    @Override
    public void reserve(Duration duration) {
        this.agones.reserve(duration);
    }

    @Override
    public CompletableFuture<Sdk.Empty> allocateFuture() {
        return this.agones.allocateFuture();
    }

    synchronized static AgonesAPI instance() {
        if (INSTANCE == null) {
            return new AgonesAPIImpl();
        }
        return INSTANCE;
    }

    static class AgonesCounterStreamObserver implements StreamObserver<AgonesCounter> {

        @Override
        public void onNext(AgonesCounter agonesCounter) {

        }

        @Override
        public void onError(Throwable throwable) {

        }

        @Override
        public void onCompleted() {

        }
    }

}
