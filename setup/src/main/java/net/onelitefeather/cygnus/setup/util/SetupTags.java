package net.onelitefeather.cygnus.setup.util;

import net.minestom.server.tag.Tag;

public final class SetupTags {

    public static final Tag<Byte> OCCUPIED_TAG = Tag.Byte("blocked");

    private SetupTags() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
