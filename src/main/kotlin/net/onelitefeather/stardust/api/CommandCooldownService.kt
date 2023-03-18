package net.onelitefeather.stardust.api

import net.onelitefeather.stardust.command.CommandCooldown
import net.onelitefeather.stardust.command.CooldownData
import java.util.*
import java.util.concurrent.TimeUnit

interface CommandCooldownService {

    fun getCommandCooldown(commandSender: UUID, command: String): CommandCooldown?

    fun addCommandCooldown(commandSender: UUID, command: String, timeUnit: TimeUnit, time: Long)

    fun removeCommandCooldown(commandSender: UUID, command: String)

    fun exists(commandSender: UUID, command: String): Boolean

    fun isCooldownOver(commandSender: UUID, command: String): Boolean

    fun hasCommandCooldown(commandLabel: String): Boolean

    fun getCooldownDataList(): List<CooldownData>

    fun getCooldownData(commandName: String): CooldownData?

    fun getCooldownTime(timeUnit: TimeUnit, time: Long): Long {
        return System.currentTimeMillis() + when (timeUnit) {
            TimeUnit.DAYS -> 1000 * 60 * 60 * 24 * time
            TimeUnit.HOURS -> 1000 * 60 * 60 * time
            TimeUnit.MINUTES -> 1000 * 60 * time
            TimeUnit.SECONDS -> 1000 * time
            else -> throw IllegalStateException(
                "The TimeUnit " + timeUnit.name.lowercase() + " is not allowed here"
            )
        }
    }
}