package net.onelitefeather.cygnus.utils;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@link PlayerState} tracks one value per player, keeps players apart, and forgets
 * them cleanly.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
@ExtendWith(MicrotusExtension.class)
class PlayerStateTest {

    @Test
    void nothingIsTrackedForAFreshPlayer(Env env) {
        Player player = spawn(env);
        PlayerState<String> state = new PlayerState<>();

        assertNull(state.get(player));
        assertTrue(state.isEmpty());
    }

    @Test
    void putThenGetReturnsTheStoredValue(Env env) {
        Player player = spawn(env);
        PlayerState<String> state = new PlayerState<>();

        state.put(player, "value");

        assertEquals("value", state.get(player));
        assertFalse(state.isEmpty());
    }

    @Test
    void playersAreKeptApart(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = env.createPlayer(instance);
        Player second = env.createPlayer(instance);
        PlayerState<String> state = new PlayerState<>();

        state.put(first, "first");
        state.put(second, "second");

        assertEquals("first", state.get(first));
        assertEquals("second", state.get(second));
    }

    @Test
    void removeForgetsThePlayerAndReturnsTheOldValue(Env env) {
        Player player = spawn(env);
        PlayerState<String> state = new PlayerState<>();
        state.put(player, "value");

        assertEquals("value", state.remove(player));
        assertNull(state.get(player));
        assertNull(state.remove(player), "removing an untracked player must not throw");
    }

    @Test
    void computeIfAbsentStoresAndReusesTheComputedValue(Env env) {
        Player player = spawn(env);
        PlayerState<StringBuilder> state = new PlayerState<>();

        StringBuilder first = state.computeIfAbsent(player, StringBuilder::new);
        StringBuilder second = state.computeIfAbsent(player, StringBuilder::new);

        assertEquals(first, second, "a second call must not overwrite the already-tracked value");
    }

    @Test
    void clearForgetsEveryPlayer(Env env) {
        Instance instance = env.createFlatInstance();
        Player first = env.createPlayer(instance);
        Player second = env.createPlayer(instance);
        PlayerState<String> state = new PlayerState<>();
        state.put(first, "first");
        state.put(second, "second");

        state.clear();

        assertTrue(state.isEmpty());
    }

    @Test
    void removingThroughValuesForgetsThePlayerToo(Env env) {
        Player player = spawn(env);
        PlayerState<String> state = new PlayerState<>();
        state.put(player, "value");

        Iterator<String> values = state.values().iterator();
        values.next();
        values.remove();

        assertTrue(state.isEmpty(), "the map backing values() must be live, the way BloodSplatterService needs it");
        assertNull(state.get(player));
    }

    private Player spawn(Env env) {
        Instance instance = env.createFlatInstance();
        return env.createPlayer(instance);
    }
}
