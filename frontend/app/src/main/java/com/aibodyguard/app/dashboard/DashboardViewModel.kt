package com.aibodyguard.app.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibodyguard.app.dashboard.model.Alert
import com.aibodyguard.app.dashboard.model.CarouselItem
import com.aibodyguard.app.dashboard.model.Member
import com.aibodyguard.app.dashboard.model.SecurityMode
import com.aibodyguard.app.network.RobotRetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val repository = RobotDashboardRepository(RobotRetrofitClient.enrollmentApi)

    private val _robotConnected = MutableStateFlow(false)
    val robotConnected: StateFlow<Boolean> = _robotConnected.asStateFlow()

    private val _securityMode = MutableStateFlow(SecurityMode.HOME)
    val securityMode: StateFlow<SecurityMode> = _securityMode.asStateFlow()

    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    private val _trustedMembers = MutableStateFlow<List<Member>>(emptyList())
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

    init {
        bootstrapDefaultMode()
        startRobotPolling()
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.stopDetection()
            onComplete()
        }
    }

    fun onSecurityModeSelected(mode: SecurityMode) {
        viewModelScope.launch {
            repository.applyModeAndStart(mode)
                .onSuccess {
                    _securityMode.value = mode
                    refreshRobotStatusAndAlerts()
                }
                .onFailure {
                    _robotConnected.value = false
                }
        }
    }

    fun onAddTrustedMember() {
        // Handled by DashboardActivity, which opens the enrollment flow.
    }

    fun onMemberEnrolled(name: String) {
        viewModelScope.launch {
            refreshTrustedMembers()
        }
    }

    private fun refreshDashboard() {
        viewModelScope.launch {
            refreshRobotStatusAndAlerts()
            refreshTrustedMembers()
        }
    }

    private fun bootstrapDefaultMode() {
        viewModelScope.launch {
            _securityMode.value = SecurityMode.HOME

            repository.applyModeAndStart(SecurityMode.HOME)
                .onFailure {
                    _robotConnected.value = false
                }

            refreshDashboard()
        }
    }

    private fun startRobotPolling() {
        viewModelScope.launch {
            while (isActive) {
                refreshRobotStatusAndAlerts()
                delay(3000)
            }
        }
    }

    private suspend fun refreshRobotStatusAndAlerts() {
        repository.fetchStatus()
            .onSuccess { status ->
                _robotConnected.value = true
                _securityMode.value = if (status.mode.equals("AWAY", ignoreCase = true)) {
                    SecurityMode.AWAY
                } else {
                    SecurityMode.HOME
                }
            }
            .onFailure {
                _robotConnected.value = false
                return
            }

        repository.fetchAlerts()
            .onSuccess { alerts ->
                _alerts.value = alerts
            }
            .onFailure {
                _alerts.value = emptyList()
            }
    }

    private suspend fun refreshTrustedMembers() {
        repository.fetchTrustedMembers()
            .onSuccess { members ->
                _trustedMembers.value = members
            }
            .onFailure {
                if (_trustedMembers.value.isEmpty()) {
                    _trustedMembers.value = emptyList()
                }
            }
    }
}
