package net.onelitefeather.cygnus.common.block;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;

import java.util.Collection;
import java.util.List;

public class ShelfBlockHandler implements BlockHandler {

    private static final Key KEY = Key.key("minecraft", "shelf");

    public static final Tag<BinaryTag> ITEMS = Tag.NBT("Items");
    public static final Tag<Integer> LAST_INTERACTED_SLOT = Tag.Integer("last_interacted_slot");

    static final List<Tag<?>> TAGS = List.of(
            ITEMS,
            LAST_INTERACTED_SLOT
    );

    /**
     * {@inheritDoc}
     */
    @Override
    public Key getKey() {
        return KEY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Tag<?>> getBlockEntityTags() {
        return TAGS;
    }
}
