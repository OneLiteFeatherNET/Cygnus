package net.onelitefeather.stardust.extenstions

import cloud.commandframework.annotations.AnnotationParser
import cloud.commandframework.arguments.parser.ParserParameters
import cloud.commandframework.arguments.parser.StandardParameters
import cloud.commandframework.bukkit.CloudBukkitCapabilities
import cloud.commandframework.execution.CommandExecutionCoordinator
import cloud.commandframework.meta.CommandMeta
import cloud.commandframework.minecraft.extras.MinecraftHelp
import cloud.commandframework.paper.PaperCommandManager
import io.sentry.Sentry
import net.kyori.adventure.text.format.NamedTextColor
import net.onelitefeather.stardust.StardustPlugin
import net.onelitefeather.stardust.command.commands.*
import net.onelitefeather.stardust.service.LuckPermsService
import org.bukkit.command.CommandSender
import java.util.function.Function
import java.util.logging.Level

fun StardustPlugin.initLuckPermsSupport() {
    if(server.pluginManager.isPluginEnabled("LuckPerms")) {
        luckPermsService = LuckPermsService(this)
        luckPermsService.init()
    }
}

fun StardustPlugin.registerCommands() {
    annotationParser.parse(FlightCommand(this))
    annotationParser.parse(GameModeCommand(this))
    annotationParser.parse(GlowCommand(this))
    annotationParser.parse(GodmodeCommand(this))
    annotationParser.parse(HealCommand(this))
    annotationParser.parse(HelpCommand(this))
    annotationParser.parse(RenameCommand(this))
    annotationParser.parse(RepairCommand(this))
    annotationParser.parse(SignCommand(this))
    annotationParser.parse(SkullCommand(this))
    annotationParser.parse(VanishCommand(this))
    annotationParser.parse(syncFrogService)
}

fun StardustPlugin.buildCommandSystem() {
    try {
        paperCommandManager = PaperCommandManager(
            this,
            CommandExecutionCoordinator.simpleCoordinator(),
            Function.identity(),
            Function.identity()
        )
    } catch (e: Exception) {
        logger.log(Level.WARNING, "Failed to build command system", e)
        Sentry.captureException(e)
        server.pluginManager.disablePlugin(this)
        return
    }

    if (paperCommandManager.hasCapability(CloudBukkitCapabilities.BRIGADIER)) {
        paperCommandManager.registerBrigadier()
        logger.info("Brigadier support enabled")
    }

    if (paperCommandManager.hasCapability(CloudBukkitCapabilities.ASYNCHRONOUS_COMPLETION)) {
        paperCommandManager.registerAsynchronousCompletions()
        logger.info("Asynchronous completions enabled")
    }


    val commandMetaFunction =
        Function<ParserParameters, CommandMeta> { p: ParserParameters ->
            CommandMeta.simple().with(
                CommandMeta.DESCRIPTION,
                p.get(StandardParameters.DESCRIPTION, "No description")
            ).build()
        }

    annotationParser = AnnotationParser(
        paperCommandManager,
        CommandSender::class.java, commandMetaFunction
    )
}

fun StardustPlugin.buildHelpSystem() {
    minecraftHelp = MinecraftHelp.createNative(
        "/stardust help",
        paperCommandManager
    )

    minecraftHelp.helpColors = MinecraftHelp.HelpColors.of(
        NamedTextColor.GOLD,
        NamedTextColor.YELLOW,
        NamedTextColor.GOLD,
        NamedTextColor.GRAY,
        NamedTextColor.GOLD
    )
}