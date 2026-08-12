package net.onelitefeather.cygnus.hud.player;

import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import net.onelitefeather.cygnus.player.CygnusPlayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerHudContainerTest extends CygnusPlayerTestBase {

    private static Instance instance;
    private static CygnusPlayer player;

    @BeforeAll
    static void setup(@NotNull Env env) {
        instance = env.createFlatInstance();
        player = (CygnusPlayer) env.createPlayer(instance);
    }

    @AfterAll
    static void teardown(@NotNull Env env) {
        env.destroyInstance(instance, true);
        instance = null;
        player = null;
    }

    static class DummyScoreboardComponent extends PersonalHudComponent {
        boolean rendered = false;
        boolean hidden = false;

        public DummyScoreboardComponent(CygnusPlayer player) {
            super(player);
        }

        @Override
        public void render() {
            rendered = true;
        }

        @Override
        public void hide() {
            hidden = true;
            visible = false;
        }
    }

    @Test
    void testRegisterGetAndRenderAll() {
        PlayerHudContainer container = new PlayerHudContainer();
        DummyScoreboardComponent scoreboard = new DummyScoreboardComponent(player);

        container.register(DummyScoreboardComponent.class, scoreboard);
        assertTrue(container.get(DummyScoreboardComponent.class).isPresent());
        assertEquals(scoreboard, container.get(DummyScoreboardComponent.class).get());

        container.renderAll();
        assertTrue(scoreboard.rendered);

        container.hideAll();
        assertTrue(scoreboard.hidden);
    }
}
