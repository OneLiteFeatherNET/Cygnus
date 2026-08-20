package net.onelitefeather.cygnus.common.block;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignBlockHandlerTest {

    @Test
    void testSignKey() {
        SignBlockHandler handler = new SignBlockHandler();
        assertEquals(Key.key("minecraft:sign"), handler.getKey());
    }

    @Test
    void testHangingSignKey() {
        HangingSignBlockHandler handler = new HangingSignBlockHandler();
        assertEquals(Key.key("minecraft:hanging_sign"), handler.getKey());
    }

    @Test
    void testBlockEntityTags() {
        SignBlockHandler handler = new SignBlockHandler();
        var tags = handler.getBlockEntityTags();

        assertEquals(3, tags.size());
        assertTrue(tags.contains(SignBlockHandler.IS_WAXED));
        assertTrue(tags.contains(SignBlockHandler.FRONT_TEXT));
        assertTrue(tags.contains(SignBlockHandler.BACK_TEXT));

        HangingSignBlockHandler hangingHandler = new HangingSignBlockHandler();
        assertEquals(tags, hangingHandler.getBlockEntityTags());
    }

    @Test
    void testBlockWithHandlerPreservesTags() {
        SignBlockHandler handler = new SignBlockHandler();
        CompoundBinaryTag frontText = CompoundBinaryTag.builder()
                .putString("color", "black")
                .putByte("has_glowing_text", (byte) 0)
                .build();

        Block block = Block.OAK_SIGN
                .withHandler(handler)
                .withTag(SignBlockHandler.FRONT_TEXT, frontText)
                .withTag(SignBlockHandler.IS_WAXED, (byte) 1);

        assertNotNull(block.handler());
        assertEquals(handler, block.handler());
        assertEquals(frontText, block.getTag(SignBlockHandler.FRONT_TEXT));
        assertEquals((byte) 1, block.getTag(SignBlockHandler.IS_WAXED));
    }
}
