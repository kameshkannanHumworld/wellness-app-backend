package com.wellnessapp.hydration.routes

import com.wellnessapp.common.exception.ValidationException
import com.wellnessapp.common.response.ApiSuccess
import com.wellnessapp.common.security.AUTH_JWT
import com.wellnessapp.common.security.currentUserId
import com.wellnessapp.hydration.dto.WaterLogRequest
import com.wellnessapp.hydration.service.HydrationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.hydrationRoutes() {
    route("/api/v1/water") {
        authenticate(AUTH_JWT) {

            // Log a single water intake entry, e.g. after the user taps "+250ml".
            post {
                val userId = call.currentUserId()
                val body = call.receive<WaterLogRequest>()
                val result = HydrationService.logWater(userId, body)
                call.respond(HttpStatusCode.Created, ApiSuccess(data = result))
            }

            // List this user's logs, newest first. Optional ?date=YYYY-MM-DD to filter a single day,
            // ?limit=N (default 100, max 500).
            get {
                val userId = call.currentUserId()
                val date = call.request.queryParameters["date"]
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 500) ?: 100
                val result = HydrationService.listLogs(userId, date, limit)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Last 7 days of totals (today + previous 6, zero-filled) for the hydration chart screen,
            // plus the user's daily goal from onboarding if they set one.
            get("/weekly") {
                val userId = call.currentUserId()
                val result = HydrationService.weeklySummary(userId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            get("/{id}") {
                val userId = call.currentUserId()
                val logId = call.parameters.parseUuid("id")
                val result = HydrationService.getLog(userId, logId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Correct a previously logged entry (e.g. user mis-tapped the amount).
            put("/{id}") {
                val userId = call.currentUserId()
                val logId = call.parameters.parseUuid("id")
                val body = call.receive<WaterLogRequest>()
                val result = HydrationService.updateLog(userId, logId, body)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            delete("/{id}") {
                val userId = call.currentUserId()
                val logId = call.parameters.parseUuid("id")
                HydrationService.deleteLog(userId, logId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = mapOf("deleted" to true)))
            }
        }
    }
}

private fun Parameters.parseUuid(name: String): UUID =
    this[name]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        ?: throw ValidationException(listOf(name to "Must be a valid UUID"))
