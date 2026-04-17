package com.aibodyguard.app.dashboard.model

import androidx.annotation.DrawableRes

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
    @DrawableRes val imageRes: Int
)

data class CarouselItem(
    val title: String,
    val description: String,
    @DrawableRes val imageRes: Int
)
