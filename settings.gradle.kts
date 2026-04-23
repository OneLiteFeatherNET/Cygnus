rootProject.name = "Cygnus"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/")
        maven("https://repository.derklaro.dev/snapshots/")
        maven {
            name = "OneLiteFeatherRepository"
            url = uri("https://repo.onelitefeather.dev/onelitefeather")
            if (System.getenv("CI") != null) {
                credentials {
                    username = System.getenv("ONELITEFEATHER_MAVEN_USERNAME")
                    password = System.getenv("ONELITEFEATHER_MAVEN_PASSWORD")
                }
            } else {
                credentials(PasswordCredentials::class)
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
    }
    versionCatalogs {
        create("libs") {
            version("shadow", "9.4.1")
            version("cloudnet", "4.0.0-RC16")
            version("aonyx", "0.7.1")
            version("cyclonedx", "3.2.4")
            version("pica", "0.0.1")

            library("aonyx.bom", "net.onelitefeather", "aonyx-bom").versionRef("aonyx")
            library("minestom", "net.minestom", "minestom").withoutVersion()
            library("adventure", "net.kyori", "adventure-text-minimessage").withoutVersion()
            library("cyano", "net.onelitefeather", "cyano").withoutVersion()
            library("guira", "net.onelitefeather", "guira").withoutVersion()
            library("junit.api", "org.junit.jupiter", "junit-jupiter-api").withoutVersion()
            library("junit.engine", "org.junit.jupiter", "junit-jupiter-engine").withoutVersion()
            library("junit.platform.launcher", "org.junit.platform", "junit-platform-launcher").withoutVersion()
            library("junit.params", "org.junit.jupiter", "junit-jupiter-params").withoutVersion()
            library("aves", "net.theevilreaper", "aves").version("1.13.1")
            library("xerus", "net.theevilreaper", "xerus").withoutVersion()
            library("pica", "net.onelitefeather", "pica").versionRef("pica")
            library("canis", "com.github.theEvilReaper", "Canis").version("master-SNAPSHOT")

            library("cloudnet-bom", "eu.cloudnetservice.cloudnet", "bom").versionRef("cloudnet")
            library("cloudnet-bridge", "eu.cloudnetservice.cloudnet", "bridge-api").withoutVersion()
            library("cloudnet-bridge-impl", "eu.cloudnetservice.cloudnet", "bridge-impl").withoutVersion()
            library("cloudnet-driver-impl", "eu.cloudnetservice.cloudnet", "driver-impl").withoutVersion()
            library("cloudnet-platform-inject", "eu.cloudnetservice.cloudnet", "platform-inject-api").withoutVersion()
            library("cloudnet-jvm-wrapper", "eu.cloudnetservice.cloudnet", "wrapper-jvm-api").withoutVersion()

            plugin("shadow", "com.gradleup.shadow").versionRef("shadow")

            bundle(
                "cloudnet",
                listOf(
                    "cloudnet-bridge",
                    "cloudnet-bridge-impl",
                    "cloudnet-driver-impl",
                    "cloudnet-platform-inject",
                    "cloudnet-jvm-wrapper"
                )
            )

            plugin("cyclonedx", "org.cyclonedx.bom").versionRef("cyclonedx")
        }
    }
}

include("common")
include("setup")
include("game")
