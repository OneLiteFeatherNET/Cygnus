subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")

    tasks {
        getByName<JavaCompile>("compileJava") {
            options.release.set(24)
            options.encoding = "UTF-8"
        }
        getByName<JacocoReport>("jacocoTestReport") {
            dependsOn(project.tasks.named("test"))
            reports {
                xml.required.set(true)
            }
        }
        getByName<Test>("test") {
            jvmArgs("-Dminestom.inside-test=true")
            finalizedBy(project.tasks.named("jacocoTestReport"))
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
            }
        }

        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(24))
            }
        }
    }
}