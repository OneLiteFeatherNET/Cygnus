package net.onelitefeather.agones;

import agones.dev.sdk.Sdk;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * The Agones Integration.
 */
public sealed interface AgonesAPI permits AgonesAPIImpl {

    void increaseCurrentPlayerCount();

    void decreaseCurrentPlayerCount();

    /**
     * Sets the GameServer to ready.
     */
    void setReady();

    /**
     * Shuts down the GameServer.
     */
    void shutdown();

    /**
     * Sends a health check to the GameServer.
     */
    void alive();

    /**
     * Reserves the GameServer for a specific duration.
     *
     * @param duration the duration to reserve the GameServer
     */
    void reserve(Duration duration);

    CompletableFuture<Sdk.Empty> allocateFuture();

    /**
     * Creates a new instance of the Agones API.
     *
     * @return a new instance of the Agones API
     */
    static AgonesAPI instance() {
        return AgonesAPIImpl.instance();
    }

}
