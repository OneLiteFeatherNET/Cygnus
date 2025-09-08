package net.onelitefeather.cygnus.common.page;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MicrotusExtension.class)
class PageEntityTest {

    @Test
    void testPageEntityCreation(@NotNull Env env) {
        Instance instance = env.createFlatInstance();

        PageEntity pageEntity = new PageEntity(instance, Pos.ZERO, 1);

        assertNotNull(pageEntity);
        assertNotNull(pageEntity.getPageItem());
        assertEquals(Pos.ZERO, pageEntity.getPosition());
        assertNotEquals(pageEntity.getUuid(), pageEntity.getHitBoxUUID());
        Component displayName = pageEntity.getPageItem().get(DataComponents.CUSTOM_NAME);

        assertNotNull(displayName);

        String rawName = PlainTextComponentSerializer.plainText().serialize(displayName);

        assertEquals("Page: 1", rawName);

        pageEntity.remove();
        env.destroyInstance(instance);
    }
}
