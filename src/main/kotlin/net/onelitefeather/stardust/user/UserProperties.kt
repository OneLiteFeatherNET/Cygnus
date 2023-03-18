package net.onelitefeather.stardust.user

import jakarta.persistence.*
import org.hibernate.Hibernate

@Entity
@Table
data class UserProperties(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long?,

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "userProperties")
    val values: List<UserProperty> = getDefaultUserProperties()) {

    constructor() : this(null)

    fun getProperty(propertyType: UserPropertyType): UserProperty {
        return values.firstOrNull { it.name == propertyType.name.lowercase() } ?: UserProperty(
            null,
            propertyType.name.lowercase(),
            propertyType.defaultValue.toString(),
            propertyType.type
        )
    }

    fun isVanished(): Boolean = getProperty(UserPropertyType.VANISHED).getValue() ?: false

    fun isFlying(): Boolean = getProperty(UserPropertyType.FLYING).getValue() ?: false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as UserProperties

        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id )"
    }
}