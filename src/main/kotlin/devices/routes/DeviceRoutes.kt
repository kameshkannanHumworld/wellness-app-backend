package com.wellnessapp.devices.routes

import com.wellnessapp.common.response.ApiSuccess
import com.wellnessapp.common.security.AUTH_JWT
import com.wellnessapp.common.security.currentUserId
import com.wellnessapp.devices.dto.DeviceRegisterRequest
import com.wellnessapp.devices.dto.DeviceUnregisterRequest
import com.wellnessapp.devices.dto.TestPushRequest
import com.wellnessapp.devices.dto.UnregisterResponse
import com.wellnessapp.devices.service.DeviceTokenService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.deviceRoutes() {
    route("/api/v1/devices") {
        authenticate(AUTH_JWT) {

            // Called on app launch / after login with the FCM token from the client SDK.
            // Upserts — the same physical token can legitimately move to a different user over
            // time (reinstall, different account signed in on the same phone).
            post("/register") {
                val userId = call.currentUserId()
                val body = call.receive<DeviceRegisterRequest>()
                val result = DeviceTokenService.registerDevice(userId, body)
                call.respond(HttpStatusCode.Created, ApiSuccess(data = result))
            }

            // List this user's registered device tokens — useful for a "manage devices" settings screen.
            get {
                val userId = call.currentUserId()
                val result = DeviceTokenService.listDevices(userId)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }

            // Called on logout so this device stops receiving pushes for the account that just
            // signed out. Idempotent — succeeds even if the token was already gone.
            post("/unregister") {
                val userId = call.currentUserId()
                val body = call.receive<DeviceUnregisterRequest>()
                val removed = DeviceTokenService.unregisterDevice(userId, body.fcmToken)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = UnregisterResponse(unregistered = removed)))
            }

            // Sends an immediate test push to every device registered to the caller — for
            // verifying the FCM plumbing end-to-end from Postman without waiting for
            // ReminderScheduler's next minute tick.
            post("/test-push") {
                val userId = call.currentUserId()
                val body = call.receive<TestPushRequest>()
                val result = DeviceTokenService.sendTestPush(userId, body.title, body.body)
                call.respond(HttpStatusCode.OK, ApiSuccess(data = result))
            }
        }
    }
}
