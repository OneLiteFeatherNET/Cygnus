plugins {
    `java-library`
}

group = "net.onelitefeather.cygnus"
version = "1.0.1"

dependencies {
    implementation(platform(libs.mycelium.bom))
    implementation(platform(libs.aonyx.bom))
    compileOnly(libs.adventure)
    compileOnly(libs.minestom)
    compileOnly(libs.aves)
    compileOnly(libs.xerus)

    testImplementation(libs.minestom)
    testImplementation(libs.cyano)
    testImplementation(libs.aves)
    testImplementation(libs.xerus)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.engine)
}
