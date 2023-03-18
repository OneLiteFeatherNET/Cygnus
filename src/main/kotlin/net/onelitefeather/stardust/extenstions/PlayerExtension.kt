package net.onelitefeather.stardust.extenstions

import io.sentry.Scope
import io.sentry.protocol.Browser
import io.sentry.protocol.User
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Mob
import org.bukkit.entity.Player

fun Player.removeEnemies(radius: Double) {
    this.location.getNearbyLivingEntities(radius).forEach { livingEntity ->
        if (livingEntity is Mob) {
            val target = livingEntity.target ?: return@forEach
            if (target == this) {
                livingEntity.target = null
            }
        }
    }
}

fun Player.coloredDisplayName(): String = MiniMessage.miniMessage().serialize(
    LegacyComponentSerializer.legacyAmpersand()
        .deserialize(LegacyComponentSerializer.legacyAmpersand().serialize(this.displayName()))
)

fun Player.toSentryUser() : User {
    val user = User()
    user.username = name
    user.id = uniqueId.toString()
    return user
}

fun Player.addClient(it: Scope) {

    val client = Browser().apply {
        name = player?.clientBrandName
        version = player?.protocolVersion.toString()
    }

    it.setContexts("browser", client)
}
