package net.onelitefeather.stardust.command.commands

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import cloud.commandframework.annotations.specifier.Quoted
import net.onelitefeather.stardust.StardustPlugin
import net.onelitefeather.stardust.extenstions.colorText
import net.onelitefeather.stardust.extenstions.miniMessage
import org.bukkit.Material
import org.bukkit.entity.Player

class RenameCommand(private val stardustPlugin: StardustPlugin) {

    @CommandMethod("itemrename|rename <text>")
    @CommandPermission("stardust.command.rename")
    @CommandDescription("Rename a Item")
    fun handleCommand(player: Player, @Argument(value = "text") @Quoted text: String) {

        val itemInHand = player.inventory.itemInMainHand
        if (itemInHand.type == Material.AIR) {
            player.sendMessage(miniMessage {
                stardustPlugin.i18nService.getMessage(
                    "commands.rename.invalid-item",
                    *arrayOf(stardustPlugin.i18nService.getPluginPrefix())
                )
            })
            return
        }

        val itemMeta = itemInHand.itemMeta
        itemMeta.displayName(miniMessage { text.colorText() })
        itemInHand.itemMeta = itemMeta
        player.updateInventory()
        player.sendMessage(miniMessage {
            stardustPlugin.i18nService.getMessage(
                "commands.rename.success",
                *arrayOf(
                    stardustPlugin.i18nService.getPluginPrefix(),
                    text.colorText()
                )
            )
        })
    }
}