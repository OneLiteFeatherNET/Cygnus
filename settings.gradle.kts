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
        // Lunar Client publishes the Apollo protocol classes (apollo-protos) here. Public, no credentials.
        maven {
            name = "lunarclient"
            url = uri("https://repo.lunarclient.dev")
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
            version("aonyx", "0.8.9")
            version("cyclonedx", "3.5.1")
            version("pica", "0.1.5")
            version("slf4j", "2.0.20")
            version("luckperms", "6.0.2")
            version("luckperms-minestom-loader", "6.0.2")
            // Lunar Client's Apollo protobuf messages. 0.2.7 is the newest release; it needs protobuf-java 3.25.0 or later.
            version("apollo-protos", "0.2.7")
            version("protobuf", "3.25.9")
            version("guava", "33.7.2-jre")
            version("falco", "3.0.1")
            version("minestom-extensions", "2.2.0")
            version("sentry", "8.61.0")
            // The API is the only OpenTelemetry artifact Cygnus ships. Production attaches the
            // opentelemetry-javaagent, which brings the SDK and the OTLP exporter and bridges
            // GlobalOpenTelemetry by exact class name - so this must stay unrelocated, and its
            // version must not exceed what the attached agent supports. Nothing in this repository
            // (setup/, docs, task configs) states the agent version, so this follows Ploceus, the
            // other OneLiteFeather service on the same agent line: agent 2.16.0, whose muzzle
            // markers top out at API 1.50, hence 1.51.0 as the most conservative release at or
            // above that ceiling. Raise it together with the agent, never ahead of it. Without the
            // agent every call lands on the no-op implementation.
            version("opentelemetry", "1.67.0")
            // Yearly player statistics (the :stats module). Hibernate runs on a Hikari pool, Flyway owns
            // the schema, PostgreSQL is the production database and H2 (PostgreSQL mode) backs the tests.
            version("hibernate", "7.4.13.Final")
            version("hikari", "7.1.0")
            version("flyway", "13.10.0")
            version("postgresql", "42.7.14")
            version("h2", "2.5.252")

            library("aonyx.bom", "net.onelitefeather", "aonyx-bom").versionRef("aonyx")
            library("slf4j.api", "org.slf4j", "slf4j-api").versionRef("slf4j")
            library("slf4j.simple", "org.slf4j", "slf4j-simple").versionRef("slf4j")
            library("guava", "com.google.guava", "guava").versionRef("guava")
            library("sentry", "io.sentry", "sentry").versionRef("sentry")
            library("opentelemetry.api", "io.opentelemetry", "opentelemetry-api").versionRef("opentelemetry")
            // Test only: an in-process SDK with an in-memory exporter, so tests assert on real spans.
            library("opentelemetry.sdk", "io.opentelemetry", "opentelemetry-sdk").versionRef("opentelemetry")
            library("opentelemetry.sdk.testing", "io.opentelemetry", "opentelemetry-sdk-testing").versionRef("opentelemetry")
            library("hibernate.core", "org.hibernate.orm", "hibernate-core").versionRef("hibernate")
            library("hikari", "com.zaxxer", "HikariCP").versionRef("hikari")
            library("flyway.core", "org.flywaydb", "flyway-core").versionRef("flyway")
            library("flyway.postgresql", "org.flywaydb", "flyway-database-postgresql").versionRef("flyway")
            library("postgresql", "org.postgresql", "postgresql").versionRef("postgresql")
            library("h2", "com.h2database", "h2").versionRef("h2")
            library("jetbrains.annotations", "org.jetbrains", "annotations").version("26.1.0")
            library("luckperms.api", "net.luckperms", "api").versionRef("luckperms")
            library("luckperms.minestom.loader", "net.luckperms", "minestom-loader").versionRef("luckperms-minestom-loader")
            library("apollo.protos", "com.lunarclient", "apollo-protos").versionRef("apollo-protos")
            library("protobuf.java", "com.google.protobuf", "protobuf-java").versionRef("protobuf")

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
include("stats")
include("game")
include("bridge")
