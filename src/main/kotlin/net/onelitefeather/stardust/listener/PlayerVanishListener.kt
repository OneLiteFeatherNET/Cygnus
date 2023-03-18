package net.onelitefeather.stardust.listener

import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent
import io.sentry.Sentry
import net.kyori.adventure.text.Component
import net.onelitefeather.stardust.StardustPlugin
import net.onelitefeather.stardust.extenstions.addClient
import net.onelitefeather.stardust.extenstions.toSentryUser
import net.onelitefeather.stardust.user.User
import net.onelitefeather.stardust.user.UserPropertyType
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.*
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.permissions.Permissible

class PlayerVanishListener(private val stardustPlugin: StardustPlugin) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onEntityDamageByBlock(event: EntityDamageByBlockEvent) {

        val target = event.entity
        val attacker = event.damager
        val targetUser = stardustPlugin.userService.getUser(target.uniqueId)
        if (attacker is Permissible) {
            event.isCancelled = if (targetUser != null && targetUser.properties.isVanished()) {
                !attacker.hasPermission("stardust.bypass.damage.vanish")
            } else if (targetUser != null && target.isInvulnerable) {
                !attacker.hasPermission("stardust.bypass.damage.invulnerable")
            } else {
                false
            }
        }
    }

    @EventHandler
    fun onTarget(event: EntityTargetEvent) {

        var user: User? = null
        var entity: Entity? = null

        if (event.entity is Player) {
            user = stardustPlugin.userService.getUser(event.entity.uniqueId)
            entity = event.entity
        } else if (event.target is Player) {
            user = stardustPlugin.userService.getUser((event.target as Player).uniqueId)
            entity = event.target
        }

        if (user != null && user.properties.isVanished() || entity != null && entity.isInvulnerable) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        if (event.entity is Player) {
            val player = event.entity as Player
            try {
                val user = stardustPlugin.userService.getUser(player.uniqueId)
                event.isCancelled = user != null && (user.properties.isVanished() || player.isInvulnerable)
            } catch (e: Exception) {
                Sentry.captureException(e) {
                    it.user = player.toSentryUser()
                    player.addClient(it)
                }
            }
        }
    }

    @EventHandler
    fun onPickUp(event: EntityPickupItemEvent) {
        if (event.entity is Player) {
            val player = event.entity as Player
            try {
                val user = stardustPlugin.userService.getUser(player.uniqueId) ?: return
                event.isCancelled =
                    user.properties.isVanished() && user.properties.getProperty(UserPropertyType.VANISH_DISABLE_ITEM_COLLECT)
                        .getValue<Boolean>() == true

            } catch (e: Exception) {
                Sentry.captureException(e) {
                    it.user = player.toSentryUser()
                    player.addClient(it)
                }
            }
        }
    }

    @EventHandler
    fun onDrop(event: PlayerDropItemEvent) {
        val player = event.player
        try {
            val user = stardustPlugin.userService.getUser(player.uniqueId) ?: return
            event.isCancelled =
                user.properties.isVanished() && user.properties.getProperty(UserPropertyType.VANISH_DISABLE_ITEM_DROP)
                    .getValue<Boolean>() == true
        } catch (e: Exception) {
            Sentry.captureException(e) {
                it.user = player.toSentryUser()
                player.addClient(it)
            }
        }
    }

    @EventHandler
    fun onPlayerPickupExp(event: PlayerPickupExperienceEvent) {
        val player = event.player
        val user = stardustPlugin.userService.getUser(player.uniqueId) ?: return
        try {
            event.isCancelled =
                user.properties.isVanished() && user.properties.getProperty(UserPropertyType.VANISH_DISABLE_ITEM_COLLECT)
                    .getValue<Boolean>() == true
        } catch (e: Exception) {
            Sentry.captureException(e) {
                it.user = player.toSentryUser()
                player.addClient(it)
            }
        }
    }

    @EventHandler
    fun onPlayerDeath(event: PlayerDeathEvent) {
        val player = event.entity
        try {
            val user = stardustPlugin.userService.getUser(player.uniqueId) ?: return
            if (user.properties.isVanished()) {
                event.drops.clear()
                event.deathMessage(Component.text(""))
                event.keepInventory = true
                event.keepLevel = true
                event.setShouldDropExperience(false)
                event.setShouldPlayDeathSound(false)
            }
        } catch (e: Exception) {
            Sentry.captureException(e) {
                it.user = player.toSentryUser()
                player.addClient(it)
            }
        }
    }
}