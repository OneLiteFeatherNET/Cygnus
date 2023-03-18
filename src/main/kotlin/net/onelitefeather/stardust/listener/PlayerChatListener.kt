package net.onelitefeather.stardust.listener

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.onelitefeather.stardust.StardustPlugin
import net.onelitefeather.stardust.extenstions.miniMessage
import net.onelitefeather.stardust.user.UserPropertyType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class PlayerChatListener(private val stardustPlugin: StardustPlugin) : Listener {

    @EventHandler
    fun onAsyncChat(event: AsyncChatEvent) {
        val player = event.player
        val user = stardustPlugin.userService.getUser(player.uniqueId)

        event.renderer { _: Player, sourceDisplayName: Component, _: Component, _: Audience ->
            Component.text()
                .append(sourceDisplayName)
                .append(Component.text(": "))
                .append(event.message())
                .build()
        }

        if (user != null && user.properties.getProperty(UserPropertyType.VANISHED).getValue<Boolean>() == true) {

            if (!user.hasChatConfirmation(stardustPlugin.chatConfirmationKey)) {
                user.confirmChatMessage(stardustPlugin.chatConfirmationKey, true)
                event.isCancelled = true
                player.sendMessage(miniMessage { stardustPlugin.i18nService.getMessage("vanish.confirm-chat-message", stardustPlugin.i18nService.getPluginPrefix()) })
            } else {
                user.confirmChatMessage(stardustPlugin.chatConfirmationKey, false)
            }

            return
        }
    }
}