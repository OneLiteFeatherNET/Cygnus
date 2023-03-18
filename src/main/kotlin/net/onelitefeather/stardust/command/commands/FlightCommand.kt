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
import net.onelitefeather.stardust.user.UserPropertyType
import org.bukkit.GameMode
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.lang.Exception

class FlightCommand(val stardustPlugin: StardustPlugin) {

    @CommandMethod("flight|fly [player]")
    @CommandPermission("stardust.command.flight")
    @CommandDescription("Allows a player to flight.")
    fun handleFlightCommand(commandSender: CommandSender, @Greedy @Argument(value = "player") target: Player?) {

        if (commandSender is Player && target == null) {
            handleFlight(commandSender, commandSender)
            return
        }

        if (target != null) {
            handleFlight(commandSender, target)
        }
    }

    private fun handleFlight(commandSender: CommandSender, target: Player) {

        try {
            val user = stardustPlugin.userService.getUser(target.uniqueId)!!

            val enabledMessage = stardustPlugin.i18nService.getMessage(
                "commands.flight.enable",
                *arrayOf(stardustPlugin.i18nService.getPluginPrefix(), target.coloredDisplayName())
            )

            val disabledMessage = stardustPlugin.i18nService.getMessage(
                "commands.flight.disable",
                *arrayOf(stardustPlugin.i18nService.getPluginPrefix(), target.coloredDisplayName())
            )

            if (commandSender != target && !commandSender.hasPermission("stardust.command.flight.others")) {
                commandSender.sendMessage(miniMessage {
                    this.stardustPlugin.i18nService.getMessage(
                        "plugin.not-enough-permissions",
                        *arrayOf(stardustPlugin.i18nService.getPluginPrefix())
                    )
                })
                return
            }

            if (target.gameMode == GameMode.CREATIVE) {
                commandSender.sendMessage(miniMessage {
                    stardustPlugin.i18nService.getMessage(
                        "commands.flight.already-in-creative",
                        *arrayOf(
                            stardustPlugin.i18nService.getPluginPrefix(),
                            target.coloredDisplayName()
                        )
                    )
                })
                return
            }

            target.allowFlight = !target.allowFlight
            stardustPlugin.userService.setUserProperty(user, UserPropertyType.FLYING, target.allowFlight)
            commandSender.sendMessage(miniMessage { if (target.allowFlight) enabledMessage else disabledMessage })

            if (commandSender != target) {
                target.sendMessage(miniMessage { if (target.allowFlight) enabledMessage else disabledMessage })
            }
        } catch (e: Exception) {
            Sentry.captureException(e) {
                it.user = target.toSentryUser()
                target.addClient(it)
            }
        }
    }
}