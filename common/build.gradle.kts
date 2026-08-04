plugins {
    id("cygnus.java-conventions")
    `java-library`
}

dependencies {
    implementation(platform(libs.aonyx.bom))
    implementation(libs.slf4j.api)
    api(libs.adventure)
    compileOnly(libs.minestom)
    compileOnly(libs.aves)
    compileOnly(libs.xerus)
    compileOnly(libs.luckperms.api) {
        exclude(group = "net.kyori.adventure")
    }

    testImplementation(libs.minestom)
    testImplementation(libs.cyano)
    testImplementation(libs.aves)
    testImplementation(libs.xerus)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.engine)
}
