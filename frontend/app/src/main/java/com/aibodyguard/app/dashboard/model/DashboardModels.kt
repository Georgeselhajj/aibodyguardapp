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
