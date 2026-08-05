package com.wellnessapp.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

/**
 * Initializes Firebase Admin from individual env vars (project id / client email / private key)
 * rather than a checked-in service-account JSON file. Keeps the real key out of the repo.
 */
object FirebaseConfig {
    fun init() {
        if (FirebaseApp.getApps().isNotEmpty()) return

        // Build the same JSON shape Firebase expects, purely in-memory from env vars.
        val credentialsJson = """
            {
              "type": "service_account",
              "project_id": "${EnvConfig.firebaseProjectId}",
              "client_id": "${EnvConfig.firebaseClientId}",
              "client_email": "${EnvConfig.firebaseClientEmail}",
              "private_key_id": "${EnvConfig.firebasePrivateKeyId}",
              "private_key": "${EnvConfig.firebasePrivateKey.replace("\n", "\\n")}",
              "token_uri": "https://oauth2.googleapis.com/token"
            }
        """.trimIndent()

        val credentials: GoogleCredentials = ServiceAccountCredentials.fromStream(
            ByteArrayInputStream(credentialsJson.toByteArray(StandardCharsets.UTF_8))
        )

        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .setProjectId(EnvConfig.firebaseProjectId)
            .build()

        FirebaseApp.initializeApp(options)
    }
}
