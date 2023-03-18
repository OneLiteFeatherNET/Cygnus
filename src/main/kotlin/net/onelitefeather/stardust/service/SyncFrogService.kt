package net.onelitefeather.stardust.service

import cloud.commandframework.annotations.Argument
import cloud.commandframework.annotations.CommandDescription
import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import cloud.commandframework.annotations.specifier.Quoted
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent
import net.kyori.adventure.text.Component
import net.onelitefeather.stardust.StardustPlugin
import net.onelitefeather.stardust.extenstions.colorText
import net.onelitefeather.stardust.extenstions.coloredDisplayName
import net.onelitefeather.stardust.extenstions.convertComponentToString
import net.onelitefeather.stardust.extenstions.miniMessage
import org.bukkit.*
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Frog
import org.bukkit.entity.Frog.Variant
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class SyncFrogService(val stardustPlugin: StardustPlugin) : Listener {

    private var frogNameSpacedKey: NamespacedKey = NamespacedKey(stardustPlugin, "frog_data_key")
    private var frogVariantKey: NamespacedKey = NamespacedKey(stardustPlugin, "frog_variant_key")
    private var frogCustomNameKey: NamespacedKey = NamespacedKey(stardustPlugin, "frog_custom_name_key")
    private var frogInBucket: Int = 1
    private var frogBucketName: String = "Frog Bucket"

    init {
        stardustPlugin.server.pluginManager.registerEvents(this, stardustPlugin)
    }

    @CommandMethod("frogbucket <customName> <variant> <amount>")
    @CommandDescription("Gives you a Frog bucket with the given amount!")
    @CommandPermission("stardust.command.frogbucket")
    fun executeCommand(
        player: Player,
        @Quoted @Argument(value = "customName") customName: String,
        @Argument(value = "variant") variant: Variant,
        @Argument(value = "amount") amount: Int
    ) {
        addFrogBucketToPlayer(player, amount, miniMessage { customName }, variant, false)
    }

    @EventHandler
    fun handlePlayerInteract(event: PlayerInteractEvent) {

        val player = event.player
        val itemStack = event.item ?: return
        val clickedBlock = event.clickedBlock ?: return

        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        if (!isFrogInBucket(itemStack)) return
        if (deserializeFrogData(itemStack, clickedBlock.location.add(0.0, 1.0, 0.0))) {

            removeFrogBucket(player, player.inventory.itemInMainHand, true)

            player.sendMessage(miniMessage {
                stardustPlugin.i18nService.getMessage(
                    "frog-bucket-spawn-success",
                    *arrayOf(stardustPlugin.i18nService.getPluginPrefix())
                )
            })
        }
    }

    private fun removeFrogBucket(player: Player, itemStack: ItemStack, giveEmptyBucket: Boolean) {
        if (player.gameMode == GameMode.CREATIVE) return
        if (itemStack.amount > 1) {
            player.inventory.setItemInMainHand(itemStack.subtract(1))
        } else {
            player.inventory.setItemInMainHand(null)
        }

        if (giveEmptyBucket) player.inventory.addItem(ItemStack(Material.BUCKET))
    }

    @EventHandler
    fun handlePlayerEntityInteract(event: PrePlayerAttackEntityEvent) {

        val player = event.player
        val entity = event.attacked
        val itemInHand = player.inventory.itemInMainHand

        if (isFrogInBucket(itemInHand)) return
        if (entity is Frog && itemInHand.type == Material.BUCKET) {
            addFrogBucketToPlayer(player, 1, entity.customName() ?: miniMessage { entity.name }, entity.variant, true)
            entity.health = 0.0
            removeFrogBucket(player, itemInHand, false)
            event.isCancelled = true
        }
    }

    private fun isFrogInBucket(itemStack: ItemStack): Boolean {

        val itemMeta = itemStack.itemMeta ?: return false
        val container = itemMeta.persistentDataContainer

        if (!container.has(frogNameSpacedKey)) return false
        val value = container[frogNameSpacedKey, PersistentDataType.INTEGER]
        return value != null && value == frogInBucket
    }

    private fun deserializeFrogData(itemStack: ItemStack, location: Location): Boolean {

        val frog = location.world.spawn(location, Frog::class.java)
        val itemMeta = itemStack.itemMeta
        val container = itemMeta.persistentDataContainer
        if (!container.has(frogCustomNameKey) || !container.has(frogVariantKey)) return false

        val customName = container[frogCustomNameKey, PersistentDataType.STRING]
        val variant = container[frogVariantKey, PersistentDataType.STRING] ?: Variant.TEMPERATE.name

        if (customName != null) {
            frog.customName(miniMessage { customName.colorText() })
        }

        frog.variant = Variant.valueOf(variant.uppercase())
        return true
    }

    private fun createFrogItemStack(amount: Int, variant: Variant, customName: Component?): ItemStack {

        val itemStack = ItemStack(Material.BUCKET, amount)
        val itemMeta = itemStack.itemMeta

        itemMeta.displayName(miniMessage { frogBucketName })

        itemMeta.addEnchant(Enchantment.DIG_SPEED, 1, false)
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        itemMeta.lore(listOf(miniMessage { variant.name.lowercase() }))

        val container = itemMeta.persistentDataContainer

        if (customName != null) {
            container[frogCustomNameKey, PersistentDataType.STRING] = convertComponentToString(customName)
        }
        container[frogNameSpacedKey, PersistentDataType.INTEGER] = frogInBucket
        container[frogVariantKey, PersistentDataType.STRING] = variant.name

        itemStack.itemMeta = itemMeta
        return itemStack
    }

    private fun addFrogBucketToPlayer(
        player: Player,
        amount: Int,
        customName: Component,
        variant: Variant,
        actionBarMessage: Boolean
    ) {

        val slot = player.inventory.firstEmpty()

        //If the slot returns -1 the inventory of the player is full
        if (slot == -1) {
            player.sendMessage(
                miniMessage {
                    stardustPlugin.i18nService.getMessage(
                        "plugin.inventory-full",
                        *arrayOf(stardustPlugin.i18nService.getPluginPrefix())
                    )
                }
            )
            return
        }

        val itemStack = createFrogItemStack(amount, variant, customName)
        player.inventory.setItem(slot, itemStack)
        val message = miniMessage {
            stardustPlugin.i18nService.getMessage(
                "frog-bucket-added-to-inventory",
                *arrayOf(
                    stardustPlugin.i18nService.getPluginPrefix(),
                    amount,
                    frogBucketName,
                    player.coloredDisplayName()
                )
            )
        }

        if (!actionBarMessage) {
            player.sendMessage(message)
        } else {
            player.sendActionBar(message)
        }
    }
}