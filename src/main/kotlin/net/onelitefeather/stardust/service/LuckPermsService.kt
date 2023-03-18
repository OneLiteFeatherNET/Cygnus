package net.onelitefeather.stardust.service

import io.sentry.Sentry
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.luckperms.api.LuckPerms
import net.luckperms.api.model.group.Group
import net.luckperms.api.query.QueryOptions
import net.onelitefeather.stardust.StardustPlugin
import org.bukkit.entity.Player
import java.util.logging.Level


class LuckPermsService(val stardustPlugin: StardustPlugin) {

    lateinit var luckPerms: LuckPerms
    fun init() {
        try {
            val server = stardustPlugin.server
            if (server.pluginManager.isPluginEnabled("LuckPerms")) {
                val provider = server.servicesManager.getRegistration(LuckPerms::class.java)
                if (provider != null) {
                    luckPerms = provider.provider
                    stardustPlugin.logger.log(Level.INFO, "Using ${provider.plugin.name} as Permission provider.")
                }
            }
        } catch (e: Exception) {
            Sentry.captureException(e)
        }
    }

    fun isEnabled(): Boolean = this::luckPerms.isInitialized

    fun getPlayerDisplayName(player: Player): Component {
        return MiniMessage.miniMessage().deserialize(getPlayerGroupPrefix(player).plus(" ${player.name}"))
    }

    fun getGroupPriority(groupName: String): Int {
        val group = luckPerms.groupManager.getGroup(groupName) ?: return 0
        return group.weight.orElse(0)
    }

    fun getPrimaryGroup(player: Player): Group {
        val user = luckPerms.userManager.getUser(player.uniqueId) ?: return getDefaultGroup()
        return luckPerms.groupManager.getGroup(user.primaryGroup) ?: getDefaultGroup()
    }

    fun getGroupPriority(player: Player): Int {
        if (!isEnabled()) return 0
        return getPrimaryGroup(player).weight.orElse(0)
    }

    fun getDefaultGroup(): Group = luckPerms.groupManager.getGroup("default")!!

    fun getPlayerGroupPrefix(player: Player): String {
        val metaData = luckPerms.getPlayerAdapter(Player::class.java).getMetaData(player)
        return metaData.prefix
            ?: luckPerms.groupManager.getGroup("default")?.cachedData?.getMetaData(QueryOptions.nonContextual())?.prefix
            ?: ""
    }

    fun getPlayerGroupSuffix(player: Player): String {
        val metaData = luckPerms.getPlayerAdapter(Player::class.java).getMetaData(player)
        return metaData.suffix
            ?: luckPerms.groupManager.getGroup("default")?.cachedData?.getMetaData(QueryOptions.nonContextual())?.suffix
            ?: ""
    }

    fun getGroupSortId(group: Group): Int {
        return luckPerms.groupManager.loadedGroups.sortedByDescending {
            getGroupPriority(it.name)
        }.map { it.name }.indexOfFirst { it.equals(group.name, true) }
    }
}