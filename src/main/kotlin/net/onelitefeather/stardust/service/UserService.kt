package net.onelitefeather.stardust.service

import io.sentry.Sentry
import net.onelitefeather.stardust.StardustPlugin
import net.onelitefeather.stardust.api.PlayerVanishService
import net.onelitefeather.stardust.extenstions.addClient
import net.onelitefeather.stardust.extenstions.toSentryUser
import net.onelitefeather.stardust.tasks.UserTask
import net.onelitefeather.stardust.user.*
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitTask
import org.hibernate.HibernateException
import org.hibernate.Transaction
import java.util.UUID
import java.util.function.Consumer
import java.util.logging.Level

class UserService(private val stardustPlugin: StardustPlugin) {

    lateinit var userTask: UserTask
    lateinit var bukkitUserTask: BukkitTask
    lateinit var playerVanishService: PlayerVanishService<Player>

    fun startUserTask() {
        userTask = UserTask(stardustPlugin)
        playerVanishService = BukkitPlayerVanishService(stardustPlugin, this)
        bukkitUserTask = stardustPlugin.server.scheduler.runTaskTimerAsynchronously(stardustPlugin, userTask, 0L, 20L)
    }

    fun stopUserTask() {
        if (this::bukkitUserTask.isInitialized) {
            bukkitUserTask.cancel()
        }
    }

    fun getUsers(): List<User> {
        try {
            stardustPlugin.databaseService.sessionFactory.openSession().use { session ->
                val query = session.createQuery("SELECT u FROM User u", User::class.java)
                return query.list()
            }
        } catch (e: HibernateException) {
            stardustPlugin.logger.log(Level.SEVERE, "Could not load users.", e)
            Sentry.captureException(e)
        }

        return emptyList()
    }

    fun getUser(uuid: UUID): User? {
        try {
            stardustPlugin.databaseService.sessionFactory.openSession().use {
                val query = it.createQuery("SELECT u FROM User u WHERE u.uuid = :uuid", User::class.java)
                query.setParameter("uuid", uuid.toString())
                return query.uniqueResult()
            }
        } catch (e: HibernateException) {
            stardustPlugin.logger.log(Level.SEVERE, "Could not find an user by the given uuid $uuid", e)
            Sentry.captureException(e)
        }

        return null
    }

    fun getUser(name: String): User? {
        try {
            stardustPlugin.databaseService.sessionFactory.openSession().use {
                val query = it.createQuery("SELECT u FROM User u WHERE u.name = :name", User::class.java)
                query.setParameter("name", name)
                return query.uniqueResult()
            }
        } catch (e: HibernateException) {
            stardustPlugin.logger.log(Level.SEVERE, "Could not find an user by the given name $name", e)
            Sentry.captureException(e)
        }

        return null
    }

    fun registerUser(player: Player, consumer: Consumer<User>) {

        val uuid = player.uniqueId
        val name = player.name

        if (!isUserCreated(uuid)) {

            var transaction: Transaction? = null

            try {
                stardustPlugin.databaseService.sessionFactory.openSession().use { session ->

                    transaction = session.beginTransaction()
                    val userProperties = UserProperties(null)

                    session.persist(userProperties)

                    getDefaultUserProperties().forEach {
                        session.persist(it.copy(userProperties = userProperties))
                    }

                    val user = User(null, uuid.toString(), name, userProperties)

                    session.persist(user)

                    transaction?.commit()
                    consumer.accept(user)
                }
            } catch (e: HibernateException) {
                transaction?.rollback()
                Sentry.captureException(e) {
                    it.user = player.toSentryUser()
                    player.addClient(it)
                }
            }
        }
    }

    fun isUserCreated(uuid: UUID): Boolean = getUser(uuid) != null

    fun updateUserProperties(uuid: UUID, userproperties: UserProperties) {

        val user = getUser(uuid) ?: return
        var transaction: Transaction? = null
        try {
            stardustPlugin.databaseService.sessionFactory.openSession().use { session ->
                transaction = session.beginTransaction()

                val userProperties = getUserProperties(user)
                if (userProperties == null) {
                    session.persist(userproperties)
                } else {
                    session.merge(
                        userProperties.copy(
                            values = userproperties.values
                        )
                    )
                }

                transaction?.commit()
            }
        } catch (e: HibernateException) {
            stardustPlugin.logger.log(Level.SEVERE, "Cannot update user properties", e)
            transaction?.rollback()
            Sentry.captureException(e)
        }
    }

    fun getUserProperties(user: User): UserProperties? {

        try {
            stardustPlugin.databaseService.sessionFactory.openSession().use { session ->
                val query = session.createQuery(
                    "SELECT p FROM User u JOIN u.properties p WHERE p.id = :id",
                    UserProperties::class.java
                )
                query.setParameter("id", user.id)
                return query.uniqueResult()
            }
        } catch (e: HibernateException) {
            stardustPlugin.logger.log(Level.SEVERE, "Could not user properties for player ${user.name}", e)
            Sentry.captureException(e)
        }

        return null
    }

    fun updateUser(user: User): User? {
        var transaction: Transaction? = null
        try {
            stardustPlugin.databaseService.sessionFactory.openSession().use { session ->
                transaction = session.beginTransaction()

                val existUser = isUserCreated(user.getUniqueId())
                if (!existUser) {
                    session.persist(user)
                } else {
                    session.merge(user)
                }

                transaction?.commit()
                stardustPlugin.logger.info("Successfully ${if (!existUser) "created" else "updated"} User ${user.getUniqueId()} (${user.name})")
            }
        } catch (e: HibernateException) {
            stardustPlugin.logger.log(Level.SEVERE, "Something went wrong!", e)
            transaction?.rollback()
            Sentry.captureException(e)
        }

        return getUser(user.getUniqueId())
    }

    fun deleteUser(uuid: UUID, consumer: Consumer<Boolean>) {

        val cachedUser = getUser(uuid)
        var success: Boolean
        var transaction: Transaction? = null
        try {
            stardustPlugin.databaseService.sessionFactory.openSession().use { session ->
                transaction = session.beginTransaction()
                session.remove(cachedUser)
                transaction?.commit()
                success = true
            }
        } catch (e: HibernateException) {
            success = false
            stardustPlugin.logger.log(Level.SEVERE, "Something went wrong!", e)
            transaction?.rollback()
            Sentry.captureException(e)
        }

        consumer.accept(success)
    }

    fun existsUserProperty(userProperties: UserProperties, type: UserPropertyType): Boolean {
        try {
            stardustPlugin.databaseService.sessionFactory.openSession().use { session ->
                val query = session.createQuery(
                    "SELECT u FROM UserProperty u JOIN FETCH u.userProperties up WHERE up.id = :id AND u.name = :type",
                    UserProperty::class.java
                )
                query.setParameter("id", userProperties.id)
                query.setParameter("type", type.name.lowercase())
                return query.list().isNotEmpty()
            }
        } catch (e: HibernateException) {
            stardustPlugin.logger.log(Level.SEVERE, "Could not find user property $type", e)
            Sentry.captureException(e)
        }

        return false
    }

    fun getUserProperty(userProperties: UserProperties, type: UserPropertyType): UserProperty {

        val defaultProperty =
            UserProperty(null, type.name.lowercase(), type.defaultValue.toString(), type.type, userProperties)

        if (!existsUserProperty(userProperties, type)) return defaultProperty

        try {
            stardustPlugin.databaseService.sessionFactory.openSession().use { session ->
                val query = session.createQuery(
                    "SELECT u FROM UserProperty u JOIN FETCH u.userProperties up WHERE up.id = :id AND u.name = :type",
                    UserProperty::class.java
                )
                query.setParameter("id", userProperties.id)
                query.setParameter("type", type.name.lowercase())
                return query.uniqueResult()
            }
        } catch (e: HibernateException) {
            stardustPlugin.logger.log(Level.SEVERE, "Could not find user property $type", e)
            Sentry.captureException(e)
        }

        return defaultProperty
    }

    fun setUserProperty(user: User, type: UserPropertyType, value: Any) {

        var transaction: Transaction? = null
        try {
            stardustPlugin.databaseService.sessionFactory.openSession().use { session ->
                transaction = session.beginTransaction()
                val userProperty = getUserProperty(user.properties, type)
                if (!existsUserProperty(user.properties, type)) {
                    session.persist(UserProperty(null, type.name.lowercase(), value.toString(), type.type, user.properties))
                } else {
                    session.merge(userProperty.copy(value = value.toString()))
                }

                transaction?.commit()
            }
        } catch (e: HibernateException) {
            transaction?.rollback()
            stardustPlugin.logger.log(Level.SEVERE, "Cannot set user property $type for user ${user.name}", e)
            Sentry.captureException(e)
        }
    }
}