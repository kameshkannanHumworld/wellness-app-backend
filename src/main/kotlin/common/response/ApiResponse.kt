package com.wellnessapp.common.response

import kotlinx.serialization.Serializable

@Serializable
data class ApiSuccess<T>(
    val success: Boolean = true,
    val data: T,
    val message: String = "Success"
)

@Serializable
data class FieldError(val field: String, val message: String)

@Serializable
data class ApiError(
    val success: Boolean = false,
    val code: String,
    val message: String,
    val errors: List<FieldError> = emptyList()
)
