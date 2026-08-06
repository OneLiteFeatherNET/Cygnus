package net.onelitefeather.cygnus.common;

import net.kyori.adventure.key.Key;
import net.minestom.server.tag.Tag;

import java.util.UUID;

/**
 * The {@link Tags} class contains all tags used in the project. It is a utility class and should not be instantiated.
 *
 * @author theEvilReaper
 * @version 1.1.0
 * @since 1.0.0
 **/
public final class Tags {

    public static final Tag<UUID> PAGE_TAG = Tag.UUID("page");
    public static final Tag<Byte> ITEM_TAG = Tag.Byte("itemTag");
    public static final Tag<Key> TEAM_KEY = Tag.Transient("teamKey");
    public static final Tag<Byte> HIDDEN = Tag.Byte("hidden");

    private Tags() {
        // Nothing do to here
    }
}
