package net.onelitefeather.cygnus.hud;

import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.hud.player.PersonalHudComponent;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HudComponentTest extends CygnusPlayerTestBase {

    private static Instance instance;
    private static CygnusPlayer player;

    @BeforeAll
    static void setup(@NotNull Env env) {
        env.process().connection().setPlayerProvider(CygnusPlayer::new);
        instance = env.createFlatInstance();
        player = (CygnusPlayer) env.createPlayer(instance);
    }

    @AfterAll
    static void teardown(@NotNull Env env) {
        env.destroyInstance(instance, true);
        instance = null;
        player = null;
    }

    @Test
    void testPersonalHudComponentVisibility() {
        PersonalHudComponent component = new PersonalHudComponent(player) {
            @Override
            public void render() {}
            @Override
            public void hide() {
                visible = false;
            }
        };

        assertTrue(component.isVisible());
        assertEquals(player, component.getPlayer());
        component.hide();
        assertFalse(component.isVisible());
    }

    @Test
    void testGlobalHudComponentAddRemovePlayer() {
        GlobalHudComponent component = new GlobalHudComponent() {
            @Override
            public void render() {}
            @Override
            public void hide() {
                visible = false;
            }
        };

        assertTrue(component.getPlayers().isEmpty());
        component.addPlayer(player);
        assertTrue(component.getPlayers().contains(player));
        component.removePlayer(player);
        assertFalse(component.getPlayers().contains(player));
    }
}
