package net.onelitefeather.stardust.user

import jakarta.persistence.*
import org.hibernate.Hibernate

@Entity
@Table
data class UserProperty(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long?,

    @Column
    val name: String = "",

    @Column
    val value: String = "",

    @Column
    val type: Byte = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    val userProperties: UserProperties? = null) {

    constructor() : this(null)

    fun <T : Any> getValue(): T? {
        val result = when (this.type.toInt()) {
            0 -> this.value
            1 -> this.value.toIntOrNull()
            2 -> this.value.toBooleanStrictOrNull()
            3 -> this.value.toDoubleOrNull()
            4 -> this.value.toFloatOrNull()
            5 -> this.value.toShortOrNull()
            6 -> this.value.toByteOrNull()
            else -> null
        }
        return result as T?
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as UserProperty

        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id , key = $name , value = $value )"
    }
}