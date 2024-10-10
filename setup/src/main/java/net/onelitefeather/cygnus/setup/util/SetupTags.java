package net.onelitefeather.cygnus.setup.util;

import net.minestom.server.tag.Tag;

/**
 * The {@link SetupTags} class is a utility class that contains all the tags used by the setup system.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class SetupTags {

    public static final Tag<Integer> SETUP_ID_TAG = Tag.Integer("setup_id");
    public static final Tag<Boolean> DELETE_TAG = Tag.Boolean("delete").defaultValue(false);

    private SetupTags() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
