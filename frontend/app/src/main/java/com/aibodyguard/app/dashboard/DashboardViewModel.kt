package com.aibodyguard.app.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibodyguard.app.alerts.network.AlertDto
import com.aibodyguard.app.alerts.network.AlertRepository
import com.aibodyguard.app.dashboard.model.Alert
import com.aibodyguard.app.dashboard.model.CarouselItem
import com.aibodyguard.app.dashboard.model.Member
import com.aibodyguard.app.dashboard.model.SecurityMode
import com.aibodyguard.app.dashboard.model.ThreatPerson
import com.aibodyguard.app.enrollment.model.PersonRole
import com.aibodyguard.app.network.RetrofitClient
import com.aibodyguard.app.network.RobotRetrofitClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DashboardViewModel : ViewModel() {

    private val robotRepository = RobotDashboardRepository(RobotRetrofitClient.enrollmentApi)
    private val alertRepository = AlertRepository(RetrofitClient.alertApi)

    private val _robotConnected = MutableStateFlow(false)
    val robotConnected: StateFlow<Boolean> = _robotConnected.asStateFlow()

    private val _robotPowered = MutableStateFlow(true)
    val robotPowered: StateFlow<Boolean> = _robotPowered.asStateFlow()

    private val _securityMode = MutableStateFlow(SecurityMode.HOME)
    val securityMode: StateFlow<SecurityMode> = _securityMode.asStateFlow()

    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    private val _alertError = MutableStateFlow<String?>(null)
    val alertError: StateFlow<String?> = _alertError.asStateFlow()

    private val _trustedMembers = MutableStateFlow<List<Member>>(emptyList())
    val trustedMembers: StateFlow<List<Member>> = _trustedMembers.asStateFlow()

    private val _threats = MutableStateFlow<List<ThreatPerson>>(emptyList())
    val threats: StateFlow<List<ThreatPerson>> = _threats.asStateFlow()

    private var currentUserEmail: String? = null

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

    /** Bind the signed-in user so DB-backed alerts can be loaded for them. */
    fun bindUser(email: String?) {
        if (email == null || email == currentUserEmail) return
        currentUserEmail = email
        viewModelScope.launch { refreshAllAlerts() }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            robotRepository.stopDetection()
            currentUserEmail = null
            _alerts.value = emptyList()
            onComplete()
        }
    }

    fun onSecurityModeSelected(mode: SecurityMode) {
        viewModelScope.launch {
            robotRepository.applyModeAndStart(mode)
                .onSuccess {
                    _securityMode.value = mode
                    refreshRobotStatusAndAlerts()
                }
                .onFailure {
                    _robotConnected.value = false
                }
        }
    }

    /** Toggle the robot power state. Backend wiring will be added later. */
    fun onToggleRobotPower() {
        _robotPowered.value = !_robotPowered.value
    }

    fun onAddTrustedMember() {
        // Handled by DashboardActivity, which opens the enrollment flow.
    }

    /** Called when FaceEnrollmentActivity returns RESULT_OK. */
    fun onMemberEnrolled(name: String, role: PersonRole = PersonRole.FAMILY_MEMBER) {
        viewModelScope.launch { refreshTrustedMembers() }
    }

    /**
     * Remove a trusted member from the list locally.
     * TODO: also call EnrollmentRepository.deletePerson(memberId) once the Pi
     *       endpoint is wired up so the robot stops recognizing them.
     */
    fun onRemoveMember(memberId: String) {
        _trustedMembers.value = _trustedMembers.value.filterNot { it.id == memberId }
    }

    /**
     * Register a new threat person from gallery photos.
     * TODO: upload [photoUris] to the Pi/model so it learns to flag this person.
     */
    fun onAddThreat(name: String, photoUris: List<String>) {
        if (name.isBlank() || photoUris.isEmpty()) return
        _threats.value = _threats.value + ThreatPerson(name = name.trim(), photoUris = photoUris)
    }

    /**
     * Remove a threat person from the list.
     * TODO: also call the Pi endpoint to forget the person once it exists.
     */
    fun onRemoveThreat(threatId: String) {
        _threats.value = _threats.value.filterNot { it.id == threatId }
    }

    /** Create a user-authored alert in the Spring Boot DB. */
    fun createAlert(title: String, description: String) {
        val email = currentUserEmail ?: return
        viewModelScope.launch {
            alertRepository.create(title, description, email)
                .onSuccess { dto ->
                    _alerts.value = listOf(dto.toUiAlert()) + _alerts.value
                }
                .onFailure { _alertError.value = it.message }
        }
    }

    fun refreshAlerts() {
        viewModelScope.launch { refreshAllAlerts() }
    }

    fun consumeAlertError() {
        _alertError.value = null
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

            robotRepository.applyModeAndStart(SecurityMode.HOME)
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
        robotRepository.fetchStatus()
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
            }

        refreshAllAlerts()
    }

    /** Combine DB-stored user alerts (top) with Pi-polled robot alerts (below). */
    private suspend fun refreshAllAlerts() {
        val email = currentUserEmail
        val dbAlerts = if (email != null) {
            alertRepository.list(email).getOrNull()?.map { it.toUiAlert() }.orEmpty()
        } else {
            emptyList()
        }
        val robotAlerts = robotRepository.fetchAlerts().getOrNull().orEmpty()
        _alerts.value = dbAlerts + robotAlerts
    }

    private suspend fun refreshTrustedMembers() {
        robotRepository.fetchTrustedMembers()
            .onSuccess { members ->
                _trustedMembers.value = members
            }
            .onFailure {
                if (_trustedMembers.value.isEmpty()) {
                    _trustedMembers.value = emptyList()
                }
            }
    }

    private fun AlertDto.toUiAlert(): Alert = Alert(
        title = title,
        description = description,
        timestamp = formatTimestamp(createdAt)
    )

    private fun formatTimestamp(iso: String): String {
        val date = parseIsoInstant(iso) ?: return iso
        val seconds = ((System.currentTimeMillis() - date.time) / 1000L).coerceAtLeast(0)
        return when {
            seconds < 60 -> "Just now"
            seconds < 3600 -> "${seconds / 60} min ago"
            seconds < 86_400 -> "${seconds / 3600} h ago"
            else -> SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(date)
        }
    }

    private fun parseIsoInstant(iso: String): Date? {
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        for (pattern in patterns) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(iso)
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }
}
