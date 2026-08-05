package com.wellnessapp.common.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.wellnessapp.config.EnvConfig
import java.util.*

object JwtService {

    private val algorithm = Algorithm.HMAC256(EnvConfig.jwtSecret)

    fun generateAccessToken(userId: String): String =
        JWT.create()
            .withIssuer(EnvConfig.jwtIssuer)
            .withAudience(EnvConfig.jwtAudience)
            .withClaim("userId", userId)
            .withClaim("type", "access")
            .withExpiresAt(Date(System.currentTimeMillis() + EnvConfig.jwtAccessExpiryMinutes * 60_000))
            .sign(algorithm)

    fun generateRefreshToken(userId: String): String =
        JWT.create()
            .withIssuer(EnvConfig.jwtIssuer)
            .withAudience(EnvConfig.jwtAudience)
            .withClaim("userId", userId)
            .withClaim("type", "refresh")
            .withExpiresAt(Date(System.currentTimeMillis() + EnvConfig.jwtRefreshExpiryDays * 24 * 60 * 60_000))
            .sign(algorithm)

    /** Used by the auth-verifier Ktor plugin sets up in Application.kt */
    fun verifier() = JWT.require(algorithm)
        .withIssuer(EnvConfig.jwtIssuer)
        .withAudience(EnvConfig.jwtAudience)
        .build()
}
