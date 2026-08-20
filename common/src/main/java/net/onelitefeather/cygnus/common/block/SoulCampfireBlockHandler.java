package net.onelitefeather.cygnus.common.block;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.tag.Tag;

import java.util.Collection;

public class SoulCampfireBlockHandler implements BlockHandler {

    private static final Key KEY = Key.key("minecraft", "soul_campfire");

    @Override
    public Key getKey() {
        return KEY;
    }

    @Override
    public Collection<Tag<?>> getBlockEntityTags() {
        return CampfireBlockHandler.TAGS;
    }
}
