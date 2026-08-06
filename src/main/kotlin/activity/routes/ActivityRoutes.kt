package com.wellnessapp.activity.routes

import com.wellnessapp.activity.dto.ActivityLogRequest
import com.wellnessapp.activity.service.ActivityService
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

fun Route.activityRoutes() {
    route("/api/v1/activity") {
        authenticate(AUTH_JWT) {

            // Log a single activity entry — steps / workout / sleep / movement.
            post {
                val userId = call.currentUserId()
                val body = call.receive<ActivityLogRequest>()
                val result = ActivityService.logActivity(userId, body)
                call.respond(HttpStatusCode.Created, ApiSuccess(data = result))
            }

            // List this user's logs, newest first. Optional ?type=steps|workout|sleep|movement,
            // ?date=YYYY-MM-DD (single day), ?limit=N (default 100, max 500).
            get {
                val userId = call.currentUserId()
                val type = call.request.queryParameters["type"]
                val date = call.request.queryParameters["date"]
                val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 500) ?: 100
                val result = ActivityService.listActivities(userId, type, date, limit)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Last 7 days (today + previous 6, zero-filled), broken down per activity type, for the chart screen.
            get("/weekly") {
                val userId = call.currentUserId()
                val result = ActivityService.weeklySummary(userId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            get("/{id}") {
                val userId = call.currentUserId()
                val activityId = call.parameters.parseUuid("id")
                val result = ActivityService.getActivity(userId, activityId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Correct a previously logged entry (type, value, duration, and/or calories — full replace).
            put("/{id}") {
                val userId = call.currentUserId()
                val activityId = call.parameters.parseUuid("id")
                val body = call.receive<ActivityLogRequest>()
                val result = ActivityService.updateActivity(userId, activityId, body)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            delete("/{id}") {
                val userId = call.currentUserId()
                val activityId = call.parameters.parseUuid("id")
                ActivityService.deleteActivity(userId, activityId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = mapOf("deleted" to true)))
            }
        }
    }
}

private fun Parameters.parseUuid(name: String): UUID =
    this[name]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        ?: throw ValidationException(listOf(name to "Must be a valid UUID"))
