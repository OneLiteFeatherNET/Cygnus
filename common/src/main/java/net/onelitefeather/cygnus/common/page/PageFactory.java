package net.onelitefeather.cygnus.common.page;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.Direction;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * The factory should be used to create references from a {@link PageEntity}.
 * It's not possible to create a new instance from the entity class directly because the constructor of it is only visible to the package level.
 * @see PageEntity#PageEntity(Instance, Pos, int)
 * @author theEvilReaper
 * @since 1.0.0
 * @version 1.0.0
 */
public abstract class PageFactory {

    private PageFactory() {}

    /**
     * Creates a new reference from a {@link PageEntity} class.
     * @param instance the instance where the entity should be spawned
     * @param spawnPos the position to spawn
     * @param direction the direction for the spawning
     * @param pageCount the current count of the page
     * @return the created entity reference
     */
    @Contract(value = "_, _, _, _ -> new" , pure = true)
    public static @NotNull PageEntity createPage(
            @NotNull Instance instance,
            @NotNull Pos spawnPos,
            @NotNull Direction direction,
            int pageCount
    ) {
        Check.argCondition(direction == Direction.UP || direction == Direction.DOWN, "The direction " + direction + " is not supported");
        Check.argCondition(pageCount < 1, "The page count can't be zero or negative");
        return new PageEntity(instance, spawnPos, pageCount);
    }
}
