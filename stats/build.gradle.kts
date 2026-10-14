plugins {
    id("cygnus.java-conventions")
    `java-library`
}

dependencies {
    implementation(platform(libs.aonyx.bom))
    implementation(libs.slf4j.api)
    compileOnly(libs.jetbrains.annotations)

    // Tracing API only, for the write-failure counter. The javaagent attached in production supplies
    // the SDK; :game bundles the same artifact, so the real io.opentelemetry packages stay unrelocated.
    implementation(libs.opentelemetry.api)

    // Persistence of the yearly statistics. Only opened when stats.jdbc-url is configured, see
    // StatsRepositories. Hibernate runs on a Hikari pool, Flyway owns the schema (the migrations live
    // in this module's resources), PostgreSQL is the production database.
    implementation(libs.hibernate.core)
    implementation(libs.hikari)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)

    testCompileOnly(libs.jetbrains.annotations)
    // H2 in PostgreSQL mode backs the repository tests.
    testImplementation(libs.h2)
    testImplementation(libs.opentelemetry.sdk)
    testImplementation(libs.opentelemetry.sdk.testing)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly(libs.slf4j.simple)
}
