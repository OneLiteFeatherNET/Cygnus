plugins {
    `maven-publish`
    alias(libs.plugins.shadow)
    alias(libs.plugins.publishdata)
    application
}

application {
    mainClass.set("net.onelitefeather.cygnus.CygnusLoader")
}

dependencies {
    implementation(platform(libs.mycelium.bom))
    implementation(platform(libs.aonyx.bom))
    implementation(project(":common"))
    implementation(libs.minestom)
    implementation(libs.aves)
    implementation(libs.xerus)
  //  implementation(libs.canis)

    //CloudNet
    implementation(platform(libs.cloudnet.bom))
    implementation(libs.bundles.cloudnet)

    testImplementation(libs.minestom)
    testImplementation(libs.adventure)
    testImplementation(libs.cyano)
    testImplementation(libs.aves)
    testImplementation(libs.xerus)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.engine)
}

tasks {
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
