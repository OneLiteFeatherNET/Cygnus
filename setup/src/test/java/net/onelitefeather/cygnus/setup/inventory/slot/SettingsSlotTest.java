package net.onelitefeather.cygnus.setup.inventory.slot;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.instance.Instance;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.config.CreekSettings;
import net.onelitefeather.cygnus.setup.dialog.settings.SettingsForms;
import net.onelitefeather.cygnus.setup.event.dialog.DialogContext;
import net.onelitefeather.cygnus.setup.event.dialog.DialogRequestEvent;
import net.onelitefeather.cygnus.setup.event.dialog.DialogTarget;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class SettingsSlotTest {

    private static final CreekSettings SETTINGS = new CreekSettings(30, 18, 28, 20, 2.5D, 10, 0.0D);

    private EventNode<Event> testNode;
    private Instance instance;

    @BeforeEach
    void setUp(@NotNull Env env) {
        testNode = EventNode.all("test-node-" + UUID.randomUUID());
        env.process().eventHandler().addChild(testNode);
        instance = env.createFlatInstance();
    }

    @AfterEach
    void tearDown(@NotNull Env env) {
        env.process().eventHandler().removeChild(testNode);
        env.destroyInstance(instance, true);
    }

    private static SettingsSlot<CreekSettings> slot() {
        return new SettingsSlot<>(MapDataCategory.CREEK_SETTINGS, DialogTarget.CREEK_SETTINGS, SettingsForms.CREEK, SETTINGS);
    }

    @Test
    void loreShowsEveryValueWithItsUnit() {
        List<String> lore = slot().getItem().get(DataComponents.LORE).stream()
                .map(line -> PlainTextComponentSerializer.plainText().serialize(line))
                .toList();

        assertTrue(lore.contains("» Seen from: 30 blocks"), lore.toString());
        assertTrue(lore.contains("» Links routes within: 2.5 blocks"), lore.toString());
        assertTrue(lore.contains("» Throws a catch: 0 blocks"), lore.toString());
    }

    @Test
    void leftClickAsksForTheDialogWithTheCurrentValues(@NotNull Env env) {
        Player player = env.createPlayer(instance);
        AtomicReference<DialogRequestEvent> request = new AtomicReference<>();
        testNode.addListener(DialogRequestEvent.class, request::set);

        slot().click(player, 0, new Click.Left(0), ItemStack.AIR, _ -> {});

        assertNotNull(request.get(), "a left click must ask for the settings dialog");
        assertEquals(DialogTarget.CREEK_SETTINGS, request.get().getTarget());
        DialogContext.SettingsValues values = assertInstanceOf(DialogContext.SettingsValues.class, request.get().getContext());
        assertEquals(SettingsForms.CREEK.values(SETTINGS), values.values());
    }
}
