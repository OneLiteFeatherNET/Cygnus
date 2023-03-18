package net.onelitefeather.stardust.command.commands

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import cloud.commandframework.annotations.specifier.Greedy
import net.onelitefeather.stardust.StardustPlugin
import org.bukkit.command.CommandSender

class HelpCommand(private val stardustPlugin: StardustPlugin) {

    @CommandDescription("Shows the help menu")
    @CommandMethod("stardust help [query]")
    @CommandPermission("stardust.command.help")
    private fun helpCommand(sender: CommandSender, @Argument("query") @Greedy query: String?) {
        stardustPlugin.minecraftHelp.queryCommands(query ?: "", sender)
    }
}