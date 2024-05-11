rootProject.name = "Cygnus"
val cloudNetVersion = "4.0.0-RC9"
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("minestom","1.3.1")
            version("junit", "5.10.2")
            library("minestom", "net.onelitefeather.microtus", "Minestom").versionRef("minestom")
            library("minestom-test", "net.onelitefeather.microtus.testing", "testing").versionRef("minestom")
            library("junit-jupiter", "org.junit.jupiter", "junit-jupiter").versionRef("junit")
            library("junit-jupiter-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef("junit")
            library("mockito-core", "org.mockito", "mockito-core").version("5.12.0")
            library("mockito-junit", "org.mockito", "mockito-junit-jupiter").version("5.12.0")
            library("aves", "de.icevizion.lib", "Aves").version("1.3.0+f7b17be8")
            library("xerus", "net.theevilreaper.xerus", "Xerus").version("1.2.0-SNAPSHOT+0971da3a")
            library("canis", "com.github.theEvilReaper", "Canis").version("master-SNAPSHOT")
            library("mini", "net.kyori", "adventure-text-minimessage").version("4.14.0")
            val cloudnetBaseGroup = "eu.cloudnetservice.cloudnet"
            library("cloudnet-wrapper", cloudnetBaseGroup, "wrapper-jvm").version(cloudNetVersion)
            library("cloudnet-bridge", cloudnetBaseGroup, "bridge").version(cloudNetVersion)
            library("cloudnet-driver", cloudnetBaseGroup, "driver").version(cloudNetVersion)
            bundle("cloudnet", listOf("cloudnet-wrapper", "cloudnet-bridge", "cloudnet-driver"))
        }
    }
}