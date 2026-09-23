# Build recipe for hosts that run Java through Docker (Render). You do not need
# Docker installed locally: the host reads this file and builds the image itself.

# Stage 1: compile and package the jar with Maven. Tests are skipped here because
# the build machine has no database; run them locally (and later in CI) instead.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# Stage 2: run the jar on a small Java runtime without Maven or the source code.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Free instances have about 512 MB of memory; let the JVM use most of it for the
# heap instead of its conservative default of a quarter.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"

# Documentation only: the app listens on $PORT, which the host provides.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
