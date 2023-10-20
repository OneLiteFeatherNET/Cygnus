plugins {
    id("java")
    application
}

group = "net.onelitefeather"
version = "0.0.1-SNAPSHOT" // Change

repositories {
    mavenCentral()
}

dependencies {
    implementation("info.picocli:picocli:4.7.5")
    annotationProcessor("info.picocli:picocli-codegen:4.7.5")
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks {
    compileJava {
        options.release.set(17)
        options.encoding = "UTF-8"
    }
}

application {
    mainClass = "net.onelitefeather.mergemaestro.ApplicationEntry"
}
