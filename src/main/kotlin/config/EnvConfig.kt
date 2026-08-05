package com.wellnessapp.config

import io.github.cdimascio.dotenv.dotenv

/**
 * Loads config from environment variables.
 * Locally: falls back to a .env file (gitignored) if present.
 * In production (Railway/Render): reads real environment variables — no file needed.
 */
object EnvConfig {
    private val dotenv = dotenv {
        ignoreIfMissing = true // fine in prod where env vars are injected directly
    }

    private fun get(key: String): String =
        System.getenv(key) ?: dotenv[key] ?: error("Missing required env var: $key")

    private fun getOrNull(key: String): String? =
        System.getenv(key) ?: dotenv[key]

    // Database
    val databaseUrl: String get() = get("DATABASE_URL")
    val databaseUser: String get() = get("DATABASE_USER")
    val databasePassword: String get() = get("DATABASE_PASSWORD")

    // Firebase — pulled from env, never from a checked-in JSON file
    val firebaseProjectId: String get() = get("FIREBASE_PROJECT_ID")
    val firebaseClientId: String get() = get("FIREBASE_CLIENT_ID")
    val firebaseClientEmail: String get() = get("FIREBASE_CLIENT_EMAIL")
    val firebasePrivateKeyId: String get() = get("FIREBASE_PRIVATE_KEY_ID")
    val firebasePrivateKey: String get() = get("FIREBASE_PRIVATE_KEY").replace("\\n", "\n")

    // JWT
    val jwtSecret: String get() = get("JWT_SECRET")
    val jwtIssuer: String get() = get("JWT_ISSUER")
    val jwtAudience: String get() = get("JWT_AUDIENCE")
    val jwtAccessExpiryMinutes: Long get() = (getOrNull("JWT_ACCESS_TOKEN_EXPIRY_MINUTES") ?: "15").toLong()
    val jwtRefreshExpiryDays: Long get() = (getOrNull("JWT_REFRESH_TOKEN_EXPIRY_DAYS") ?: "30").toLong()

    // Server
    val port: Int get() = (getOrNull("PORT") ?: "8080").toInt()
}
