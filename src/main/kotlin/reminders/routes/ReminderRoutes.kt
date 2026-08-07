package com.wellnessapp.reminders.routes

import com.wellnessapp.common.exception.ValidationException
import com.wellnessapp.common.response.ApiSuccess
import com.wellnessapp.common.security.AUTH_JWT
import com.wellnessapp.common.security.currentUserId
import com.wellnessapp.reminders.dto.ReminderRequest
import com.wellnessapp.reminders.service.ReminderService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.reminderRoutes() {
    route("/api/v1/reminders") {
        authenticate(AUTH_JWT) {

            // Create a reminder — always starts enabled.
            post {
                val userId = call.currentUserId()
                val body = call.receive<ReminderRequest>()
                val result = ReminderService.createReminder(userId, body)
                call.respond(HttpStatusCode.Created, ApiSuccess(data = result))
            }

            // List this user's reminders, soonest time-of-day first. Optional ?enabled=true|false.
            get {
                val userId = call.currentUserId()
                val enabledFilter = call.request.queryParameters["enabled"]?.toBooleanStrictOrNull()
                val result = ReminderService.listReminders(userId, enabledFilter)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            get("/{id}") {
                val userId = call.currentUserId()
                val reminderId = call.parameters.parseUuid("id")
                val result = ReminderService.getReminder(userId, reminderId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Full replace — reminderType/reminderTime/repeatType required, enabled optional.
            put("/{id}") {
                val userId = call.currentUserId()
                val reminderId = call.parameters.parseUuid("id")
                val body = call.receive<ReminderRequest>()
                val result = ReminderService.updateReminder(userId, reminderId, body)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Flip enabled on/off — the one write in this module that doesn't need a body.
            patch("/{id}/toggle") {
                val userId = call.currentUserId()
                val reminderId = call.parameters.parseUuid("id")
                val result = ReminderService.toggleReminder(userId, reminderId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            delete("/{id}") {
                val userId = call.currentUserId()
                val reminderId = call.parameters.parseUuid("id")
                ReminderService.deleteReminder(userId, reminderId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = mapOf("deleted" to true)))
            }
        }
    }
}

private fun Parameters.parseUuid(name: String): UUID =
    this[name]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
        ?: throw ValidationException(listOf(name to "Must be a valid UUID"))
