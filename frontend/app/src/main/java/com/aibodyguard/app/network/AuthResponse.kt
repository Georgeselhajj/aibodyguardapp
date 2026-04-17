package com.aibodyguard.app.network

data class AuthResponse(
    val token: String,
    val email: String,
    val role: String
)