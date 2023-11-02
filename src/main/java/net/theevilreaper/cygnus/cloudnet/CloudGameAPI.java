package net.theevilreaper.cygnus.cloudnet;

public sealed interface CloudGameAPI permits CloudNetCloudGameAPI, DummyCloudGameAPI {

    public static CloudGameAPI cloudGameAPI() {
        try {
            Class.forName("eu.cloudnetservice.wrapper.Main");
            return new CloudNetCloudGameAPI();
        } catch (ClassNotFoundException e) {
            return new DummyCloudGameAPI();
        }
    }
    void switchInGame();

}
