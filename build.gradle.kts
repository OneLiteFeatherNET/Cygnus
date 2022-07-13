plugins {
    id("java")
    `java-library`
    // Shadow
    // alias(libs.plugins.shadow)

    // Bukkit
    // alias(libs.plugins.pluginYmlBukkit)
    // alias(libs.plugins.runPaper)

    // LIQUIBASE
    // alias(libs.plugins.liquibase)
    // SonarQube
    // id("org.sonarqube") version "3.4.0.2513"
}

group = "net.onelitefeather"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://libraries.minecraft.net")
    maven("https://repo.cloudnetservice.eu/repository/releases/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://jitpack.io")
}

dependencies {
    // Paper
    compileOnly(libs.paper)
    // Sentry
    implementation(libs.bundles.sentry)
    // CloudNet
    compileOnly(libs.bundles.cloudnet)
    // Commands
    implementation(libs.bundles.cloud)

    // ChatComponents
    compileOnly(libs.bundles.adventure)


    // Database
    compileOnly(libs.bundles.hibernate)
    compileOnly(libs.bundles.liquibase)

    // liquibaseRuntime("org.liquibase.ext:liquibase-hibernate5:4.9.1") // Changelog based db
    // liquibaseRuntime("org.mariadb.jdbc:mariadb-java-client:3.0.4") // Changelog based db

    // Liquibase
    // liquibaseRuntime("org.liquibase:liquibase-core:3.10.3")
    // liquibaseRuntime("org.liquibase:liquibase-groovy-dsl:2.0.1")
    // liquibaseRuntime("ch.qos.logback:logback-core:1.2.3")
    // liquibaseRuntime("ch.qos.logback:logback-classic:1.2.3")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
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

/*liquibase {
    activities {
        create("diffMain") {
            (this.arguments as MutableMap<String, String>).apply {
                this["changeLogFile"] = "src/main/resources/db/changelog/db.changelog-diff.xml"
                this["url"] = "jdbc:mariadb://localhost:3306/elytrarace"
                this["username"] = "root"
                this["password"] = "%Schueler90"
// set e.g. the Dev Database to perform diffs
                this["referenceUrl"] = "jdbc:mariadb://localhost:3306/elytraracediff"
                this["referenceUsername"] = "root"
                this["referencePassword"] = "%Schueler90"
            }
        }
    }
}*/
/*sonarqube {
    properties {
        property("sonar.projectKey", "cliar_alwilda-loup_AYHtte8H7chqtZHGSV5T")
        property("sonar.qualitygate.wait", true)
    }
}
*/
