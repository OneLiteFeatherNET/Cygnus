package net.onelitefeather.cygnus.setup.util;

import net.minestom.server.tag.Tag;

public final class SetupTags {

    @Deprecated(forRemoval = true)
    public static final Tag<Byte> OCCUPIED_TAG = Tag.Byte("blocked");
    public static final Tag<Integer> SETUP_ID_TAG = Tag.Integer("setup_id").defaultValue(-1);

    private SetupTags() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
