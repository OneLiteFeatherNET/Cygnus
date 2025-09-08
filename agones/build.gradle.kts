plugins {
    java
    `java-library`
}

dependencies {
    api(libs.agones4j)
    api(libs.grpc.stub)
    api(libs.grpc.protobuf)
    api(libs.grpc.netty)
    api(libs.grpc.okhttp)
    api(libs.tomcat.annotations.api)
    api("com.google.protobuf:protobuf-java:4.32.0")
}