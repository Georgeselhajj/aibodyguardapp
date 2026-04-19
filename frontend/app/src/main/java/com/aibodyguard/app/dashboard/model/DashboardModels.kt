package com.aibodyguard.app.dashboard.model

import androidx.annotation.DrawableRes
import com.aibodyguard.app.enrollment.model.PersonRole
import java.util.UUID

enum class SecurityMode {
    HOME,
    AWAY
}

data class Alert(
    val title: String,
    val description: String,
    val timestamp: String
)

data class Member(
    val name: String,
    @DrawableRes val imageRes: Int,
    val id: String = UUID.randomUUID().toString(),
    val role: PersonRole = PersonRole.FAMILY_MEMBER
)

data class ThreatPerson(
    val name: String,
    val photoUris: List<String>,
    val id: String = UUID.randomUUID().toString()
)

data class CarouselItem(
    val title: String,
    val description: String,
    @DrawableRes val imageRes: Int
)

data class RobotStatusResponse(
    val mode: String = "HOME",
    val owner_present: Boolean = false,
    val alert_count: Int = 0,
    val running: Boolean = false,
)

data class RobotAlertResponse(
    val id: String,
    val level: String,
    val person_id: String,
    val name: String,
    val explanation: List<String> = emptyList(),
    val track_id: Int,
    val timestamp: String,
)

data class RobotModeRequest(
    val mode: String,
)

data class RobotCommandResponse(
    val success: Boolean? = null,
    val status: String? = null,
    val mode: String? = null,
    val message: String? = null,
)
