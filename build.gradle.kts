import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder

plugins {
    kotlin("jvm") version "1.8.10"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.3"
    id("com.github.johnrengelman.shadow") version "7.1.2"

    // SonarQube
    id("org.sonarqube") version "4.0.0.2929"
    jacoco
}

val baseVersion = "1.0.0"
group = "net.onelitefeather"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.dmulloy2.net/repository/public/")
}
dependencies {

    // Paper
    compileOnly("io.papermc.paper:paper-api:1.19.3-R0.1-SNAPSHOT")
    bukkitLibrary("cloud.commandframework", "cloud-paper", "1.8.0")
    bukkitLibrary("cloud.commandframework", "cloud-annotations", "1.8.0")
    bukkitLibrary("cloud.commandframework", "cloud-minecraft-extras", "1.8.0")
    bukkitLibrary("org.apache.commons:commons-lang3:3.12.0")
    bukkitLibrary("me.lucko:commodore:2.2") {
        isTransitive = false
    }

    compileOnly("net.luckperms:api:5.4")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.0.0-SNAPSHOT")

    // Sentry
    implementation("io.sentry:sentry:6.6.0")
    implementation("io.sentry:sentry-jul:6.6.0")

    // Database
    implementation("org.hibernate:hibernate-core:6.1.5.Final")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.0.6")
    implementation("org.hibernate.orm:hibernate-hikaricp:6.1.5.Final")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks {

    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    getByName("sonar") {
        dependsOn(rootProject.tasks.test)
    }

    jacocoTestReport {
        dependsOn(rootProject.tasks.test)
        reports {
            xml.required.set(true)
        }
    }

    test {
        useJUnitPlatform()
    }

    shadowJar {
        archiveFileName.set("${rootProject.name}.${archiveExtension.getOrElse("jar")}")
    }
}

bukkit {
    main = "${rootProject.group}.stardust.StardustPlugin"
    apiVersion = "1.19"
    name = "Stardust"
    load = PluginLoadOrder.POSTWORLD

    authors = listOf("UniqueGame", "OneLiteFeather")
    softDepend = listOf("CloudNet-Bridge", "LuckPerms", "ProtocolLib")

    permissions {
        listOf(
            "stardust.command.gamemode",
            "stardust.command.flight",
            "stardust.command.flight.others",
            "stardust.command.glow",
            "stardust.command.heal",
            "stardust.command.rename",
            "stardust.command.repair",
            "stardust.command.unsign",
            "stardust.command.sign",
            "stardust.command.sign.override",
            "stardust.command.skull",
            "stardust.command.vanish.nodrop",
            "stardust.command.vanish.nocollect",
            "stardust.command.vanish",
            "stardust.command.vanish.others",
            "stardust.command.godmode",
            "stardust.command.heal.others",
            "stardust.command.gamemode.others",
            "stardust.commandcooldown.bypass",
            "stardust.join.gamemode",
            "stardust.bypass.damage.vanish",
            "stardust.bypass.damage.invulnerable",
            "stardust.vanish.silentopen.interact",
            "stardust.vanish.silentopen",
            "stardust.vanish.auto",
            "stardust.bypass.vanish",
            "stardust.command.frogbucket"
        ).forEach { perm ->
            register(perm) {
                default = Default.OP
            }
        }
        register("stardust.command.help") {
            default = Default.TRUE
        }
    }
}

version = if (System.getenv().containsKey("CI")) {
    val releaseOrSnapshot =
        if (System.getenv("CI_COMMIT_REF_NAME").equals("main", true) || System.getenv("CI_COMMIT_REF_NAME")
                .startsWith("v")
        ) {
            ""
        } else if (System.getenv("CI_COMMIT_REF_NAME").equals("test", true)) {
            "-PREVIEW"
        } else {
            "-SNAPSHOT"
        }
    "$baseVersion$releaseOrSnapshot+${System.getenv("CI_COMMIT_SHORT_SHA")}"
} else {
    "$baseVersion-SNAPSHOT"
}

sonarqube {
    properties {
        property("sonar.projectKey", "onelitefeather_projects_stardust_AYRjNInxwVDHzVoeOyqT")
        property("sonar.qualitygate.wait", true)
    }
}

