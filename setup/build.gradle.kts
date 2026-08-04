import java.util.zip.ZipFile

plugins {
    id("cygnus.java-conventions")
    `maven-publish`
    alias(libs.plugins.shadow)
    application
}

application {
    mainClass.set("net.onelitefeather.cygnus.setup.SetupLoader")
}

dependencies {
    implementation(platform(libs.aonyx.bom))
    implementation(platform(libs.falco.bom))
    implementation(project(":common"))
    implementation(libs.slf4j.api)
    implementation(libs.minestom)
    implementation(libs.aves)
    implementation(libs.xerus)
    implementation(libs.adventure)
    implementation(libs.pica)
    implementation(libs.guira)
    implementation(libs.falco.anvil)

    // SLF4J needs a binding at runtime; without one it falls back to NOP and the
    // server logs nothing at all.
    runtimeOnly(libs.slf4j.simple)

    // CloudNet is provided by the CloudNet wrapper at runtime and its bridge is loaded as a
    // Minestom extension (separate classloader, see the :bridge module), so :setup neither
    // references nor bundles any CloudNet artifact.
    implementation(libs.minestom.ce.extensions)
    implementation(libs.kotlin.stdlib.jdk8)

    // LuckPerms
    implementation(libs.guava)
    compileOnly(libs.luckperms.api) {
        exclude(group = "net.kyori.adventure")
    }
    runtimeOnly(libs.luckperms.minestom.loader) {
        exclude(group = "net.kyori.adventure")
    }

    testImplementation(libs.minestom)
    testImplementation(libs.cyano)
    testImplementation(libs.xerus)
    testImplementation(libs.aves)
    testImplementation(libs.junit.api)
    testImplementation(libs.junit.params)
    testImplementation(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.engine)
}

// Keeps the loader off the test class path, which is what makes LuckPermsSupport report absent and
// every permission check answer TRUE during tests.
configurations.testRuntimeClasspath {
    exclude(group = "net.luckperms", module = "minestom-loader")
}
tasks {
    jar {
        archiveClassifier.set("unshaded")
        dependsOn("shadowJar")
    }

    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("setup.jar")
        mergeServiceFiles()
        // Shaded deps ship signed and multi-release jars that break a relocation-free
        // application fat jar; drop signatures and module-info.
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
        exclude("module-info.class", "META-INF/versions/**/module-info.class")
    }
}

// Guards the fat jar against silently shipping in "every permission is TRUE" mode: before this
// branch a build without the loader died instantly with NoClassDefFoundError, so a dropped
// runtimeOnly(libs.luckperms.minestom.loader) line or an over-wide shadowJar exclude has to fail
// the build just as loudly now that the fallback boots successfully instead of crashing.
val verifyLuckPermsLoaderBundled = tasks.register("verifyLuckPermsLoaderBundled") {
    group = "verification"
    description = "Fails the build when setup.jar does not bundle the LuckPerms Minestom loader."
    dependsOn(tasks.shadowJar)
    val archiveFile = tasks.shadowJar.flatMap { it.archiveFile }
    inputs.file(archiveFile)
    doLast {
        val jarFile = archiveFile.get().asFile
        val requiredEntry = "me/lucko/luckperms/minestom/loader/MinestomLoader.class"
        val bundled = ZipFile(jarFile).use { zip -> zip.getEntry(requiredEntry) != null }
        if (!bundled) {
            throw GradleException(
                "setup.jar is missing $requiredEntry. Without the LuckPerms Minestom loader bundled, " +
                    "the shipped jar boots in fallback mode where every permission check silently " +
                    "resolves to TRUE for every player. Check that runtimeOnly(libs.luckperms.minestom.loader) " +
                    "is still declared in setup/build.gradle.kts and that no shadowJar exclude filters it out."
            )
        }
    }
}

tasks.named("check") {
    dependsOn(verifyLuckPermsLoaderBundled)
}

publishing {
    repositories {
        maven {
            authentication {
                credentials(PasswordCredentials::class) {
                    // Those credentials need to be set under "Settings -> Secrets -> Actions" in your repository
                    username = System.getenv("ONELITEFEATHER_MAVEN_USERNAME")
                    password = System.getenv("ONELITEFEATHER_MAVEN_PASSWORD")
                }
            }
            name = "OneLiteFeatherRepository"
            url = if (project.version.toString().contains("SNAPSHOT")) {
                uri("https://repo.onelitefeather.dev/onelitefeather-snapshots")
            } else {
                uri("https://repo.onelitefeather.dev/onelitefeather-releases")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            artifact(project.tasks.getByName("shadowJar"))
            version = rootProject.version as String
            artifactId = "cygnus-setup"
            groupId = rootProject.group as String
            pom {
                description.set("Setup component of the Cygnus MiniGame Platform")
                name = "Cygnus Setup Component"
                url = "https://github.com/OneLiteFeatherNET/Cygnus"
                licenses {
                    license {
                        name = "AGPL-3.0 License"
                        url = "https://www.gnu.org/licenses/agpl-3.0.en.html"
                    }
                }
                developers {
                    developer {
                        name.set("OneliteFeather")
                        contributors {
                            contributor {
                                name.set("theEvilReaper")
                            }
                            contributor {
                                name.set("TheMeinerLP")
                            }
                        }
                    }
                }

                issueManagement {
                    system.set("Github")
                    url.set("https://github.com/OneLiteFeatherNET/Cygnus/issues")
                }

                scm {
                    connection = "scm:git:git://github.com:OneLiteFeatherNET/Cygnus.git"
                    developerConnection = "scm:git:ssh://git@github.com:OneLiteFeatherNET/Cygnus.git"
                    url = "https://github.com/OneLiteFeatherNET/Cygnus"
                }
            }
        }
    }
}