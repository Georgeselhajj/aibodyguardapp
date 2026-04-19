package com.aibodyguard.app.alerts.network

data class AlertDto(
    val id: Long,
    val title: String,
    val description: String,
    val userEmail: String,
    val createdAt: String
)

data class CreateAlertRequest(
    val title: String,
    val description: String,
    val userEmail: String
)
