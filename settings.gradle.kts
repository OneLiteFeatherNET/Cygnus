rootProject.name = "Cygnus"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://eldonexus.de/repository/maven-public/")
    }
}


dependencyResolutionManagement {
    if (System.getenv("CI") != null) {
        repositoriesMode = RepositoriesMode.PREFER_SETTINGS
        repositories {
            maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            maven("https://repo.htl-md.schule/repository/Gitlab-Runner/")
            maven {
                val groupdId = 28 // Gitlab Group
                val ciApiv4Url = System.getenv("CI_API_V4_URL")
                url = uri("$ciApiv4Url/groups/$groupdId/-/packages/maven")
                name = "GitLab"
                credentials(HttpHeaderCredentials::class.java) {
                    name = "Job-Token"
                    value = System.getenv("CI_JOB_TOKEN")
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
            }
        }
    } else {
        repositories {
            maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            mavenCentral()
            maven("https://jitpack.io")
            maven {
                val groupdId = 28 // Gitlab Group
                url = uri("https://gitlab.onelitefeather.dev/api/v4/groups/$groupdId/-/packages/maven")
                name = "GitLab"
                credentials(HttpHeaderCredentials::class.java) {
                    name =  "Private-Token"
                    value = providers.gradleProperty("gitLabPrivateToken").get()
                }
                authentication {
                    create<HttpHeaderAuthentication>("header")
                }
            }
        }
    }

    versionCatalogs {
        create("libs") {
            version("publishdata", "1.4.0")
            version("minestom", "1.5.0-SNAPSHOT")
            version("aves", "1.6.0-SNAPSHOT")
            version("xerus", "1.3.0-SNAPSHOT")
            version("shadow", "8.3.5")
            version("agones4j", "2.0.2")
            version("grpc", "1.68.0")
            version("tomcat-annotations-api", "6.0.53")

            library("microtus.bom", "net.onelitefeather.microtus", "bom").versionRef("minestom")
            library("dungeon.bom", "net.theevilreaper.dungeon.bom", "base").version("1.1.1")

            library("minestom", "net.onelitefeather.microtus", "Microtus").withoutVersion()
            library("minestom-test", "net.onelitefeather.microtus.testing", "testing").withoutVersion()
            library("junit.api", "org.junit.jupiter", "junit-jupiter-api").withoutVersion()
            library("junit.engine", "org.junit.jupiter", "junit-jupiter-engine").withoutVersion()
            library("mockito.core", "org.mockito", "mockito-core").withoutVersion()
            library("mockito.junit", "org.mockito", "mockito-junit-jupiter").withoutVersion()

            library("aves", "de.icevizion.lib", "aves").versionRef("aves")
            library("xerus", "net.theevilreaper.xerus", "xerus").versionRef("xerus")
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
