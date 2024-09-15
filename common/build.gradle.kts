plugins {
    java
    `java-library`
    jacoco
}

group = "net.onelitefeather"
version = "1.0.1"

repositories {
    mavenLocal()
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    mavenCentral()
    maven("https://jitpack.io")
    maven {
        val groupdId = 28 // Gitlab Group
        url = uri("https://gitlab.onelitefeather.dev/api/v4/groups/$groupdId/-/packages/maven")
        name = "GitLab"
        credentials(HttpHeaderCredentials::class.java) {
            name = "Private-Token"
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
    implementation(platform(libs.microtus.bom))
    implementation(platform(libs.dungeon.bom))
    compileOnly(libs.minestom)
    compileOnly(libs.aves)
    compileOnly(libs.xerus)
    compileOnly(libs.canis)

    compileOnly(libs.bundles.cloudnet)

    testImplementation(libs.minestom)
    testImplementation(libs.minestom.test)
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
}
