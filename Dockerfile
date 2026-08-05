# ---------- Build stage ----------
FROM gradle:8.10-jdk21 AS build

WORKDIR /app

# Copy build files first so Gradle deps are cached across rebuilds
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

# Builds a fat/shadow-style jar via the Ktor Gradle plugin's buildFatJar task
RUN gradle buildFatJar --no-daemon

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy only the built jar from the build stage - keeps the final image small
COPY --from=build /app/build/libs/*-all.jar app.jar

# Render sets $PORT at runtime; EnvConfig.kt already reads PORT from env
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]