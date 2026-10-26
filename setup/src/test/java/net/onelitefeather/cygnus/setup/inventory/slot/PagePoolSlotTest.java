package net.onelitefeather.cygnus.setup.inventory.slot;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class PagePoolSlotTest {

    private Instance instance;

    @BeforeEach
    void setUp(@NotNull Env env) {
        instance = env.createFlatInstance();
    }

    @AfterEach
    void tearDown(@NotNull Env env) {
        env.destroyInstance(instance, true);
    }

    @Test
    void activePoolIsShownGreen() {
        Component name = new PagePoolSlot("nord", 3, true, (_, _) -> {}, (_, _) -> {}).getItem()
                .get(DataComponents.CUSTOM_NAME);

        assertNotNull(name);
        assertEquals(NamedTextColor.GREEN, name.color());
    }

    @Test
    void inactivePoolIsShownYellow() {
        Component name = new PagePoolSlot("nord", 3, false, (_, _) -> {}, (_, _) -> {}).getItem()
                .get(DataComponents.CUSTOM_NAME);

        assertNotNull(name);
        assertEquals(NamedTextColor.YELLOW, name.color());
    }

    @Test
    void loreShowsThePageCount() {
        List<Component> lore = new PagePoolSlot("nord", 3, false, (_, _) -> {}, (_, _) -> {}).getItem()
                .get(DataComponents.LORE);

        assertNotNull(lore);
        List<String> lines = lore.stream().map(PlainTextComponentSerializer.plainText()::serialize).toList();
        assertTrue(lines.contains("Pages: 3"), lines.toString());
    }

    @Test
    void leftClickSelectsAndRightClickDeletes(@NotNull Env env) {
        Player player = env.createPlayer(instance);
        AtomicReference<String> selected = new AtomicReference<>();
        AtomicReference<String> deleted = new AtomicReference<>();
        PagePoolSlot slot = new PagePoolSlot("nord", 0, false, (_, name) -> selected.set(name),
                (_, name) -> deleted.set(name));

        slot.getClick().onClick(player, 0, new Click.Right(0), ItemStack.AIR, _ -> {});
        assertNull(selected.get());
        assertEquals("nord", deleted.get());

        slot.getClick().onClick(player, 0, new Click.Left(0), ItemStack.AIR, _ -> {});
        assertEquals("nord", selected.get());
    }
}
