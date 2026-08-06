package com.wellnessapp.bloodpressure.routes

import com.wellnessapp.bloodpressure.dto.BloodPressureLogRequest
import com.wellnessapp.bloodpressure.service.BloodPressureService
import com.wellnessapp.common.exception.ValidationException
import com.wellnessapp.common.response.ApiSuccess
import com.wellnessapp.common.security.AUTH_JWT
import com.wellnessapp.common.security.currentUserId
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.bloodPressureRoutes() {
    route("/api/v1/blood-pressure") {
        authenticate(AUTH_JWT) {

            // Log a single BP reading.
            post {
                val userId = call.currentUserId()
                val body = call.receive<BloodPressureLogRequest>()
                val result = BloodPressureService.logReading(userId, body)
                call.respond(HttpStatusCode.Created, ApiSuccess(data = result))
            }

            // List this user's readings, newest first. Optional ?date=YYYY-MM-DD (single day),
            // ?limit=N (default 100, max 500).
            get {
                val userId = call.currentUserId()
                val date = call.request.queryParameters["date"]
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 500) ?: 100
                val result = BloodPressureService.listReadings(userId, date, limit)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Most recent reading — for the dashboard's "current BP" card.
            get("/latest") {
                val userId = call.currentUserId()
                val result = BloodPressureService.getLatest(userId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Last 7 days (today + previous 6), per-day averages, for the BP chart screen.
            get("/weekly") {
                val userId = call.currentUserId()
                val result = BloodPressureService.weeklySummary(userId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            get("/{id}") {
                val userId = call.currentUserId()
                val readingId = call.parameters.parseUuid("id")
                val result = BloodPressureService.getReading(userId, readingId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Correct a previously logged reading (full replace).
            put("/{id}") {
                val userId = call.currentUserId()
                val readingId = call.parameters.parseUuid("id")
                val body = call.receive<BloodPressureLogRequest>()
                val result = BloodPressureService.updateReading(userId, readingId, body)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            delete("/{id}") {
                val userId = call.currentUserId()
                val readingId = call.parameters.parseUuid("id")
                BloodPressureService.deleteReading(userId, readingId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = mapOf("deleted" to true)))
            }
        }
    }
}

private fun Parameters.parseUuid(name: String): UUID =
    this[name]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        ?: throw ValidationException(listOf(name to "Must be a valid UUID"))
