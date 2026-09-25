package net.onelitefeather.cygnus.common.page;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.metadata.display.ItemDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.Direction;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.util.Helper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class PageEntityTest {

    @Test
    void testPageEntityCreation(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        PageEntity pageEntity = placedPage(instance);

        assertNotNull(pageEntity);
        assertNotNull(pageEntity.getPageItem());
        assertEquals(Helper.updatePosition(Pos.ZERO, Direction.NORTH), pageEntity.getPosition());
        assertNotEquals(pageEntity.getUuid(), pageEntity.getHitBoxUUID());
        Component displayName = pageEntity.getPageItem().get(DataComponents.CUSTOM_NAME);

        assertNotNull(displayName);

        String rawName = PlainTextComponentSerializer.plainText().serialize(displayName);

        assertEquals("Page: 1", rawName);

        pageEntity.remove();
        env.destroyInstance(instance);
    }

    @Test
    void testInteractionStateFollowsEnableAndDisable(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        PageEntity pageEntity = placedPage(instance);

        assertTrue(pageEntity.isInteractable(), "a freshly spawned page must be collectible");

        pageEntity.disableInteraction();
        assertFalse(pageEntity.isInteractable(), "a page whose TTL ran out must not count as collectible");

        pageEntity.enableInteraction();
        assertTrue(pageEntity.isInteractable(), "a page put back into play must count as collectible again");

        pageEntity.remove();
        env.destroyInstance(instance);
    }

    @Test
    void testPageIsLitIndependentlyFromTheEnvironment(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        PageEntity pageEntity = placedPage(instance);
        ItemDisplayMeta itemDisplayMeta = (ItemDisplayMeta) pageEntity.getEntityMeta();

        int blockLight = itemDisplayMeta.getBlockLight();
        assertTrue(blockLight >= PageLightUtil.DEFAULT_MIN_INITIAL_BLOCK_LIGHT
                && blockLight <= PageLightUtil.DEFAULT_MAX_INITIAL_BLOCK_LIGHT,
                "a page must be lit within the configured initial block light bounds");
        assertEquals(pageEntity.getInitialBlockLight(), blockLight,
                "the display block light must match the page's initial block light");
        assertEquals(PageLightUtil.PAGE_SKY_LIGHT, itemDisplayMeta.getSkyLight(),
                "the sky light must stay at zero so the brightness does not follow the time of day");

        pageEntity.remove();
        env.destroyInstance(instance);
    }

    @Test
    void testPageBrightnessResetOnEnableInteraction(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        PageEntity pageEntity = placedPage(instance);
        ItemDisplayMeta itemDisplayMeta = (ItemDisplayMeta) pageEntity.getEntityMeta();

        pageEntity.disableInteraction();
        pageEntity.enableInteraction();

        int reEnabledLight = itemDisplayMeta.getBlockLight();
        assertTrue(reEnabledLight >= PageLightUtil.DEFAULT_MIN_INITIAL_BLOCK_LIGHT
                && reEnabledLight <= PageLightUtil.DEFAULT_MAX_INITIAL_BLOCK_LIGHT,
                "re-enabling a page must re-roll a valid initial block light");
        assertEquals(pageEntity.getInitialBlockLight(), reEnabledLight,
                "the display block light must match the newly rolled initial block light");

        pageEntity.remove();
        env.destroyInstance(instance);
    }

    @Test
    void testPageIsOnlyInTheWorldOncePlaced(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        PageEntity pageEntity = new PageEntity(new PageResource(Pos.ZERO, Direction.NORTH), 1);
        assertNull(pageEntity.getInstance(), "creating a page must not put it into the world yet");

        pageEntity.place(instance).join();
        assertEquals(instance, pageEntity.getInstance());

        pageEntity.remove();
        env.destroyInstance(instance);
    }

    @Test
    void testAHiddenPageComesBackOnceItsDelayIsOver(@NotNull Env env) throws Exception {
        Instance instance = env.createFlatInstance();
        PageEntity pageEntity = placedPage(instance);
        Field tickTime = PageEntity.class.getDeclaredField("currentTickTime");
        tickTime.setAccessible(true);

        pageEntity.hideFor(3);
        assertFalse(pageEntity.isInteractable());

        tickTime.setInt(pageEntity, 2);
        pageEntity.tick(0);
        assertFalse(pageEntity.isInteractable(), "the page must stay hidden until its delay is over");

        tickTime.setInt(pageEntity, 3);
        pageEntity.tick(0);
        assertTrue(pageEntity.isInteractable(), "the page must come back once its delay is over");

        pageEntity.remove();
        env.destroyInstance(instance);
    }

    private static PageEntity placedPage(Instance instance) {
        PageEntity pageEntity = new PageEntity(new PageResource(Pos.ZERO, Direction.NORTH), 1);
        pageEntity.place(instance).join();
        return pageEntity;
    }
}
