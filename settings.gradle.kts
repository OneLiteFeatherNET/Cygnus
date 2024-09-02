rootProject.name = "Cygnus"
val cloudNetVersion = "4.0.0-RC9"

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
            version("minestom", "1.4.2")
            version("junit", "5.10.3")
            version("mockito", "5.12.0")
            version("aves", "1.5.2")
            version("xerus", "1.2.2+7b54a084")

            library("microtus.bom", "net.onelitefeather.microtus", "bom").versionRef("minestom")
            library("dungeon.bom", "net.theevilreaper.dungeon.bom", "base").version("1.0.4")

            library("minestom", "net.onelitefeather.microtus", "Microtus").withoutVersion()
            library("minestom-test", "net.onelitefeather.microtus.testing", "testing").withoutVersion()
            library("junit.api", "org.junit.jupiter", "junit-jupiter-api").withoutVersion()
            library("junit.engine", "org.junit.jupiter", "junit-jupiter-engine").withoutVersion()
            library("mockito.core", "org.mockito", "mockito-core").withoutVersion()
            library("mockito.junit", "org.mockito", "mockito-junit-jupiter").withoutVersion()

            library("aves", "de.icevizion.lib", "aves").versionRef("aves")
            library("xerus", "net.theevilreaper.xerus", "xerus").versionRef("xerus")
            library("canis", "com.github.theEvilReaper", "Canis").version("master-SNAPSHOT")

            val cloudnetBaseGroup = "eu.cloudnetservice.cloudnet"

            library("cloudnet-wrapper", cloudnetBaseGroup, "wrapper-jvm").version(cloudNetVersion)
            library("cloudnet-bridge", cloudnetBaseGroup, "bridge").version(cloudNetVersion)
            library("cloudnet-driver", cloudnetBaseGroup, "driver").version(cloudNetVersion)

            bundle("cloudnet", listOf("cloudnet-wrapper", "cloudnet-bridge", "cloudnet-driver"))

            plugin("publishdata", "de.chojo.publishdata").versionRef("publishdata")
        }
    }
}