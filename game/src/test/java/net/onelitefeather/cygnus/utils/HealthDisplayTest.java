package net.onelitefeather.cygnus.utils;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MicrotusExtension.class)
class HealthDisplayTest {

    @Test
    void testHeartsShowLeftAndMaximum(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        player.setHealth(7.0F);

        String hearts = PlainTextComponentSerializer.plainText().serialize(HealthDisplay.hearts(player));

        assertEquals("❤ 3.5 / 10", hearts);
        env.destroyInstance(instance, true);
    }

    @Test
    void testFormatHeartsRoundsToHalfHearts() {
        assertEquals("10", HealthDisplay.formatHearts(20.0F));
        assertEquals("3.5", HealthDisplay.formatHearts(7.0F));
        assertEquals("0", HealthDisplay.formatHearts(0.0F));
    }
}
