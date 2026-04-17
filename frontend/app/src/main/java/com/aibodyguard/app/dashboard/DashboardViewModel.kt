package com.aibodyguard.app.dashboard

import androidx.lifecycle.ViewModel
import com.aibodyguard.app.dashboard.model.Alert
import com.aibodyguard.app.dashboard.model.CarouselItem
import com.aibodyguard.app.dashboard.model.Member
import com.aibodyguard.app.dashboard.model.SecurityMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DashboardViewModel : ViewModel() {

    private val _robotConnected = MutableStateFlow(true)
    val robotConnected: StateFlow<Boolean> = _robotConnected.asStateFlow()

    private val _securityMode = MutableStateFlow(SecurityMode.HOME)
    val securityMode: StateFlow<SecurityMode> = _securityMode.asStateFlow()

    private val _alerts = MutableStateFlow(
        listOf(
            Alert(
                title = "Suspicious Person Detected",
                description = "The robot flagged repeated movement near the front entrance.",
                timestamp = "2 min ago"
            ),
            Alert(
                title = "Unrecognized Motion Pattern",
                description = "Unexpected activity was detected in the backyard perimeter.",
                timestamp = "12 min ago"
            ),
            Alert(
                title = "Patrol Route Interrupted",
                description = "Smart patrol paused briefly and resumed after obstacle avoidance.",
                timestamp = "31 min ago"
            )
        )
    )
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    private val _trustedMembers = MutableStateFlow(
        listOf(
            Member(name = "Maya", imageRes = android.R.drawable.ic_menu_myplaces),
            Member(name = "Omar", imageRes = android.R.drawable.ic_menu_camera),
            Member(name = "Lea", imageRes = android.R.drawable.ic_menu_info_details)
        )
    )
    val trustedMembers: StateFlow<List<Member>> = _trustedMembers.asStateFlow()

    val carouselItems: List<CarouselItem> = listOf(
        CarouselItem(
            title = "AI Threat Detection",
            description = "Scans live surroundings and highlights unusual behavior in real time.",
            imageRes = android.R.drawable.ic_dialog_alert
        ),
        CarouselItem(
            title = "Smart Patrol",
            description = "Navigates through the property autonomously with adaptive route coverage.",
            imageRes = android.R.drawable.ic_menu_compass
        ),
        CarouselItem(
            title = "Trusted Face Recognition",
            description = "Differentiates trusted members from unknown visitors before escalating.",
            imageRes = android.R.drawable.ic_menu_camera
        )
    )

    fun onSecurityModeSelected(mode: SecurityMode) {
        _securityMode.value = mode
    }

    fun onAddTrustedMember() {
        // Placeholder for future member enrollment flow.
    }
}
