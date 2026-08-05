plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    id("io.ktor.plugin") version "3.1.1" // Updated to latest version
    application
}

group = "com.wellnessapp"
version = "0.0.1"

// Add this block to set JVM target
kotlin {
    jvmToolchain(21) // or 17, 21 - choose a stable version
}

application {
    mainClass.set("com.wellnessapp.ApplicationKt")
}

repositories {
    mavenCentral()
}

val exposedVersion = "0.55.0"

dependencies {
    // Ktor server
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-call-logging")
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-auth")
    implementation("io.ktor:ktor-server-auth-jwt")
    implementation("io.ktor:ktor-server-rate-limit")

    // Exposed ORM
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")

    // Postgres driver + connection pool
    implementation("org.postgresql:postgresql:42.7.4")
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Firebase Admin SDK (verifies Google/Email ID tokens)
    implementation("com.google.firebase:firebase-admin:9.4.1")

    // JWT (for our own access/refresh tokens)
    implementation("com.auth0:java-jwt:4.4.0")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.5.6")

    // Env vars
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")

    testImplementation("io.ktor:ktor-server-tests")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}
