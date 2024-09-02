plugins {
    java
    jacoco
    id("io.github.goooler.shadow") version "8.1.7"
    alias(libs.plugins.publishdata)
    `maven-publish`

}

group = "net.onelitefeather"
version = "1.0.1-SNAPSHOT" // Change

repositories {
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    mavenCentral()
    maven("https://jitpack.io")
    maven {
        val groupdId = 28 // Gitlab Group
        url = uri("https://gitlab.onelitefeather.dev/api/v4/groups/$groupdId/-/packages/maven")
        name = "GitLab"
        credentials(HttpHeaderCredentials::class.java) {
            name =  "Private-Token"
            value = providers.gradleProperty("gitLabPrivateToken").getOrElse("")
        }
        authentication {
            create<HttpHeaderAuthentication>("header")
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(libs.mini)
    implementation(platform(libs.microtus.bom))
    compileOnly(libs.microtus.core)
    compileOnly(libs.aves)
    compileOnly(libs.xerus)
    //compileOnly(libs.canis)

    compileOnly(libs.bundles.cloudnet)

    testImplementation(libs.microtus.core)
    testImplementation(libs.microtus.test)
    testImplementation(libs.xerus)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    jacocoTestReport {
        dependsOn(rootProject.tasks.test)
        reports {
            xml.required.set(true)
        }
    }

    test {
        finalizedBy(rootProject.tasks.jacocoTestReport)
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    jar {
        dependsOn("shadowJar")
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


