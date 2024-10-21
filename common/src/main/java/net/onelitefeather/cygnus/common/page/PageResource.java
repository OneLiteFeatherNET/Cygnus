package net.onelitefeather.cygnus.common.page;

import net.minestom.server.coordinate.Point;
import net.minestom.server.utils.Direction;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link PageResource} class represents a data structure used for spawning a page entity
 * during the GamePhase. It includes a string variable that represents the face used in the setup process.
 * When spawning a page entity, it uses the opposite face from the provided face.
 *
 * @param position The position at which to spawn a page.
 * @param face The face used in the resource setup process.
 */
public record PageResource(@NotNull Point position, @NotNull Direction face) { }