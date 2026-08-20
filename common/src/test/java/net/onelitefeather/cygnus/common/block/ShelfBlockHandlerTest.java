package net.onelitefeather.cygnus.common.block;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.instance.block.Block;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShelfBlockHandlerTest {

    @Test
    void testShelfKey() {
        ShelfBlockHandler handler = new ShelfBlockHandler();
        assertEquals(Key.key("minecraft:shelf"), handler.getKey());
    }

    @Test
    void testChiseledBookshelfKey() {
        ChiseledBookshelfBlockHandler handler = new ChiseledBookshelfBlockHandler();
        assertEquals(Key.key("minecraft:chiseled_bookshelf"), handler.getKey());
    }

    @Test
    void testBlockEntityTags() {
        ShelfBlockHandler handler = new ShelfBlockHandler();
        var tags = handler.getBlockEntityTags();

        assertEquals(2, tags.size());
        assertTrue(tags.contains(ShelfBlockHandler.ITEMS));
        assertTrue(tags.contains(ShelfBlockHandler.LAST_INTERACTED_SLOT));

        ChiseledBookshelfBlockHandler chiseledHandler = new ChiseledBookshelfBlockHandler();
        assertEquals(tags, chiseledHandler.getBlockEntityTags());
    }

    @Test
    void testBlockWithHandlerPreservesTags() {
        ShelfBlockHandler handler = new ShelfBlockHandler();
        CompoundBinaryTag item = CompoundBinaryTag.builder()
                .putByte("Slot", (byte) 0)
                .putString("id", "minecraft:written_book")
                .putByte("Count", (byte) 1)
                .build();
        ListBinaryTag itemsList = ListBinaryTag.builder().add(item).build();

        Block block = Block.CHISELED_BOOKSHELF
                .withHandler(handler)
                .withTag(ShelfBlockHandler.ITEMS, itemsList)
                .withTag(ShelfBlockHandler.LAST_INTERACTED_SLOT, 0);

        assertNotNull(block.handler());
        assertEquals(handler, block.handler());
        assertEquals(itemsList, block.getTag(ShelfBlockHandler.ITEMS));
        assertEquals(0, block.getTag(ShelfBlockHandler.LAST_INTERACTED_SLOT));
    }
}
