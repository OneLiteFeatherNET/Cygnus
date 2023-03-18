package net.onelitefeather.stardust.command

import java.util.concurrent.TimeUnit

data class CooldownData(val commandName: String, val timeUnit: TimeUnit, val time: Long) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CooldownData

        if (commandName != other.commandName) return false
        if (timeUnit != other.timeUnit) return false
        if (time != other.time) return false

        return true
    }

    override fun hashCode(): Int {
        var result = commandName.hashCode()
        result = 31 * result + timeUnit.hashCode()
        result = 31 * result + time.hashCode()
        return result
    }
}