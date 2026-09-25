package net.onelitefeather.cygnus.common.page;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.Direction;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.util.Helper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class PageFactoryTest {

    @Test
    void testPageCreationWithInvalidDirection() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> PageFactory.createPage(new PageResource(Pos.ZERO, Direction.UP), 0),
                "The direction " + Direction.UP + " is not supported"
        );
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> PageFactory.createPage(new PageResource(Pos.ZERO, Direction.DOWN), 0),
                "The direction " + Direction.DOWN + " is not supported"
        );
    }

    @Test
    void testPageCreationWithInvalidPageCount() {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> PageFactory.createPage(new PageResource(Pos.ZERO, Direction.SOUTH), -1),
                "The page count can't be zero or negative"
        );
    }

    @Test
    void testPageCreationViaFactory(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        PageResource resource = new PageResource(Pos.ZERO, Direction.SOUTH);
        PageEntity pageEntity = PageFactory.createPage(resource, 1);
        pageEntity.place(instance).join();

        assertEquals(resource, pageEntity.getResource());
        assertEquals(Helper.updatePosition(Pos.ZERO, Direction.SOUTH), pageEntity.getPosition());
        assertEquals(instance.getUuid(), pageEntity.getInstance().getUuid());
        pageEntity.remove();

        env.destroyInstance(instance);
    }
}
