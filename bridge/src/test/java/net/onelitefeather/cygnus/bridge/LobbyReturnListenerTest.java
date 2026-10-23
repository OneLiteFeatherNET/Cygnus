package net.onelitefeather.cygnus.bridge;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.event.LobbyReturnRequestEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class LobbyReturnListenerTest {

    private Player player(Env env) {
        return env.createConnection().connect(env.createFlatInstance(), new Pos(0, 40, 0));
    }

    @Test
    @DisplayName("A request connects the player and is marked as handled")
    void connectsThePlayer(Env env) {
        Player player = player(env);
        List<UUID> connected = new ArrayList<>();
        LobbyReturnListener listener = new LobbyReturnListener(connected::add);
        LobbyReturnRequestEvent event = new LobbyReturnRequestEvent(player);

        listener.accept(event);

        assertEquals(List.of(player.getUuid()), connected, "the player must be connected once");
        assertTrue(event.handled(), "a successful connect must handle the request");
    }

    @Test
    @DisplayName("A failing connect leaves the request unhandled")
    void failureLeavesRequestUnhandled(Env env) {
        LobbyReturnListener listener = new LobbyReturnListener(id -> {
            throw new IllegalStateException("no cloudnet");
        });
        LobbyReturnRequestEvent event = new LobbyReturnRequestEvent(player(env));

        assertDoesNotThrow(() -> listener.accept(event));

        assertFalse(event.handled(), "the game needs its fallback when the connect failed");
    }
}
