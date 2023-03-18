package net.onelitefeather.stardust.command.commands

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import cloud.commandframework.annotations.specifier.Greedy
import io.sentry.Sentry
import net.onelitefeather.stardust.StardustPlugin
import net.onelitefeather.stardust.extenstions.*
import net.onelitefeather.stardust.util.RADIUS_REMOVE_ENEMIES
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class GodmodeCommand(private val stardustPlugin: StardustPlugin) {

    @CommandMethod("godmode [player]")
    @CommandPermission("stardust.command.godmode")
    @CommandDescription("Makes a player invulnerable to everything")
    fun handleCommand(commandSender: CommandSender, @Greedy @Argument(value = "player") target: Player?) {
        handleInvulnerability(commandSender, target ?: commandSender as Player)
    }

    private fun handleInvulnerability(commandSender: CommandSender, target: Player) {

        try {
            if (target != commandSender && !commandSender.hasPermission("stardust.command.godmode.others")) {
                commandSender.sendMessage(miniMessage {
                    stardustPlugin.i18nService.getMessage(
                        "plugin.not-enough-permissions", stardustPlugin.i18nService.getPluginPrefix()
                    )
                })
                return
            }

            target.isInvulnerable = !target.isInvulnerable
            target.removeEnemies(RADIUS_REMOVE_ENEMIES)

            val enabledMessage = stardustPlugin.i18nService.getMessage(
                "commands.god-mode.enable", stardustPlugin.i18nService.getPluginPrefix(), target.coloredDisplayName()
            )
            val disabledMessage = stardustPlugin.i18nService.getMessage(
                "commands.god-mode.disable", stardustPlugin.i18nService.getPluginPrefix(), target.coloredDisplayName()
            )

            target.sendMessage(miniMessage { if (target.isInvulnerable) enabledMessage else disabledMessage })
            if (commandSender != target) {
                commandSender.sendMessage(miniMessage { if (target.isInvulnerable) enabledMessage else disabledMessage })
            }
        } catch (e: Exception) {
            Sentry.captureException(e) {
                it.user = target.toSentryUser()
                target.addClient(it)
            }
        }
    }
}