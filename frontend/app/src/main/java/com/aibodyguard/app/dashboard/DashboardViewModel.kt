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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class DashboardViewModel : ViewModel() {

    private val alertRepository = AlertRepository(RetrofitClient.alertApi)

    private val _robotConnected = MutableStateFlow(true)
    val robotConnected: StateFlow<Boolean> = _robotConnected.asStateFlow()

    private val _robotPowered = MutableStateFlow(true)
    val robotPowered: StateFlow<Boolean> = _robotPowered.asStateFlow()

    private val _securityMode = MutableStateFlow(SecurityMode.HOME)
    val securityMode: StateFlow<SecurityMode> = _securityMode.asStateFlow()

    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    private val _alertError = MutableStateFlow<String?>(null)
    val alertError: StateFlow<String?> = _alertError.asStateFlow()

    private var currentUserEmail: String? = null

    private val _trustedMembers = MutableStateFlow(
        listOf(
            Member(
                name = "Maya",
                imageRes = android.R.drawable.ic_menu_myplaces,
                id = "seed_maya",
                role = PersonRole.OWNER
            ),
            Member(
                name = "Omar",
                imageRes = android.R.drawable.ic_menu_camera,
                id = "seed_omar",
                role = PersonRole.FAMILY_MEMBER
            ),
            Member(
                name = "Lea",
                imageRes = android.R.drawable.ic_menu_info_details,
                id = "seed_lea",
                role = PersonRole.FAMILY_MEMBER
            )
        )
    )
    val trustedMembers: StateFlow<List<Member>> = _trustedMembers.asStateFlow()

    private val _threats = MutableStateFlow<List<ThreatPerson>>(emptyList())
    val threats: StateFlow<List<ThreatPerson>> = _threats.asStateFlow()

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

    fun bindUser(email: String?) {
        if (email == null || email == currentUserEmail) return
        currentUserEmail = email
        refreshAlerts()
    }

    fun refreshAlerts() {
        val email = currentUserEmail ?: return
        viewModelScope.launch {
            alertRepository.list(email)
                .onSuccess { dtos -> _alerts.value = dtos.map { it.toUiAlert() } }
                .onFailure { _alertError.value = it.message }
        }
    }

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

    fun consumeAlertError() {
        _alertError.value = null
    }

    fun onSecurityModeSelected(mode: SecurityMode) {
        _securityMode.value = mode
    }

    /** Toggle the robot power state. Backend wiring will be added later. */
    fun onToggleRobotPower() {
        _robotPowered.value = !_robotPowered.value
    }

    fun onAddTrustedMember() {
        // Handled by DashboardActivity — opens EnrollMemberDialog → FaceEnrollmentActivity.
    }

    /** Called when FaceEnrollmentActivity returns RESULT_OK with an enrolled name. */
    fun onMemberEnrolled(name: String, role: PersonRole = PersonRole.FAMILY_MEMBER) {
        val icon = if (role == PersonRole.OWNER) {
            android.R.drawable.ic_menu_myplaces
        } else {
            android.R.drawable.ic_menu_camera
        }
        _trustedMembers.value = _trustedMembers.value + Member(
            name = name,
            imageRes = icon,
            role = role
        )
    }

    /**
     * Remove a trusted member from the list.
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
        // Spring serializes java.time.Instant as e.g. "2026-04-18T12:34:56.789Z".
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
