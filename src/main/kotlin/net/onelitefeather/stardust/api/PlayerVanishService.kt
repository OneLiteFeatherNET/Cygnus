package net.onelitefeather.stardust.api

import net.onelitefeather.stardust.user.User

interface PlayerVanishService<P> {

    fun hidePlayer(player: P)

    fun showPlayer(player: P)

    fun toggle(player: P): Boolean

    fun isVanished(player: P): Boolean

    fun setVanished(user: User, vanished: Boolean)
    fun onPlayerJoin(player: P)
}
