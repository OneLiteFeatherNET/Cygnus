package net.onelitefeather.cygnus.common.block;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;

import java.util.Collection;
import java.util.List;

public class BannerBlockHandler implements BlockHandler {

    private static final Key KEY = Key.key("minecraft", "banner");

    public static final Tag<BinaryTag> PATTERNS = Tag.NBT("patterns");
    public static final Tag<BinaryTag> CUSTOM_NAME = Tag.NBT("CustomName");

    private static final List<Tag<?>> TAGS = List.of(
            PATTERNS,
            CUSTOM_NAME
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
