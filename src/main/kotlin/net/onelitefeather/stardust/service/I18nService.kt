package net.onelitefeather.stardust.service

import net.kyori.adventure.util.UTF8ResourceBundleControl
import net.onelitefeather.stardust.StardustPlugin
import net.onelitefeather.stardust.util.NOT_AVAILABLE_CONFIG_FALLBACK
import java.text.MessageFormat
import java.util.*
import kotlin.math.abs

class I18nService(val stardustPlugin: StardustPlugin) {

    val defaultMessages: ResourceBundle = ResourceBundle.getBundle("stardust", Locale.US, UTF8ResourceBundleControl())

    fun getPluginPrefix(): String {
        return MessageFormat.format(defaultMessages.getString("plugin.prefix"), stardustPlugin.name)
    }

    fun getMessage(key: String, vararg variables: Any): String {
        return if (defaultMessages.containsKey(key)) MessageFormat(defaultMessages.getString(key)).format(variables)
        else NOT_AVAILABLE_CONFIG_FALLBACK.format(key)
    }

    fun getRemainingTime(time: Long): String {
        val diff = abs(time - System.currentTimeMillis())
        val seconds = diff / 1000 % 60
        val minutes = diff / (1000 * 60) % 60
        val hours = diff / (1000 * 60 * 60) % 24
        val days = diff / (1000 * 60 * 60 * 24)
        val remainingTime = if (days > 0) {
            getMessage("remaining-time.days", days, hours, minutes, seconds)
        } else if (hours > 0) {
            getMessage("remaining-time.hours", hours, minutes, seconds)
        } else if (minutes > 0) {
            getMessage("remaining-time.minutes", minutes, seconds)
        } else {
            getMessage("remaining-time.seconds", seconds)
        }
        return remainingTime
    }
}