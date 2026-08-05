package com.wellnessapp.auth.routes

import com.wellnessapp.auth.dto.FirebaseTokenRequest
import com.wellnessapp.auth.service.AuthService
import com.wellnessapp.common.response.ApiSuccess
import com.wellnessapp.common.security.AUTH_JWT
import com.wellnessapp.common.security.currentUserId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.authRoutes() {
    route("/api/v1/auth") {

        // Android app signs in with Google via Firebase, then sends us the ID token here.
        post("/google") {
            val body = call.receive<FirebaseTokenRequest>()
            val result = AuthService.authenticateWithFirebaseToken(body.firebaseToken)
            call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
        }

        // Email/password: Android app authenticates with Firebase client SDK first,
        // then sends us the resulting ID token — same verification path as Google.
        post("/login") {
            val body = call.receive<FirebaseTokenRequest>()
            val result = AuthService.authenticateWithFirebaseToken(body.firebaseToken)
            call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
        }

        post("/signup") {
            // Signup happens client-side via Firebase Auth SDK (createUserWithEmailAndPassword).
            // The app then calls /login with the resulting ID token to get our JWT + create the row.
            val body = call.receive<FirebaseTokenRequest>()
            val result = AuthService.authenticateWithFirebaseToken(body.firebaseToken)
            call.respond(HttpStatusCode.Created, ApiSuccess(data = result))
        }

        authenticate(AUTH_JWT) {
            get("/session") {
                val userId = call.currentUserId()
                call.respond(HttpStatusCode.OK, ApiSuccess(data = AuthService.getSession(userId)))
            }

            post("/refresh") {
                val userId = call.currentUserId()
                call.respond(HttpStatusCode.OK, ApiSuccess(data = AuthService.refresh(userId)))
            }

            post("/logout") {
                // Stateless JWT: logout is handled client-side by discarding tokens.
                // (Optional upgrade: maintain a refresh-token denylist table.)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = mapOf("loggedOut" to true)))
            }
        }
    }
}
