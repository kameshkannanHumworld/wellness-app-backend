package com.wellnessapp.dashboard.routes

import com.wellnessapp.common.response.ApiSuccess
import com.wellnessapp.common.security.AUTH_JWT
import com.wellnessapp.common.security.currentUserId
import com.wellnessapp.dashboard.service.DashboardService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.dashboardRoutes() {
    route("/api/v1/dashboard") {
        authenticate(AUTH_JWT) {

            // Today's water/steps/sleep vs. goals, latest BP reading, and the overall wellness score.
            get {
                val userId = call.currentUserId()
                val result = DashboardService.getDashboard(userId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Rule-based tip text derived from today's dashboard metrics.
            get("/insight") {
                val userId = call.currentUserId()
                val result = DashboardService.getInsight(userId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Last 7 days of step totals (zero-filled) for the dashboard's steps chart.
            get("/weekly-steps") {
                val userId = call.currentUserId()
                val result = DashboardService.getWeeklySteps(userId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }
        }
    }
}
