plugins {
    `maven-publish`
    alias(libs.plugins.shadow)
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

tasks {
    jar {
        dependsOn("shadowJar")
    }
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
            artifactId = "cygnus-game"
            groupId = rootProject.group as String
            pom {
                description.set("Game component of the Cygnus MiniGame Platform")
                name = "Cygnus Game Component"
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

