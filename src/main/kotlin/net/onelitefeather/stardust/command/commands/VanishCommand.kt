package net.onelitefeather.stardust.command.commands

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import cloud.commandframework.annotations.specifier.Greedy
import com.google.common.base.Preconditions
import io.sentry.Sentry
import net.onelitefeather.stardust.StardustPlugin
import net.onelitefeather.stardust.extenstions.addClient
import net.onelitefeather.stardust.extenstions.miniMessage
import net.onelitefeather.stardust.extenstions.toSentryUser
import net.onelitefeather.stardust.user.UserPropertyType
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@Suppress("unused")
class VanishCommand(private val stardustPlugin: StardustPlugin) {

    @CommandMethod("vanish|v nodrop [player]")
    @CommandPermission("stardust.command.vanish.nodrop")
    @CommandDescription("Disable the ability to drop items")
    fun commandVanishNoDrop(
        commandSender: CommandSender,
        @Greedy @Argument(value = "player") target: Player?
    ) {

        if (target == null) {
            if (commandSender is Player) {
                toggleProperty(commandSender, commandSender, UserPropertyType.VANISH_DISABLE_ITEM_DROP)
            }
            return
        }

        toggleProperty(commandSender, target, UserPropertyType.VANISH_DISABLE_ITEM_DROP)
    }

    @CommandMethod("vanish|v noCollect [player]")
    @CommandPermission("stardust.command.vanish.nocollect")
    @CommandDescription("Disable the ability to collect items")
    fun commandVanishNoCollect(
        commandSender: CommandSender,
        @Greedy @Argument(value = "player") target: Player?
    ) {

        if (target == null) {
            if (commandSender is Player) {
                toggleProperty(commandSender, commandSender, UserPropertyType.VANISH_DISABLE_ITEM_COLLECT)
            }
            return
        }

        toggleProperty(commandSender, target, UserPropertyType.VANISH_DISABLE_ITEM_COLLECT)
    }

    @CommandMethod("vanish|v [player]")
    @CommandPermission("stardust.command.vanish")
    @CommandDescription("Make a player invisible for other players")
    fun handleCommand(commandSender: CommandSender, @Greedy @Argument(value = "player") target: Player?) {
        if (target == null) {
            toggleVanish(commandSender, commandSender as Player)
        } else {
            toggleVanish(commandSender, target)
        }
    }

    fun toggleProperty(commandSender: CommandSender, target: Player, propertyType: UserPropertyType) {

        Preconditions.checkArgument(
            propertyType == UserPropertyType.VANISH_DISABLE_ITEM_COLLECT || propertyType == UserPropertyType.VANISH_DISABLE_ITEM_DROP,
            "Invalid UserProperty type"
        )

        val user = stardustPlugin.userService.getUser(target.uniqueId) ?: return
        val property = stardustPlugin.userService.getUserProperty(user.properties, propertyType)
        val currentValue = property.getValue<Boolean>() ?: return

        stardustPlugin.userService.setUserProperty(user, propertyType, !currentValue)
        commandSender.sendMessage(miniMessage {
            stardustPlugin.i18nService.getMessage(
                "commands.vanish.property-set",
                *arrayOf(
                    stardustPlugin.i18nService.getPluginPrefix(),
                    propertyType.friendlyName,
                    !currentValue,
                    target.name
                )
            )
        })
    }

    fun toggleVanish(commandSender: CommandSender, target: Player) {

        try {
            if (target != commandSender && !commandSender.hasPermission("stardust.command.vanish.others")) {
                commandSender.sendMessage(miniMessage {
                    stardustPlugin.i18nService.getMessage(
                        "plugin.not-enough-permissions",
                        *arrayOf(stardustPlugin.i18nService.getPluginPrefix())
                    )
                })
                return
            }

            val user = stardustPlugin.userService.getUser(target.uniqueId)
            if (user != null) {

                val state = stardustPlugin.userService.playerVanishService.toggle(target)
                val targetEnable = stardustPlugin.i18nService.getMessage(
                    "commands.vanish.enable",
                    *arrayOf(stardustPlugin.i18nService.getPluginPrefix(), user.getDisplayName())
                )
                val targetDisable = stardustPlugin.i18nService.getMessage(
                    "commands.vanish.disable",
                    *arrayOf(stardustPlugin.i18nService.getPluginPrefix(), user.getDisplayName())
                )

                if (commandSender != target) {
                    commandSender.sendMessage(miniMessage { if (state) targetEnable else targetDisable })
                }

                target.sendMessage(miniMessage { if (state) targetEnable else targetDisable })
            }
        } catch (e: Exception) {
            Sentry.captureException(e) {
                it.user = target.toSentryUser()
                target.addClient(it)
            }
        }
    }
}