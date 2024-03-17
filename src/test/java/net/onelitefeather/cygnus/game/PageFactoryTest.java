package net.onelitefeather.cygnus.game;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.Direction;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import net.onelitefeather.cygnus.game.PageFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@EnvTest
class PageFactoryTest {

    @Test
    void testPageCreationWithInvalidDirection(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> PageFactory.createPage(instance, Pos.ZERO, Direction.UP, 0),
                "The direction " + Direction.UP + " is not supported"
        );
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> PageFactory.createPage(instance, Pos.ZERO, Direction.DOWN, 0),
                "The direction " + Direction.DOWN + " is not supported"
        );
        env.destroyInstance(instance);
    }

    @Test
    void testPageCreationWithInvalidPageCount(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> PageFactory.createPage(instance, Pos.ZERO, Direction.SOUTH, -1),
                "The page count can't be zero or negative"
        );
        env.destroyInstance(instance);
    }
}
