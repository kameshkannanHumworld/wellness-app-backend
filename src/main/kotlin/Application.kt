package com.wellnessapp

import com.wellnessapp.auth.routes.authRoutes
import com.wellnessapp.onboarding.routes.onboardingRoutes
import com.wellnessapp.hydration.routes.hydrationRoutes
import com.wellnessapp.activity.routes.activityRoutes
import com.wellnessapp.bloodpressure.routes.bloodPressureRoutes
import com.wellnessapp.dashboard.routes.dashboardRoutes
import com.wellnessapp.common.exception.registerExceptionHandling
import com.wellnessapp.common.security.configureJwtAuth
import com.wellnessapp.config.DatabaseFactory
import com.wellnessapp.config.EnvConfig
import com.wellnessapp.config.FirebaseConfig
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main() {
    // Order matters: config first, then external services, then the server.
    FirebaseConfig.init()
    DatabaseFactory.init()

    embeddedServer(Netty, port = EnvConfig.port, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(CallLogging)

    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; prettyPrint = false })
    }

    install(CORS) {
        anyHost() // tighten this to your app's origin before production launch
        allowHeader(io.ktor.http.HttpHeaders.Authorization)
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
    }

    install(StatusPages) {
        registerExceptionHandling()
    }

    configureJwtAuth()

    routing {
        get("/health") { call.respondText("Deployed successfully") }
        authRoutes()
        onboardingRoutes()
        hydrationRoutes()
        activityRoutes()
        bloodPressureRoutes()
        dashboardRoutes()
        // reminderRoutes() — added next, following this same pattern.
    }
}
