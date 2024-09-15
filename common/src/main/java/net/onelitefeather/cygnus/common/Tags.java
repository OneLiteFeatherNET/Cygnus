package net.onelitefeather.cygnus.common;

import net.minestom.server.tag.Tag;

import java.util.UUID;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since
 **/

public final class Tags {

    public static final Tag<Byte> ITEM_TAG = Tag.Byte("itemTag");
    public static final Tag<UUID> PAGE_TAG = Tag.UUID("page");
    public static final Tag<Byte> OCCUPIED_TAG = Tag.Byte("blocked");
    public static final Tag<Byte> GAME_TAG = Tag.Byte("slender");
    public static final Tag<Byte> TEAM_ID = Tag.Byte("team");

    public static final Tag<Byte> HIDDEN = Tag.Byte("hidden");

    private Tags() {}
}
