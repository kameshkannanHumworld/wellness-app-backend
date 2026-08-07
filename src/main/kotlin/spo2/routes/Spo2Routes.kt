package com.wellnessapp.spo2.routes

import com.wellnessapp.common.exception.ValidationException
import com.wellnessapp.common.response.ApiSuccess
import com.wellnessapp.common.security.AUTH_JWT
import com.wellnessapp.common.security.currentUserId
import com.wellnessapp.spo2.dto.Spo2LogRequest
import com.wellnessapp.spo2.service.Spo2Service
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.spo2Routes() {
    route("/api/v1/spo2") {
        authenticate(AUTH_JWT) {

            // Log a single SpO2 reading — manual entry, or synced from Health Connect / a BLE pulse oximeter.
            post {
                val userId = call.currentUserId()
                val body = call.receive<Spo2LogRequest>()
                val result = Spo2Service.logReading(userId, body)
                call.respond(HttpStatusCode.Created, ApiSuccess(data = result))
            }

            // List this user's readings, newest first. Optional ?date=YYYY-MM-DD (single day, i.e. "Daily"),
            // ?limit=N (default 100, max 500).
            get {
                val userId = call.currentUserId()
                val date = call.request.queryParameters["date"]
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 500) ?: 100
                val result = Spo2Service.listReadings(userId, date, limit)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Most recent reading — for a "current SpO2" card.
            get("/latest") {
                val userId = call.currentUserId()
                val result = Spo2Service.getLatest(userId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Last 7 days, per-day avg/min/max.
            get("/weekly") {
                val userId = call.currentUserId()
                val result = Spo2Service.weeklySummary(userId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Last 30 days, per-day avg/min/max.
            get("/monthly") {
                val userId = call.currentUserId()
                val result = Spo2Service.monthlySummary(userId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Last 12 calendar months, per-month avg/min/max.
            get("/yearly") {
                val userId = call.currentUserId()
                val result = Spo2Service.yearlySummary(userId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            get("/{id}") {
                val userId = call.currentUserId()
                val readingId = call.parameters.parseUuid("id")
                val result = Spo2Service.getReading(userId, readingId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Correct a previously logged reading (full replace of spo2Percentage/source).
            put("/{id}") {
                val userId = call.currentUserId()
                val readingId = call.parameters.parseUuid("id")
                val body = call.receive<Spo2LogRequest>()
                val result = Spo2Service.updateReading(userId, readingId, body)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            delete("/{id}") {
                val userId = call.currentUserId()
                val readingId = call.parameters.parseUuid("id")
                Spo2Service.deleteReading(userId, readingId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = mapOf("deleted" to true)))
            }
        }
    }
}

private fun Parameters.parseUuid(name: String): UUID =
    this[name]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        ?: throw ValidationException(listOf(name to "Must be a valid UUID"))
