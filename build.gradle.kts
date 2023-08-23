plugins {
    id("java")
}

group = "net.onelitefeather"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
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

