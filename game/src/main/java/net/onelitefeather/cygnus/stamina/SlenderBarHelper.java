package net.onelitefeather.cygnus.stamina;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;

/**
 * The StaminaDamage interface is designed to provide a method to apply damage to entities in a given range.
 * The method will iterate over all entities in the given range and apply the damage to the entity if it is a player.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
interface SlenderBarHelper {

    /**
     * Applies the damage to all entities in the given range.
     *
     * @param instance the instance where the entities are located
     * @param uuid     the uuid of the player who caused the damage
     * @param center   the center position where the damage should be applied
     * @param range    the range where the damage should be applied
     * @param damage   the damage that should be applied
     */
    default void applyDamage(@NotNull Instance instance, @NotNull UUID uuid, @NotNull Pos center, int range, float damage) {
        Collection<Entity> nearbyEntities = instance.getNearbyEntities(center, range);
        if (nearbyEntities.isEmpty()) return;
        for (Entity nearbyEntity : nearbyEntities) {
            if (nearbyEntity instanceof Player target && !uuid.equals(target.getUuid()) && (target.getHealth() > 0)) {
                target.setHealth(target.getHealth() - damage);
            }
        }
    }
}
