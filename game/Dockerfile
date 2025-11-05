FROM docker.io/eclipse-temurin:25-jdk-alpine
LABEL maintainer="theEvilReaper"
LABEL description="Docker container for the Cygnus game"
LABEL type="Microtus"
LABEL game="Cygnus-Game"

RUN mkdir /app
WORKDIR /app

ARG JAR_FILE
COPY ${JAR_FILE} /app/app.jar

ARG MAPS
ADD ${MAPS} /app/maps

ENTRYPOINT ["java", "-jar", "app.jar"]