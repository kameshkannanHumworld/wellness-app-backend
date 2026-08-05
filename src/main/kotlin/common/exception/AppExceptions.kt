package com.wellnessapp.common.exception

class ValidationException(
    val fieldErrors: List<Pair<String, String>>,
    message: String = "Invalid input"
) : Exception(message)

class UnauthorizedException(message: String = "Unauthorized") : Exception(message)

class NotFoundException(message: String = "Resource not found") : Exception(message)

class ForbiddenException(message: String = "You do not have access to this resource") : Exception(message)
