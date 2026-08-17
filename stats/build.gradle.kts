plugins {
    id("cygnus.java-conventions")
    `java-library`
}

dependencies {
    implementation(platform(libs.aonyx.bom))
    implementation(libs.slf4j.api)
    compileOnly(libs.jetbrains.annotations)
    implementation(libs.hibernate.core)
    implementation(libs.hibernate.hikaricp)
    implementation(libs.hikaricp)
    implementation(libs.postgresql.driver)
    implementation(libs.liquibase.core)

    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.h2)
    testRuntimeOnly(libs.junit.engine)
}
