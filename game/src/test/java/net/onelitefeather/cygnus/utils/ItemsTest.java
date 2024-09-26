package net.onelitefeather.cygnus.utils;

import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.Tags;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MicrotusExtension.class)
class ItemsTest {

    @Test
    void testSlenderEyeSet(@NotNull Env env) {
        Instance instance = env.createFlatInstance();
        Player player = env.createPlayer(instance);
        assertNotNull(player);

        Items items = new Items();

        items.setSlenderEye(player);
        ItemStack eye = player.getInventory().getItemStack(0x00);
        assertNotNull(eye);
        assertNotEquals(Material.AIR, eye.material());
        assertEquals(Material.ENDER_EYE, eye.material());
        assertEquals(0x00, eye.getTag(Tags.ITEM_TAG).byteValue());

        env.destroyInstance(instance, true);
    }
}
