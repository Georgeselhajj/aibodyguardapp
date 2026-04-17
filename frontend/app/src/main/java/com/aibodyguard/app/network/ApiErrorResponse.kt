package com.aibodyguard.app.network

data class ApiErrorResponse(
    val message: String? = null,
    val detail: String? = null,
    val error: String? = null
)
