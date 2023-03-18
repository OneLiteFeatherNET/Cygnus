package net.onelitefeather.stardust.command.commands

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import cloud.commandframework.annotations.specifier.Quoted
import net.onelitefeather.stardust.StardustPlugin
import net.onelitefeather.stardust.extenstions.colorText
import net.onelitefeather.stardust.extenstions.coloredDisplayName
import net.onelitefeather.stardust.extenstions.miniMessage
import net.onelitefeather.stardust.util.DATE_FORMAT
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class SignCommand(private val stardustPlugin: StardustPlugin) {

    @CommandMethod("unsign")
    @CommandPermission("stardust.command.unsign")
    @CommandDescription("Remove your signature from a Item")
    fun execute(player: Player) {

        val itemStack = player.inventory.itemInMainHand
        if (!stardustPlugin.itemSignService.hasSigned(itemStack, player)) {
            player.sendMessage(miniMessage { stardustPlugin.i18nService.getMessage("commands.unsign.not-signed", stardustPlugin.i18nService.getPluginPrefix()) })
            return
        }

        giveItemStack(player, stardustPlugin.itemSignService.removeSignature(itemStack, player))
        player.sendMessage(miniMessage { stardustPlugin.i18nService.getMessage("commands.unsign.success", stardustPlugin.i18nService.getPluginPrefix()) })
    }

    @CommandMethod("sign <text>")
    @CommandPermission("stardust.command.sign")
    @CommandDescription("Signature the Item in your Hand.")
    fun handleCommand(player: Player, @Argument(value = "text") @Quoted text: String) {

        val itemStack = player.inventory.itemInMainHand
        if (itemStack.type == Material.AIR) {
            player.sendMessage(miniMessage {
                stardustPlugin.i18nService.getMessage(
                    "commands.sign.no-item-in-hand", *arrayOf(stardustPlugin.i18nService.getPluginPrefix())
                )
            })
            return
        }

        val signService = stardustPlugin.itemSignService
        if (signService.hasSigned(itemStack, player) && !player.hasPermission("stardust.command.sign.override")) {
            player.sendMessage(miniMessage {
                stardustPlugin.i18nService.getMessage(
                    "commands.sign.already-signed", *arrayOf(stardustPlugin.i18nService.getPluginPrefix())
                )
            })

            return
        }

        val message = miniMessage {
            stardustPlugin.i18nService.getMessage(
                "commands.sign.item-lore-message",
                *arrayOf(text.colorText(), player.coloredDisplayName(), DATE_FORMAT.format(System.currentTimeMillis()))
            )
        }

        giveItemStack(player, signService.sign(itemStack, listOf(message), player))

        player.sendMessage(miniMessage {
            stardustPlugin.i18nService.getMessage(
                "commands.sign.signed", *arrayOf(stardustPlugin.i18nService.getPluginPrefix())
            )
        })
    }

    private fun giveItemStack(player: Player, itemStack: ItemStack) {
        if (player.gameMode == GameMode.CREATIVE) {
            player.inventory.setItemInMainHand(itemStack)
        } else {
            if (player.inventory.firstEmpty() != -1) {
                player.inventory.setItem(player.inventory.firstEmpty(), itemStack)
            } else {
                player.sendMessage(miniMessage {
                    stardustPlugin.i18nService.getMessage(
                        "plugin.inventory-full",
                        *arrayOf(stardustPlugin.i18nService.getPluginPrefix())
                    )
                })
            }
        }
    }
}