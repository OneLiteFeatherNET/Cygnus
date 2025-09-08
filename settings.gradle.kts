rootProject.name = "Cygnus"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://eldonexus.de/repository/maven-public/")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://reposilite.atlasengine.ca/public")
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
            version("publishdata", "1.4.0")
            version("shadow", "9.1.0")
            version("agones4j", "2.0.2")
            version("grpc", "1.68.0")
            version("tomcat-annotations-api", "6.0.53")

            version("bom", "1.4.2")
            version("aonyx", "0.5.2")

            library("mycelium.bom", "net.onelitefeather", "mycelium-bom").versionRef("bom")
            library("aonyx.bom", "net.onelitefeather", "aonyx-bom").versionRef("aonyx")
            library("minestom", "net.minestom", "minestom").withoutVersion()
            library("adventure", "net.kyori", "adventure-text-minimessage").withoutVersion()
            library("cyano", "net.onelitefeather", "cyano").withoutVersion()
            library("guira", "net.onelitefeather", "guira").withoutVersion()
            library("junit.api", "org.junit.jupiter", "junit-jupiter-api").withoutVersion()
            library("junit.engine", "org.junit.jupiter", "junit-jupiter-engine").withoutVersion()
            library("junit.platform.launcher", "org.junit.platform", "junit-platform-launcher").withoutVersion()
            library("junit.params", "org.junit.jupiter", "junit-jupiter-params").withoutVersion()
            library("aves", "net.theevilreaper", "aves").version("1.11.1-SNAPSHOT")
            library("xerus", "net.theevilreaper", "xerus").withoutVersion()

            library("mockito.core", "org.mockito", "mockito-core").withoutVersion()
            library("mockito.junit", "org.mockito", "mockito-junit-jupiter").withoutVersion()

            library("canis", "com.github.theEvilReaper", "Canis").version("master-SNAPSHOT")

            library("agones4j", "net.infumia", "agones4j").versionRef("agones4j")
            library("grpc.stub", "io.grpc", "grpc-stub").versionRef("grpc")
            library("grpc.protobuf", "io.grpc", "grpc-protobuf").versionRef("grpc")
            library("grpc.netty", "io.grpc", "grpc-netty").versionRef("grpc")
            library("grpc.okhttp", "io.grpc", "grpc-okhttp").versionRef("grpc")
            library("tomcat-annotations-api", "org.apache.tomcat", "annotations-api").versionRef("tomcat-annotations-api")

            plugin("shadow", "com.gradleup.shadow").versionRef("shadow")
            plugin("publishdata", "de.chojo.publishdata").versionRef("publishdata")
        }
    }
}

include("common")
include("setup")
include("game")
include("agones")
