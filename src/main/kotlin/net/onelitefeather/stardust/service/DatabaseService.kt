package net.onelitefeather.stardust.service

import net.onelitefeather.stardust.command.CommandCooldown
import net.onelitefeather.stardust.user.User
import net.onelitefeather.stardust.user.UserProperties
import net.onelitefeather.stardust.user.UserProperty
import org.hibernate.SessionFactory
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.Configuration
import org.hibernate.cfg.Environment
import org.hibernate.dialect.MariaDBDialect
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider
import org.hibernate.tool.schema.Action
import java.util.Properties

class DatabaseService(val jdbcUrl: String, val username: String, val password: String, val driver: String) {

    lateinit var sessionFactory: SessionFactory

    fun init() {
        sessionFactory = buildSessionFactory()
    }

    private fun buildSessionFactory(): SessionFactory {

        val configuration = Configuration()
        val properties = Properties()

        properties[Environment.URL] = jdbcUrl
        properties[Environment.DRIVER] = driver
        properties[Environment.USER] = username
        properties[Environment.PASS] = password
        properties[Environment.CONNECTION_PROVIDER] = HikariCPConnectionProvider::class.java
        properties[Environment.DIALECT] = MariaDBDialect()
        properties[Environment.HBM2DDL_AUTO] = Action.UPDATE.name.lowercase()

        configuration.properties = properties

        configuration.addAnnotatedClass(User::class.java)
        configuration.addAnnotatedClass(UserProperties::class.java)
        configuration.addAnnotatedClass(UserProperty::class.java)
        configuration.addAnnotatedClass(CommandCooldown::class.java)

        val registry = StandardServiceRegistryBuilder().applySettings(configuration.properties).build()
        return configuration.buildSessionFactory(registry)
    }

    fun shutdown() {
        if (this::sessionFactory.isInitialized) {
            sessionFactory.close()
        }

    }
}