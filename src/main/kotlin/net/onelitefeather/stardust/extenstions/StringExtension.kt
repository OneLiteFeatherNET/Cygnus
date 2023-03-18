package net.onelitefeather.stardust.extenstions;

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer


fun miniMessage(message: () -> String) = MiniMessage.miniMessage().deserialize(message())

fun String.colorText(): String = convertComponentToString(miniMessage { this })

fun convertComponentToString(message: Component): String {
    return MiniMessage.miniMessage().serialize(
        LegacyComponentSerializer.legacyAmpersand()
            .deserialize(LegacyComponentSerializer.legacyAmpersand().serialize(message))
    )
}