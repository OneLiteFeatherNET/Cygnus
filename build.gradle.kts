plugins {
    id("java")
    `java-library`
    // FIXME
    // Bukkit
    // id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
    // id("xyz.jpenilla.run-paper") version "1.0.6"

    // Shadow
    // id("com.github.johnrengelman.shadow") version "7.1.2"

    // LIQUIBASE
    // id("org.liquibase.gradle") version "2.1.0"
}

group = "net.onelitefeather"
version = "1.0.0-SNAPSHOT"

val cloudNetVersion = "3.4.4-RELEASE"

repositories {
    mavenCentral()
    maven(url = uri("https://papermc.io/repo/repository/maven-public/"))
    maven(url = uri("https://maven.enginehub.org/repo/"))
    maven(url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/"))
    maven(url = uri("https://oss.sonatype.org/content/groups/public/"))
    maven(url = uri("https://libraries.minecraft.net"))
    maven(url = uri("https://repo.cloudnetservice.eu/repository/releases/"))
    maven(url = uri("https://repo.dmulloy2.net/repository/public/"))
    maven(url = uri("https://jitpack.io"))
}

dependencies {
    // Paper
    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")

    // Sentry
    compileOnly("io.sentry:sentry:5.7.3")
    compileOnly("io.sentry:sentry-jul:5.7.3")

    // CloudNet
    compileOnly("de.dytanic.cloudnet:cloudnet-cloudperms:${cloudNetVersion}")
    compileOnly("de.dytanic.cloudnet:cloudnet-bridge:${cloudNetVersion}")
    compileOnly("de.dytanic.cloudnet:cloudnet-driver:${cloudNetVersion}")

    // ChatComponents
    compileOnly("net.kyori:adventure-api:4.10.1")
    compileOnly("net.kyori:adventure-text-minimessage:4.10.1")
    compileOnly("net.kyori:adventure-text-serializer-plain:4.10.1")

    // Commands
    compileOnly("cloud.commandframework", "cloud-paper", "1.6.2")
    compileOnly("cloud.commandframework", "cloud-annotations", "1.6.2")
    compileOnly("cloud.commandframework", "cloud-minecraft-extras", "1.6.2")
    compileOnly("me.lucko:commodore:1.13") {
        isTransitive = false
    }

    // Database
    compileOnly("org.hibernate:hibernate-core:6.0.0.Final")
    // compileOnly("org.liquibase:liquibase-core:3.4.1") // Changelog based db
    // compileOnly("org.hibernate.orm:hibernate-envers:6.0.0.Final") // Revision tracking
    compileOnly("org.mariadb.jdbc:mariadb-java-client:3.0.4")
    // Hikaricp
    // compileOnly("org.liquibase.ext:liquibase-hibernate5:4.9.1") // Changelog based db
    compileOnly("com.zaxxer:HikariCP:5.0.1")
    compileOnly("org.hibernate.orm:hibernate-hikaricp:6.0.0.Final")

    // liquibaseRuntime("org.liquibase.ext:liquibase-hibernate5:4.9.1") // Changelog based db
    // liquibaseRuntime("org.mariadb.jdbc:mariadb-java-client:3.0.4") // Changelog based db

    // Liquibase
    // liquibaseRuntime("org.liquibase:liquibase-core:3.10.3")
    // liquibaseRuntime("org.liquibase:liquibase-groovy-dsl:2.0.1")
    // liquibaseRuntime("ch.qos.logback:logback-core:1.2.3")
    // liquibaseRuntime("ch.qos.logback:logback-classic:1.2.3")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks {
    compileJava {
        options.release.set(17)
        options.encoding = "UTF-8"
    }
    test {
        useJUnitPlatform()
    }
    /*runServer {
        minecraftVersion("1.18.2")
    }*/
    /*shadowJar {
        archiveFileName.set("${rootProject.name}.${archiveExtension.getOrElse("jar")}")
    }*/
}

/*bukkit {
    main = "${rootProject.group}.ElytraRace"
    apiVersion = "1.18"
    name = ""
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.STARTUP

    authors = listOf("TheMeinerLP", "OneLiteFeather")

    depend = listOf("helper")
    softDepend = listOf("CloudNet-Bridge")
}*/