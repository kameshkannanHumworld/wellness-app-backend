package com.wellnessapp.fcm

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import com.wellnessapp.devices.repository.DeviceTokenRepository
import org.slf4j.LoggerFactory
import java.util.UUID

data class PushResult(val sent: Int, val failed: Int)

/**
 * Thin wrapper over the Firebase Admin SDK's messaging API (already available — `firebase-admin`
 * is already a dependency for verifying Google/email Firebase ID tokens in the Auth module, and
 * FirebaseApp.initializeApp() is already called once at startup in FirebaseConfig.kt; that's all
 * FirebaseMessaging.getInstance() needs).
 *
 * Sends one push per registered device token for a user (not a single multicast) so that one
 * dead token doesn't affect delivery to the user's other devices. Tokens Firebase reports as
 * permanently invalid (UNREGISTERED / INVALID_ARGUMENT) are pruned from device_tokens on the spot
 * so they stop being retried on every future send.
 */
object FcmService {

    private val logger = LoggerFactory.getLogger(FcmService::class.java)

    fun sendToUser(userId: UUID, title: String, body: String): PushResult {
        val tokens = DeviceTokenRepository.findAllForUser(userId).map { it.fcmToken }
        if (tokens.isEmpty()) return PushResult(sent = 0, failed = 0)

        var sent = 0
        var failed = 0

        for (token in tokens) {
            try {
                val message = Message.builder()
                    .setToken(token)
                    .setNotification(
                        Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build()
                    )
                    .build()

                FirebaseMessaging.getInstance().send(message)
                sent++
            } catch (e: FirebaseMessagingException) {
                failed++
                logger.warn("FCM send failed for a device of user {}: {}", userId, e.messagingErrorCode)
                if (e.messagingErrorCode == MessagingErrorCode.UNREGISTERED ||
                    e.messagingErrorCode == MessagingErrorCode.INVALID_ARGUMENT
                ) {
                    DeviceTokenRepository.deleteByTokenForUser(userId, token)
                }
            } catch (e: Exception) {
                // Defensive catch-all — a single bad token/network hiccup must not take down the
                // whole batch (or, worse, the ReminderScheduler's background loop).
                failed++
                logger.warn("Unexpected error sending FCM push to a device of user {}", userId, e)
            }
        }

        return PushResult(sent, failed)
    }
}
