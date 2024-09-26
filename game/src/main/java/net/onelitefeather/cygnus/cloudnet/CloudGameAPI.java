package net.onelitefeather.cygnus.cloudnet;

import org.jetbrains.annotations.NotNull;

public sealed interface CloudGameAPI permits CloudNetCloudGameAPI, DummyCloudGameAPI {

    static @NotNull CloudGameAPI cloudGameAPI() {
        try {
            Class.forName("eu.cloudnetservice.wrapper.Main");
            return new CloudNetCloudGameAPI();
        } catch (ClassNotFoundException e) {
            return new DummyCloudGameAPI();
        }
    }

    void switchInGame();

}
