package net.onelitefeather.cygnus.stats;

import java.util.UUID;

/**
 * A player the statistics know: the UUID and the last name they were seen with.
 *
 * @param uuid the player
 * @param name the last known name, spelled as the player wrote it
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.18.0
 */
public record KnownPlayer(UUID uuid, String name) {
}
