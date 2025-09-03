FROM docker.io/eclipse-temurin:23-jdk-alpine
LABEL maintainer="theEvilReaper"
LABEL description="Docker container for the Cygnus setup"
LABEL type="Microtus"
LABEL game="Cygnus-Setup"

RUN mkdir /app
WORKDIR /app

ARG JAR_FILE
COPY ${JAR_FILE} /app/app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]