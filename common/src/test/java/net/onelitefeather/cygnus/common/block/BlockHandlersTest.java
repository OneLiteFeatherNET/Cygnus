package net.onelitefeather.cygnus.common.block;

import net.minestom.server.instance.block.BlockManager;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MicrotusExtension.class)
class BlockHandlersTest {

    @Test
    void testRegisterAll(@NotNull Env env) {
        BlockManager blockManager = env.process().block();
        BlockHandlers.registerAll(blockManager);

        assertNotNull(blockManager.getHandler("minecraft:sign"));
        assertNotNull(blockManager.getHandler("minecraft:hanging_sign"));
        assertNotNull(blockManager.getHandler("minecraft:shelf"));
        assertNotNull(blockManager.getHandler("minecraft:chiseled_bookshelf"));
        assertNotNull(blockManager.getHandler("minecraft:banner"));
        assertNotNull(blockManager.getHandler("minecraft:lectern"));
        assertNotNull(blockManager.getHandler("minecraft:campfire"));
        assertNotNull(blockManager.getHandler("minecraft:soul_campfire"));
    }
}
