package net.onelitefeather.stardust.listener

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.EnumWrappers
import net.onelitefeather.stardust.StardustPlugin

class PacketListener(private val stardustPlugin: StardustPlugin) {

    var protocolManager: ProtocolManager = ProtocolLibrary.getProtocolManager()

    fun unregister() {
        protocolManager.removePacketListeners(stardustPlugin)
    }

    fun register() {
        protocolManager.addPacketListener(object :
            PacketAdapter(stardustPlugin, ListenerPriority.HIGHEST, PacketType.Play.Server.PLAYER_INFO) {
            override fun onPacketSending(event: PacketEvent) {
                val packetContainer = event.packet
                if (packetContainer.playerInfoAction.size() == 0) return

                val playerInfoAction = packetContainer.playerInfoAction.read(0)
                if (playerInfoAction == EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME ||
                    playerInfoAction == EnumWrappers.PlayerInfoAction.UPDATE_GAME_MODE ||
                    playerInfoAction == EnumWrappers.PlayerInfoAction.UPDATE_LATENCY
                ) {

                    val playerInfoDataList = packetContainer.playerInfoDataLists.read(0)

                    playerInfoDataList.removeIf {
                        stardustPlugin.userService.getUser(it.profile.uuid)?.properties?.isVanished() == true
                    }

                    packetContainer.playerInfoDataLists.write(0, playerInfoDataList)
                }
            }
        })
    }

}