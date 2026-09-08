package net.onelitefeather.cygnus.common;

import net.kyori.adventure.key.Key;
import net.minestom.server.tag.Tag;
import net.onelitefeather.cygnus.common.rank.RankTag;

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
    public static final Tag<RankTag> ACTIVE_RANK_TAG = Tag.Transient("activeRankTag");

    private Tags() {
        // Nothing do to here
    }
}
