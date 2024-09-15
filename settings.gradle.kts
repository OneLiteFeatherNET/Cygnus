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
            mavenLocal()
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
            version("microtus","1.5.0-SNAPSHOT")
            version("junit", "5.10.2")
            version("publishdata", "1.2.5-DEV")

            plugin("publishdata", "de.chojo.publishdata").versionRef("publishdata")

            library("microtus-bom", "net.onelitefeather.microtus", "bom").versionRef("microtus")
            library("microtus-core", "net.onelitefeather.microtus", "Microtus").version("1.5.0-SNAPSHOT")
            library("microtus-test", "net.onelitefeather.microtus.testing", "testing").version("1.5.0-SNAPSHOT")
            library("junit-jupiter", "org.junit.jupiter", "junit-jupiter").versionRef("junit")
            library("junit-jupiter-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef("junit")
            library("mockito-core", "org.mockito", "mockito-core").version("5.12.0")
            library("mockito-junit", "org.mockito", "mockito-junit-jupiter").version("5.12.0")
            library("aves", "de.icevizion.lib", "aves").version("1.6.0-SNAPSHOT")
            library("xerus", "net.theevilreaper.xerus", "xerus").version("1.3.0-SNAPSHOT")
            library("canis", "com.github.theEvilReaper", "Canis").version("master-SNAPSHOT")
            library("mini", "net.kyori", "adventure-text-minimessage").version("4.17.0")
            val cloudnetBaseGroup = "eu.cloudnetservice.cloudnet"
            library("cloudnet-wrapper", cloudnetBaseGroup, "wrapper-jvm").version(cloudNetVersion)
            library("cloudnet-bridge", cloudnetBaseGroup, "bridge").version(cloudNetVersion)
            library("cloudnet-driver", cloudnetBaseGroup, "driver").version(cloudNetVersion)
            bundle("cloudnet", listOf("cloudnet-wrapper", "cloudnet-bridge", "cloudnet-driver"))
        }
    }
}