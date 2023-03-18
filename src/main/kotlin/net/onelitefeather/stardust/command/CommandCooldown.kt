package net.onelitefeather.stardust.command

import jakarta.persistence.*
import org.hibernate.Hibernate

@Entity
@Table
data class CommandCooldown(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column
    val commandSender: String = "",

    @Column
    val command: String = "",

    @Column
    val executedAt: Long = -1
) {

    constructor() : this(null)

    fun isOver(): Boolean = System.currentTimeMillis() >= executedAt

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as CommandCooldown

        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id , commandSender = $commandSender , command = $command , executedAt = $executedAt )"
    }
}
