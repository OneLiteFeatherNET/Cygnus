rootProject.name = "Cygnus"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        // Reposilite proxy in front of JitPack, for catalog entries that resolve from
        // com.github.* (Canis).
        maven {
            name = "reposiliteRepositoryOnelitefeatherProxy"
            url = uri("https://repo.onelitefeather.dev/onelitefeather-proxy")
        }
        // minestom-extensions is published here and, unlike the onelitefeather repository
        // below, needs no credentials - keep it separate so a fresh clone resolves it.
        maven {
            name = "OneLiteFeatherReleases"
            url = uri("https://repo.onelitefeather.dev/releases")
        }
        maven("https://central.sonatype.com/repository/maven-snapshots/")
        maven("https://repository.derklaro.dev/snapshots/")
        maven("https://repository.derklaro.dev/releases/")
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
            version("shadow", "9.6.1")
            version("cloudnet", "4.0.0-RC16")
            version("aonyx", "0.8.4")
            version("cyclonedx", "3.4.0")
            version("pica", "0.1.2")
            version("slf4j", "2.0.18")
            version("luckperms", "5.5")
            version("luckperms-minestom-loader", "5.6-SNAPSHOT")
            version("guava", "33.6.0-jre")
            version("falco", "2.1.0")
            version("minestom-extensions", "2.1.1")
            version("hibernate", "6.6.55.Final")
            version("mariadb-driver", "3.5.3")
            version("hikaricp", "6.3.0")
            version("testcontainers", "1.20.6")
            version("jetbrains-annotations", "26.1.0")

            library("aonyx.bom", "net.onelitefeather", "aonyx-bom").versionRef("aonyx")
            library("jetbrains.annotations", "org.jetbrains", "annotations").versionRef("jetbrains-annotations")
            library("slf4j.api", "org.slf4j", "slf4j-api").versionRef("slf4j")
            library("slf4j.simple", "org.slf4j", "slf4j-simple").versionRef("slf4j")
            library("guava", "com.google.guava", "guava").versionRef("guava")
            library("luckperms.api", "net.luckperms", "api").versionRef("luckperms")
            library("luckperms.minestom.loader", "net.luckperms", "minestom-loader").versionRef("luckperms-minestom-loader")

            library("minestom", "net.minestom", "minestom").withoutVersion()
            // OneLiteFeather fork of the archived hollow-cube/minestom-ce-extensions. Same
            // packages (net.hollowcube.minestom.extensions, net.minestom.server.extensions), but
            // extension dependencies resolve through Maven Resolver instead of the Kotlin-based
            // DependencyGetter, so no Kotlin stdlib is needed on the class path anymore.
            library("minestom-extensions-bom", "net.onelitefeather", "minestom-extensions-bom").versionRef("minestom-extensions")
            library("minestom-extensions", "net.onelitefeather", "minestom-extensions").withoutVersion()
            // Generates extension.json from @ExtensionInfo at compile time; source retention, so
            // the annotation itself never reaches the extension jar.
            library("minestom-extensions-processor", "net.onelitefeather", "minestom-extensions-processor").withoutVersion()
            library("adventure", "net.kyori", "adventure-text-minimessage").withoutVersion()
            library("cyano", "net.onelitefeather", "cyano").withoutVersion()
            library("guira", "net.onelitefeather", "guira").withoutVersion()
            library("junit.api", "org.junit.jupiter", "junit-jupiter-api").withoutVersion()
            library("junit.engine", "org.junit.jupiter", "junit-jupiter-engine").withoutVersion()
            library("junit.platform.launcher", "org.junit.platform", "junit-platform-launcher").withoutVersion()
            library("junit.params", "org.junit.jupiter", "junit-jupiter-params").withoutVersion()
            library("aves", "net.theevilreaper", "aves").withoutVersion()
            library("xerus", "net.theevilreaper", "xerus").withoutVersion()
            library("pica", "net.onelitefeather", "pica").versionRef("pica")
            library("falco.bom", "net.onelitefeather", "falco-bom").versionRef("falco")
            library("falco.anvil", "net.onelitefeather", "falco-anvil").withoutVersion()
            library("canis", "com.github.theEvilReaper", "Canis").version("master-SNAPSHOT")
            library("hibernate.core", "org.hibernate.orm", "hibernate-core").versionRef("hibernate")
            library("hibernate.hikaricp", "org.hibernate.orm", "hibernate-hikaricp").versionRef("hibernate")
            library("mariadb.driver", "org.mariadb.jdbc", "mariadb-java-client").versionRef("mariadb-driver")
            library("hikaricp", "com.zaxxer", "HikariCP").versionRef("hikaricp")
            library("testcontainers", "org.testcontainers", "testcontainers").versionRef("testcontainers")
            library("testcontainers.junit.jupiter", "org.testcontainers", "junit-jupiter").versionRef("testcontainers")
            library("testcontainers.mariadb", "org.testcontainers", "mariadb").versionRef("testcontainers")

            // CloudNet is never bundled: the wrapper provides the driver at runtime and the bridge
            // arrives as a Minestom extension. Only the :bridge extension module compiles against it.
            library("cloudnet-bom", "eu.cloudnetservice.cloudnet", "bom").versionRef("cloudnet")
            library("cloudnet-bridge", "eu.cloudnetservice.cloudnet", "bridge-api").withoutVersion()
            library("cloudnet-bridge-impl", "eu.cloudnetservice.cloudnet", "bridge-impl").withoutVersion()
            library("cloudnet-driver-api", "eu.cloudnetservice.cloudnet", "driver-api").withoutVersion()
            library("cloudnet-driver-impl", "eu.cloudnetservice.cloudnet", "driver-impl").withoutVersion()
            library("cloudnet-platform-inject", "eu.cloudnetservice.cloudnet", "platform-inject-api").withoutVersion()
            library("cloudnet-jvm-wrapper", "eu.cloudnetservice.cloudnet", "wrapper-jvm-api").withoutVersion()

            plugin("shadow", "com.gradleup.shadow").versionRef("shadow")
            plugin("cyclonedx", "org.cyclonedx.bom").versionRef("cyclonedx")

        }
    }
}

include("common")
include("setup")
include("game")
include("bridge")
include("stats")
