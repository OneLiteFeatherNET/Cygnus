package net.onelitefeather.stardust.service

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.onelitefeather.stardust.StardustPlugin
import net.onelitefeather.stardust.api.ItemSignService
import net.onelitefeather.stardust.user.User
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType


class BukkitItemSignService(private val stardustPlugin: StardustPlugin) :
    ItemSignService<ItemStack, Player> {

    override fun sign(baseItemStack: ItemStack, lore: List<Component>, player: Player): ItemStack {
        return getItemStack(baseItemStack, player, lore, true)
    }

    override fun hasSigned(itemStack: ItemStack, player: Player): Boolean {
        val itemMeta = itemStack.itemMeta ?: return false
        val container = itemMeta.persistentDataContainer
        if (!container.has(stardustPlugin.signedNameSpacedKey, PersistentDataType.LONG_ARRAY)) return false
        val array = container[stardustPlugin.signedNameSpacedKey, PersistentDataType.LONG_ARRAY]
        return array != null && array.any { it == stardustPlugin.userService.getUser(player.uniqueId)?.id }
    }

    private fun getItemStack(base: ItemStack, player: Player, lore: List<Component>, sign: Boolean): ItemStack {

        val isInCreative = player.gameMode == GameMode.CREATIVE
        if (!isInCreative) {
            if (base.amount > 1) {
                base.amount = base.amount - 1
            } else {
                player.inventory.remove(base)
            }
        }

        val user = stardustPlugin.userService.getUser(player.uniqueId) ?: return base
        val itemStack = buildSignedUsers(player, base, sign, user)
        val itemMeta = itemStack.itemMeta

        itemMeta.lore(buildLore(player, base, sign, lore))
        itemStack.itemMeta = itemMeta

        return if (!isInCreative) itemStack.asOne() else itemStack
    }

    override fun removeSignature(baseItemStack: ItemStack, player: Player): ItemStack {
        return getItemStack(baseItemStack, player, baseItemStack.lore() ?: emptyList(), false)
    }

    private fun buildSignedUsers(player: Player, itemStack: ItemStack, sign: Boolean, user: User): ItemStack {

        val itemMeta = itemStack.itemMeta

        if (sign) {
            if (!hasSigned(itemStack, player)) {
                itemMeta.persistentDataContainer[stardustPlugin.signedNameSpacedKey, PersistentDataType.LONG_ARRAY] =
                    addPlayerSign(itemStack, user)
            }
        } else {
            itemMeta.persistentDataContainer[stardustPlugin.signedNameSpacedKey, PersistentDataType.LONG_ARRAY] =
                removePlayerSign(itemStack, user)
        }

        itemStack.itemMeta = itemMeta
        return itemStack
    }

    private fun buildLore(
        player: Player,
        itemStack: ItemStack,
        sign: Boolean,
        lore: List<Component>
    ): MutableList<Component> {

        val currentLore = itemStack.lore()

        return if(currentLore == null) {
            lore.toMutableList()
        } else {
            if(sign) {
                currentLore.plus(lore).toMutableList()
            } else {
                removePlayerFromLore(player, currentLore)
            }
        }
    }

    private fun removePlayerFromLore(player: Player, lore: MutableList<Component>): MutableList<Component> {
        return lore.filterNot {
            val text = PlainTextComponentSerializer.plainText().serialize(it)
            text.contains(player.name) && stardustPlugin.server.getPlayerUniqueId(player.name) == player.uniqueId
        }.toMutableList()
    }

    private fun removePlayerSign(itemStack: ItemStack, user: User): LongArray {
        val players = getSignedPlayers(itemStack) ?: longArrayOf(user.id!!)
        return players.toMutableList().filterNot { it == user.id }.toLongArray()
    }

    private fun addPlayerSign(itemStack: ItemStack, user: User): LongArray {
        return getSignedPlayers(itemStack)?.plus(user.id!!) ?: longArrayOf(user.id!!)
    }

    private fun getSignedPlayers(itemStack: ItemStack): LongArray? {
        val itemMeta = itemStack.itemMeta ?: return null
        val container = itemMeta.persistentDataContainer
        if (!container.has(stardustPlugin.signedNameSpacedKey, PersistentDataType.LONG_ARRAY)) return null
        return container[stardustPlugin.signedNameSpacedKey, PersistentDataType.LONG_ARRAY]
    }
}