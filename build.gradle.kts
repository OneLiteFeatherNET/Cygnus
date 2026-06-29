plugins {
    alias(libs.plugins.cyclonedx)
}

allprojects {
    version = (version as String).substringBefore('#').trim()
}
