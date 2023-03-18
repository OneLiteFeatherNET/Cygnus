package net.onelitefeather.stardust.user

import jakarta.persistence.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.onelitefeather.stardust.extenstions.coloredDisplayName
import net.onelitefeather.stardust.extenstions.miniMessage
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import org.hibernate.Hibernate
import java.util.*

@Entity
@Table
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column
    val uuid: String = UUID.randomUUID().toString(),

    @Column
    val name: String = "",

    @OneToOne
    val properties: UserProperties = UserProperties(),


    @OneToMany(fetch = FetchType.EAGER, mappedBy = "ignoredUser")
    val ignoredUsers: List<User> = emptyList(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    val ignoredUser: User? = null
) {
    constructor() : this(null)


    fun setDisplayName(displayName: String) {
        val base = getBase() ?: return
        base.displayName(miniMessage {
            MiniMessage.miniMessage().serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(displayName))
        })
    }

    fun getUniqueId(): UUID = UUID.fromString(uuid)

    fun getDisplayName(): String {
        val base = getBase() ?: return name
        return base.coloredDisplayName()
    }

    fun getBase(): Player? = Bukkit.getPlayer(getUniqueId())

    fun kick(message: Component): Boolean {
        val player = getBase() ?: return false
        player.kick(message)
        return true
    }

    fun confirmChatMessage(namespacedKey: NamespacedKey, value: Boolean) {
        val player = getBase() ?: return
        val container = player.persistentDataContainer
        container[namespacedKey, PersistentDataType.INTEGER] = if (value) 1 else 0
    }

    fun hasChatConfirmation(namespacedKey: NamespacedKey): Boolean {
        val player = getBase() ?: return false
        val container = player.persistentDataContainer
        if (!container.has(namespacedKey)) return false
        val value = container[namespacedKey, PersistentDataType.INTEGER] ?: return false
        return value == 1
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as User

        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()

    @Override
    override fun toString(): String {
        return this::class.simpleName + "(id = $id , uuid = $uuid , lastKnownName = $name )"
    }

}