package net.onelitefeather.cygnus.common.block;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;

import java.util.Collection;

public class ChiseledBookshelfBlockHandler implements BlockHandler {

    private static final Key KEY = Key.key("minecraft", "chiseled_bookshelf");

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
        return ShelfBlockHandler.TAGS;
    }
}
