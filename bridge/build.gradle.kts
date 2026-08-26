plugins {
    id("cygnus.java-conventions")
    `maven-publish`
}

// Minestom extension that bridges CloudNet permission checks to LuckPerms. It is packaged as a
// standalone extension jar (dropped into a CloudNet service's extensions/ folder next to the
// CloudNet bridge) and never bundled into a fat jar. Everything it compiles against is provided at
// runtime: the CloudNet driver by the CloudNet wrapper, the bridge by the CloudNet_Bridge
// extension, Minestom and Adventure by the application classloader.
dependencies {
    annotationProcessor(platform(libs.minestom.extensions.bom))
    annotationProcessor(libs.minestom.extensions.processor)

    compileOnly(platform(libs.aonyx.bom))
    compileOnly(libs.minestom)
    compileOnly(libs.adventure)
    compileOnly(platform(libs.minestom.extensions.bom))
    compileOnly(libs.minestom.extensions)
    compileOnly(libs.minestom.extensions.processor)

    compileOnly(platform(libs.cloudnet.bom))
    compileOnly(libs.cloudnet.driver.api)
    compileOnly(libs.cloudnet.bridge)
    compileOnly(libs.cloudnet.bridge.impl)
}

// The annotation processor generates extension.json but cannot know the project version. Subprojects
// do not inherit the root version, so read it from the root project - the same source the
// publications use.
tasks.compileJava {
    options.compilerArgs.add("-Aminestom.extension.version=${rootProject.version}")
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
            url = if (rootProject.version.toString().contains("SNAPSHOT")) {
                uri("https://repo.onelitefeather.dev/onelitefeather-snapshots")
            } else {
                uri("https://repo.onelitefeather.dev/onelitefeather-releases")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            artifact(project.tasks.getByName("jar"))
            version = rootProject.version as String
            artifactId = "cygnus-bridge"
            groupId = rootProject.group as String
            pom {
                description.set("CloudNet bridge extension that resolves permissions through LuckPerms")
                name = "Cygnus Bridge Component"
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
