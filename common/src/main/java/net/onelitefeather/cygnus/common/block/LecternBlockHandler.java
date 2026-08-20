package net.onelitefeather.cygnus.common.block;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;

import java.util.Collection;
import java.util.List;

public class LecternBlockHandler implements BlockHandler {

    private static final Key KEY = Key.key("minecraft", "lectern");

    public static final Tag<BinaryTag> BOOK = Tag.NBT("Book");
    public static final Tag<Integer> PAGE = Tag.Integer("Page");

    private static final List<Tag<?>> TAGS = List.of(
            BOOK,
            PAGE
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
