import org.gradle.kotlin.dsl.test

plugins {
    java
    jacoco
    `maven-publish`
    alias(libs.plugins.shadow)
    alias(libs.plugins.publishdata)
}

group = "net.onelitefeather"
version = "1.0.1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(platform(libs.microtus.bom))
    implementation(platform(libs.dungeon.bom))
    implementation(project(":common"))
    compileOnly(libs.minestom)
    compileOnly(libs.aves)
    compileOnly(libs.xerus)
    compileOnly(libs.canis)

    compileOnly(libs.bundles.cloudnet)

    testImplementation(libs.minestom)
    testImplementation(libs.minestom.test)
    testImplementation(libs.xerus)
    testImplementation(libs.aves)
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    jacocoTestReport {
        dependsOn(test)
        reports {
            xml.required.set(true)
        }
    }

    test {
        finalizedBy(project.tasks.jacocoTestReport)
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

publishData {
    addBuildData()
    useGitlabReposForProject("245", "https://gitlab.onelitefeather.dev/")
    publishTask("shadowJar")
}

publishing {
    publications.create<MavenPublication>("maven") {
        // configure the publication as defined previously.
        publishData.configurePublication(this)
        version = publishData.getVersion(false)
    }

    repositories {
        maven {
            credentials(HttpHeaderCredentials::class) {
                name = "Job-Token"
                value = System.getenv("CI_JOB_TOKEN")
            }
            authentication {
                create("header", HttpHeaderAuthentication::class)
            }


            name = "Gitlab"
            // Get the detected repository from the publishing data
            url = uri(publishData.getRepository())
        }
    }
}