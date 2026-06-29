plugins {
    alias(libs.plugins.cyclonedx)
}

allprojects {
    group = "net.onelitefeather"
    version = providers.gradleProperty("version").get().substringBefore("#").trim()
}