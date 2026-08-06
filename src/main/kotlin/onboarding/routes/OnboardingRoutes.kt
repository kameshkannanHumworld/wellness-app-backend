package com.wellnessapp.onboarding.routes

import com.wellnessapp.common.response.ApiSuccess
import com.wellnessapp.common.security.AUTH_JWT
import com.wellnessapp.common.security.currentUserId
import com.wellnessapp.onboarding.dto.BasicInfoRequest
import com.wellnessapp.onboarding.dto.GoalsRequest
import com.wellnessapp.onboarding.dto.IntegrationsRequest
import com.wellnessapp.onboarding.dto.TargetsRequest
import com.wellnessapp.onboarding.service.OnboardingService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.onboardingRoutes() {
    route("/api/v1/onboarding") {
        authenticate(AUTH_JWT) {

            // Step 1 — name/age/gender/height/weight
            post("/basic") {
                val userId = call.currentUserId()
                val body = call.receive<BasicInfoRequest>()
                val result = OnboardingService.saveBasicInfo(userId, body)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Step 2 — selected goal_type(s)
            post("/goals") {
                val userId = call.currentUserId()
                val body = call.receive<GoalsRequest>()
                val result = OnboardingService.saveGoals(userId, body)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Step 3 — water/step/sleep goals
            post("/targets") {
                val userId = call.currentUserId()
                val body = call.receive<TargetsRequest>()
                val result = OnboardingService.saveTargets(userId, body)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Step 4 — Apple Health / Google Fit / Samsung Health flags
            post("/integrations") {
                val userId = call.currentUserId()
                val body = call.receive<IntegrationsRequest>()
                val result = OnboardingService.saveIntegrations(userId, body)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Step 5 — marks users.onboarding_completed = true
            post("/complete") {
                val userId = call.currentUserId()
                val result = OnboardingService.completeOnboarding(userId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }
        }
    }
}
