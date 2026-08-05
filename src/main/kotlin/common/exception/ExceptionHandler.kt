package com.wellnessapp.common.exception

import com.wellnessapp.common.response.ApiError
import com.wellnessapp.common.response.FieldError
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun StatusPagesConfig.registerExceptionHandling() {
    exception<ValidationException> { call, cause ->
        call.respond(
            HttpStatusCode.BadRequest,
            ApiError(
                code = "VALIDATION_ERROR",
                message = cause.message ?: "Invalid input",
                errors = cause.fieldErrors.map { FieldError(it.first, it.second) }
            )
        )
    }
    exception<UnauthorizedException> { call, cause ->
        call.respond(
            HttpStatusCode.Unauthorized,
            ApiError(code = "UNAUTHORIZED", message = cause.message ?: "Unauthorized")
        )
    }
    exception<ForbiddenException> { call, cause ->
        call.respond(
            HttpStatusCode.Forbidden,
            ApiError(code = "FORBIDDEN", message = cause.message ?: "Forbidden")
        )
    }
    exception<NotFoundException> { call, cause ->
        call.respond(
            HttpStatusCode.NotFound,
            ApiError(code = "NOT_FOUND", message = cause.message ?: "Not found")
        )
    }
    exception<Throwable> { call, cause ->
        call.application.environment.log.error("Unhandled exception", cause)
        call.respond(
            HttpStatusCode.InternalServerError,
            ApiError(code = "INTERNAL_SERVER_ERROR", message = "Something went wrong")
        )
    }
}
