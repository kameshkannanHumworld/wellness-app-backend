package com.wellnessapp.common.security

import com.wellnessapp.config.EnvConfig
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

const val AUTH_JWT = "auth-jwt"

fun Application.configureJwtAuth() {
    install(Authentication) {
        jwt(AUTH_JWT) {
            verifier(JwtService.verifier())
            validate { credential ->
                val userId = credential.payload.getClaim("userId").asString()
                val type = credential.payload.getClaim("type").asString()
                if (userId != null && type == "access") JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                throw com.wellnessapp.common.exception.UnauthorizedException("Missing or invalid access token")
            }
        }
    }
}

/** Pulls the authenticated user's UUID out of the JWT principal inside a route handler. */
fun io.ktor.server.application.ApplicationCall.currentUserId(): java.util.UUID {
    val principal = this.principal<JWTPrincipal>()
        ?: throw com.wellnessapp.common.exception.UnauthorizedException()
    val userId = principal.payload.getClaim("userId").asString()
        ?: throw com.wellnessapp.common.exception.UnauthorizedException()
    return java.util.UUID.fromString(userId)
}
