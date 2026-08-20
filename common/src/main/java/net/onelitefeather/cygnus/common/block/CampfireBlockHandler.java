package net.onelitefeather.cygnus.common.block;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;

import java.util.Collection;
import java.util.List;

public class CampfireBlockHandler implements BlockHandler {

    private static final Key KEY = Key.key("minecraft", "campfire");

    public static final Tag<BinaryTag> ITEMS = Tag.NBT("Items");
    public static final Tag<BinaryTag> COOKING_TIMES = Tag.NBT("CookingTimes");
    public static final Tag<BinaryTag> COOKING_TOTAL_TIMES = Tag.NBT("CookingTotalTimes");

    static final List<Tag<?>> TAGS = List.of(
            ITEMS,
            COOKING_TIMES,
            COOKING_TOTAL_TIMES
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
