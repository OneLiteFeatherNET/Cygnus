package net.onelitefeather.stardust

import cloud.commandframework.annotations.AnnotationParser
import cloud.commandframework.minecraft.extras.MinecraftHelp
import cloud.commandframework.paper.PaperCommandManager
import io.sentry.Sentry
import io.sentry.protocol.Device
import net.onelitefeather.stardust.api.CommandCooldownService
import net.onelitefeather.stardust.api.ItemSignService
import net.onelitefeather.stardust.extenstions.buildCommandSystem
import net.onelitefeather.stardust.extenstions.buildHelpSystem
import net.onelitefeather.stardust.extenstions.initLuckPermsSupport
import net.onelitefeather.stardust.extenstions.registerCommands
import net.onelitefeather.stardust.listener.*
import net.onelitefeather.stardust.service.*
import org.bukkit.NamespacedKey
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.metadata.MetadataValue
import org.bukkit.plugin.java.JavaPlugin

class StardustPlugin : JavaPlugin() {

    lateinit var paperCommandManager: PaperCommandManager<CommandSender>
    lateinit var annotationParser: AnnotationParser<CommandSender>
    lateinit var minecraftHelp: MinecraftHelp<CommandSender>

    lateinit var i18nService: I18nService
    lateinit var databaseService: DatabaseService
    lateinit var userService: UserService
    lateinit var commandCooldownService: CommandCooldownService
    lateinit var luckPermsService: LuckPermsService
    lateinit var itemSignService: ItemSignService<ItemStack, Player>
    lateinit var packetListener: PacketListener
    lateinit var syncFrogService: SyncFrogService
    lateinit var context: StardustPlugin

    lateinit var chatConfirmationKey: NamespacedKey
    lateinit var signedNameSpacedKey: NamespacedKey

    lateinit var vanishedMetadata: MetadataValue
    lateinit var notVanishedMetadata: MetadataValue

    @Suppress("kotlin:S1874")
    override fun onEnable() {

        Sentry.init {
            it.release = description.version
            it.environment = if (description.version.contains("-SNAPSHOT", true)) "development" else "production"
            it.tracesSampleRate = 1.0
            it.dsn = "https://81a5e07ea4b54399a4cfd0b9710e0310@sentry.themeinerlp.dev/3"
        }

        Sentry.configureScope {
            Device().apply {
                name = server.name
                model = server.version
                modelId = server.bukkitVersion
            }.also { device ->
                it.setContexts("device", device)
            }
        }

        Sentry.startSession()

        try {
            context = this

            //Creating the default config
            saveDefaultConfig()

            //Saving the default config values
            config.options().copyDefaults(true)


            //Saving the config is needed
            saveConfig()

            vanishedMetadata = FixedMetadataValue(this, true)
            notVanishedMetadata = FixedMetadataValue(this, false)

            syncFrogService = SyncFrogService(this)
            itemSignService = BukkitItemSignService(this)
            i18nService = I18nService(this)

            luckPermsService = LuckPermsService(this)
            luckPermsService.init()

            val jdbcUrl = config.getString("database.jdbcUrl")
            val databaseDriver = config.getString("database.driver")
            val username = config.getString("database.username")
            val password = config.getString("database.password") ?: "IReallyKnowWhatIAmDoingISwear"

            if (jdbcUrl != null && databaseDriver != null && username != null) {
                databaseService = DatabaseService(jdbcUrl, username, password, databaseDriver)
                databaseService.init()
                commandCooldownService = BukkitCommandCooldownService(this)
            }

            initLuckPermsSupport()
            buildCommandSystem()
            buildHelpSystem()
            registerCommands()

            userService = UserService(this)
            userService.startUserTask()

            if (server.pluginManager.isPluginEnabled("ProtocolLib")) {
                packetListener = PacketListener(this)
                packetListener.register()
            }

            server.pluginManager.registerEvents(VanishSilentContainerFeature(this), this)
            server.pluginManager.registerEvents(CommandCooldownListener(this), this)
            server.pluginManager.registerEvents(PlayerChatListener(this), this)
            server.pluginManager.registerEvents(PlayerConnectionListener(this), this)
            server.pluginManager.registerEvents(PlayerVanishListener(this), this)

            signedNameSpacedKey = NamespacedKey(this, "signed")
            chatConfirmationKey = NamespacedKey(this, "chat_confirmation")
        } catch (e: Exception) {
            Sentry.captureException(e)
        }
    }

    override fun onDisable() {

        if (this::userService.isInitialized) {
            userService.stopUserTask()
        }

        if (this::databaseService.isInitialized) {
            databaseService.shutdown()
        }

        if (this::packetListener.isInitialized) {
            packetListener.unregister()
        }
    }
}
