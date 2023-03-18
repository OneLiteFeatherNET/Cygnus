package net.onelitefeather.stardust.command.commands

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import cloud.commandframework.annotations.specifier.Greedy
import io.sentry.Sentry
import net.onelitefeather.stardust.StardustPlugin
import net.onelitefeather.stardust.extenstions.addClient
import net.onelitefeather.stardust.extenstions.coloredDisplayName
import net.onelitefeather.stardust.extenstions.miniMessage
import net.onelitefeather.stardust.extenstions.toSentryUser
import net.onelitefeather.stardust.util.DEFAULT_ENTITY_HAS_VISUAL_FIRE
import net.onelitefeather.stardust.util.DEFAULT_PLAYER_FIRE_TICKS
import net.onelitefeather.stardust.util.DEFAULT_PLAYER_FOOD_LEVEL
import net.onelitefeather.stardust.util.DEFAULT_PLAYER_SATURATION_LEVEL
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class HealCommand(private val stardustPlugin: StardustPlugin) {

    @CommandMethod("heal [player]")
    @CommandPermission("stardust.command.heal")
    @CommandDescription("Heal a player.")
    fun onCommand(commandSender: CommandSender, @Greedy @Argument(value = "player") target: Player?) {
        if (commandSender is Player) {
            healPlayer(commandSender, target ?: commandSender)
        } else {
            if (target == null) return
            healPlayer(commandSender, target)
        }
    }

    private fun healPlayer(commandSender: CommandSender, target: Player) {

        try {
            if (target != commandSender && !commandSender.hasPermission("stardust.command.heal.others")) {
                commandSender.sendMessage(miniMessage {
                    stardustPlugin.i18nService.getMessage(
                        "plugin.not-enough-permissions", *arrayOf(stardustPlugin.i18nService.getPluginPrefix())
                    )
                })

                return
            }

            val healthAttribute: AttributeInstance? = target.getAttribute(Attribute.GENERIC_MAX_HEALTH)
            if (healthAttribute != null) {
                target.health = healthAttribute.value
            }

            target.fireTicks = DEFAULT_PLAYER_FIRE_TICKS
            target.isVisualFire = DEFAULT_ENTITY_HAS_VISUAL_FIRE
            target.foodLevel = DEFAULT_PLAYER_FOOD_LEVEL
            target.saturation = DEFAULT_PLAYER_SATURATION_LEVEL

            val message = this.stardustPlugin.i18nService.getMessage(
                "commands.heal.success",
                stardustPlugin.i18nService.getPluginPrefix(),
                target.coloredDisplayName(),
                target.health
            )

            if (commandSender != target) {
                target.sendMessage(miniMessage { message })
            }

            commandSender.sendMessage(miniMessage { message })
        } catch (e: Exception) {
            Sentry.captureException(e) {
                it.user = target.toSentryUser()
                target.addClient(it)
            }
        }
    }
}