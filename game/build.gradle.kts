plugins {
    `maven-publish`
    alias(libs.plugins.shadow)
    alias(libs.plugins.publishdata)
    application
}

group = "net.onelitefeather.cygnus"
version = "1.0.1"

application {
    mainClass.set("net.onelitefeather.cygnus.CygnusLoader")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(platform(libs.microtus.bom))
    implementation(platform(libs.dungeon.bom))
    implementation(project(":common"))
    implementation(project(":agones"))
    implementation(libs.minestom)
    implementation(libs.aves)
    implementation(libs.xerus)
    implementation(libs.canis)

    testImplementation(libs.minestom)
    testImplementation(libs.minestom.test)
    testImplementation(libs.aves)
    testImplementation(libs.xerus)
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
        jvmArgs("-Dminestom.inside-test=true")
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    jar {
        archiveClassifier.set("unshaded")
        dependsOn("shadowJar")
    }

    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("cygnus.jar")
        mergeServiceFiles()
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
            // Get the detected repository from the publish data
            url = uri(publishData.getRepository())
        }
    }
}

tasks {
    jar {
        dependsOn("shadowJar")
    }
}
