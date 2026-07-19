plugins {
    java
    jacoco
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

tasks.withType<Test>().configureEach {
    jvmArgs("-Dminestom.inside-test=true")
    finalizedBy(tasks.matching { it.name == "jacocoTestReport" })
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<JacocoReport>().configureEach {
    dependsOn(tasks.matching { it.name == "test" })
    reports {
        xml.required.set(true)
    }
}
