package net.onelitefeather.cygnus.game.data;

import net.minestom.server.coordinate.Point;
import net.onelitefeather.cygnus.game.PageEntity;
import net.onelitefeather.cygnus.phase.GamePhase;
import org.jetbrains.annotations.NotNull;

/**
 * The {@link PageResource} class represents a data structure used for spawning a {@link PageEntity}
 * during the {@link GamePhase}. It includes a string variable that represents the face used in the setup process.
 * When spawning a {@link PageEntity}, it uses the opposite face from the provided face.
 *
 * @param position The position at which to spawn a page.
 * @param face The face used in the resource setup process.
 */
public record PageResource(@NotNull Point position, @NotNull String face) { }