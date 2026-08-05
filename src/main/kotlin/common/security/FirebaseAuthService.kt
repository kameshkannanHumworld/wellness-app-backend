package com.wellnessapp.common.security

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import com.wellnessapp.common.exception.UnauthorizedException

data class VerifiedFirebaseUser(
    val firebaseUid: String,
    val email: String?,
    val fullName: String?,
    val photoUrl: String?,
    val provider: String // google | password
)

object FirebaseAuthService {

    /**
     * Verifies the Firebase ID token sent by the Android app after Google/Email sign-in.
     * Throws UnauthorizedException if the token is invalid, expired, or revoked.
     */
    fun verifyIdToken(idToken: String): VerifiedFirebaseUser {
        val decoded: FirebaseToken = try {
            FirebaseAuth.getInstance().verifyIdToken(idToken, true)
        } catch (e: Exception) {
            throw UnauthorizedException("Invalid or expired Firebase token")
        }

        val signInProvider = decoded.claims["firebase"]
            ?.let { it as? Map<*, *> }
            ?.get("sign_in_provider") as? String

        val provider = when (signInProvider) {
            "google.com" -> "google"
            "password" -> "email"
            "apple.com" -> "apple"
            else -> "email"
        }

        return VerifiedFirebaseUser(
            firebaseUid = decoded.uid,
            email = decoded.email,
            fullName = decoded.name,
            photoUrl = decoded.picture,
            provider = provider
        )
    }
}
