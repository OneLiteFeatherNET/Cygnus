package net.onelitefeather.cygnus.setup.util;

import net.minestom.server.tag.Tag;
import net.onelitefeather.cygnus.setup.map.MapDataCategory;

/**
 * The {@link SetupTags} class is a utility class that contains all the tags used by the setup system.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class SetupTags {

    public static final Tag<Integer> SETUP_ID_TAG = Tag.Integer("setup_id").defaultValue(-1);
    public static final Tag<MapDataCategory> MAP_DATA_CATEGORY_TAG = Tag.String("category_tag")
            .map(MapDataCategory::valueOf, MapDataCategory::name);

    private SetupTags() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
