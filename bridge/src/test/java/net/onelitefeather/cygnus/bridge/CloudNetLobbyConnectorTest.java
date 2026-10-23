package net.onelitefeather.cygnus.bridge;

import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import eu.cloudnetservice.modules.bridge.player.executor.ServerSelectorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CloudNetLobbyConnectorTest {

    private final List<String> calls = new ArrayList<>();

    private PlayerManager playerManager() {
        PlayerExecutor executor = (PlayerExecutor) Proxy.newProxyInstance(
                PlayerExecutor.class.getClassLoader(), new Class<?>[]{PlayerExecutor.class}, (proxy, method, args) -> {
                    calls.add(method.getName() + Arrays.toString(args));
                    return null;
                });
        return (PlayerManager) Proxy.newProxyInstance(
                PlayerManager.class.getClassLoader(), new Class<?>[]{PlayerManager.class}, (proxy, method, args) -> {
                    calls.add(method.getName() + Arrays.toString(args));
                    return executor;
                });
    }

    @Test
    @DisplayName("The player executor connects the player to the Lobby task")
    void connectsToTheLobbyTask() {
        UUID id = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        CloudNetLobbyConnector connector = new CloudNetLobbyConnector(this::playerManager);

        connector.connect(id);

        assertEquals(List.of("playerExecutor[" + id + "]",
                        "connectToTask[Lobby, " + ServerSelectorType.LOWEST_PLAYERS + "]"),
                calls, "the executor of the player must be asked for the Lobby task");
    }
}
