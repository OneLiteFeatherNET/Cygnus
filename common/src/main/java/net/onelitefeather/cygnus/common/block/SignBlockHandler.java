package net.onelitefeather.cygnus.common.block;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;

import java.util.Collection;
import java.util.List;

public class SignBlockHandler implements BlockHandler {

    private static final Key KEY = Key.key("minecraft", "sign");

    public static final Tag<Byte> IS_WAXED = Tag.Byte("is_waxed");
    public static final Tag<BinaryTag> FRONT_TEXT = Tag.NBT("front_text");
    public static final Tag<BinaryTag> BACK_TEXT = Tag.NBT("back_text");

    static final List<Tag<?>> TAGS = List.of(
            IS_WAXED,
            FRONT_TEXT,
            BACK_TEXT
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
