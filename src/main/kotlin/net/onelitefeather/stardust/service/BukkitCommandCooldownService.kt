package net.onelitefeather.stardust.service

import io.sentry.Sentry
import net.onelitefeather.stardust.StardustPlugin
import net.onelitefeather.stardust.api.CommandCooldownService
import net.onelitefeather.stardust.command.CommandCooldown
import net.onelitefeather.stardust.command.CooldownData
import org.hibernate.HibernateException
import org.hibernate.Transaction
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level


class BukkitCommandCooldownService(private val stardustPlugin: StardustPlugin) : CommandCooldownService {

    override fun getCommandCooldown(commandSender: UUID, command: String): CommandCooldown? {
        try {
            stardustPlugin.databaseService.sessionFactory.openSession().use { session ->

                val query = session.createQuery(
                    "SELECT cc FROM CommandCooldown cc WHERE cc.commandSender = :commandSender AND cc.command = :command",
                    CommandCooldown::class.java
                )

                query.setParameter("commandSender", commandSender.toString())
                query.setParameter("command", command)
                return query.uniqueResult()
            }
        } catch (e: HibernateException) {
            stardustPlugin.logger.log(
                Level.SEVERE,
                "Could not get command cooldown by the given sender $commandSender and command $command",
                e
            )
            Sentry.captureException(e)
        }

        return null
    }

    override fun addCommandCooldown(commandSender: UUID, command: String, timeUnit: TimeUnit, time: Long) {

        var transaction: Transaction? = null
        try {
            stardustPlugin.databaseService.sessionFactory.openSession().use { session ->

                transaction = session.beginTransaction()
                var commandCooldown = getCommandCooldown(commandSender, command)
                val executedAt = getCooldownTime(timeUnit, time)

                if (commandCooldown != null) {
                    session.merge(
                        commandCooldown.copy(
                            commandSender = commandSender.toString(),
                            command = command,
                            executedAt = executedAt
                        )
                    )
                } else {
                    commandCooldown = CommandCooldown(
                        null,
                        commandSender.toString(),
                        command,
                        executedAt
                    )
                    session.persist(commandCooldown)
                }

                transaction?.commit()
            }
        } catch (e: HibernateException) {
            stardustPlugin.logger.log(Level.SEVERE, "Could not add command cooldown", e)
            Sentry.captureException(e)
            if (transaction != null) {
                transaction?.rollback()
            }
        }
    }

    override fun removeCommandCooldown(commandSender: UUID, command: String) {
        var transaction: Transaction? = null
        try {
            stardustPlugin.databaseService.sessionFactory.openSession().use { session ->
                transaction = session.beginTransaction()
                val commandCooldown = getCommandCooldown(commandSender, command)
                session.remove(commandCooldown)
                transaction?.commit()
            }
        } catch (e: HibernateException) {
            stardustPlugin.logger.log(Level.SEVERE, "Could not remove command cooldown", e)
            Sentry.captureException(e)
            if (transaction != null) {
                transaction?.rollback()
            }
        }
    }

    override fun exists(commandSender: UUID, command: String): Boolean =
        getCommandCooldown(commandSender, command) != null

    override fun isCooldownOver(commandSender: UUID, command: String): Boolean =
        getCommandCooldown(commandSender, command)?.isOver() == true

    override fun hasCommandCooldown(commandLabel: String): Boolean = getCooldownData(commandLabel) != null

    override fun getCooldownDataList(): List<CooldownData> {
        val section = stardustPlugin.config.getConfigurationSection("command-cooldowns") ?: return emptyList()
        return section.getKeys(false).map {
            val timeunitName = section.getString("$it.timeunit") ?: "SECONDS"
            CooldownData(it, TimeUnit.valueOf(timeunitName), section.getLong("$it.time"))
        }
    }

    override fun getCooldownData(commandName: String): CooldownData? =
        getCooldownDataList().firstOrNull { it.commandName.equals(commandName, true) }
}