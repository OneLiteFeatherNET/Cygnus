package net.onelitefeather.stardust.listener

import io.sentry.Sentry
import net.onelitefeather.stardust.StardustPlugin
import net.onelitefeather.stardust.extenstions.addClient
import net.onelitefeather.stardust.extenstions.miniMessage
import net.onelitefeather.stardust.extenstions.toSentryUser
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent

class CommandCooldownListener(private val stardustPlugin: StardustPlugin) : Listener {

    @EventHandler
    fun handlePlayerCommandPreprocess(event: PlayerCommandPreprocessEvent) {

        val player = event.player
        try {

            val commandRaw = event.message.replaceFirst("/", "")
            val strings = commandRaw.split(" ").dropLastWhile { it.isEmpty() }.toTypedArray()

            if(strings.isEmpty()) return

            val commandLabelRaw = strings[0]
            val commandLabel = if (commandLabelRaw.contains(":")) commandLabelRaw.split(":")[1] else commandLabelRaw

            if (stardustPlugin.commandCooldownService.hasCommandCooldown(commandLabel)) {

                if (player.hasPermission("stardust.commandcooldown.bypass") && stardustPlugin.config.getBoolean("settings.use-cooldown-bypass")) return
                val commandCooldown =
                    stardustPlugin.commandCooldownService.getCommandCooldown(player.uniqueId, commandLabel)

                if (commandCooldown != null && !commandCooldown.isOver()) {
                    player.sendMessage(miniMessage {
                        stardustPlugin.i18nService.getMessage(
                            "plugin.command-cooldowned",
                            stardustPlugin.i18nService.getPluginPrefix(),
                            stardustPlugin.i18nService.getRemainingTime(commandCooldown.executedAt)
                        )
                    })

                    event.isCancelled = true
                    return
                }

                val cooldownData = stardustPlugin.commandCooldownService.getCooldownData(commandLabel)
                if (cooldownData != null) {
                    stardustPlugin.commandCooldownService.addCommandCooldown(
                        player.uniqueId,
                        cooldownData.commandName,
                        cooldownData.timeUnit,
                        cooldownData.time
                    )
                }
            }
        } catch (e: Exception) {
            Sentry.captureException(e) {
                it.user = player.toSentryUser()
                player.addClient(it)
            }
        }
    }
}