package com.aibodyguard.app.dashboard

import com.aibodyguard.app.dashboard.model.Alert
import com.aibodyguard.app.dashboard.model.Member
import com.aibodyguard.app.dashboard.model.RobotModeRequest
import com.aibodyguard.app.dashboard.model.RobotStatusResponse
import com.aibodyguard.app.dashboard.model.SecurityMode
import com.aibodyguard.app.dashboard.model.ThreatEnrollRequest
import com.aibodyguard.app.dashboard.model.ThreatPerson
import com.aibodyguard.app.enrollment.model.EnrolledPersonInfo
import com.aibodyguard.app.enrollment.model.EnrollmentRequest
import com.aibodyguard.app.enrollment.model.PersonRole
import com.aibodyguard.app.enrollment.network.EnrollmentApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RobotDashboardRepository(
    private val api: EnrollmentApi,
) {

    suspend fun fetchStatus(): Result<RobotStatusResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getStatus()
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code()}")
            }
            response.body() ?: throw Exception("No status received from Pi.")
        }
    }

    suspend fun fetchAlerts(limit: Int = 20): Result<List<Alert>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.getAlerts(limit)
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code()}")
            }
            response.body().orEmpty().map { alert ->
                val title = if (alert.level.equals("THREAT", ignoreCase = true)) {
                    "Threat Detected"
                } else {
                    "Suspicious Person Detected"
                }
                val details = alert.explanation.joinToString(", ").ifBlank { "Unknown activity" }
                val name = alert.name.ifBlank { "Unknown person" }

                Alert(
                    title = title,
                    description = "$name: $details",
                    timestamp = alert.timestamp.replace("T", " ").removeSuffix("Z"),
                )
            }
        }
    }

    suspend fun fetchTrustedMembers(): Result<List<Member>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.listEnrolled()
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code()}")
            }
            response.body().orEmpty()
                .filter { !it.role.equals("threat", ignoreCase = true) }
                .map { person -> person.toMember() }
        }
    }

    suspend fun deleteMember(personId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.deletePerson(personId)
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code()}")
            }
        }
    }

    suspend fun enrollThreat(threatId: String, name: String, images: List<String>): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = api.enrollPerson(
                    EnrollmentRequest(
                        person_id = threatId,
                        name      = name,
                        role      = "threat",
                        images    = images,
                    )
                )
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}")
                }
            }
        }

    suspend fun fetchThreats(): Result<List<ThreatPerson>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.listThreats()
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code()}")
            }
            response.body().orEmpty().map { t ->
                ThreatPerson(name = t.name, photoUris = emptyList(), id = t.threat_id)
            }
        }
    }

    suspend fun deleteThreat(threatId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.deleteThreat(threatId)
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code()}")
            }
        }
    }

    suspend fun stopDetection(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = api.stopDetection()
            if (!response.isSuccessful) {
                throw Exception("Could not stop detection (HTTP ${response.code()})")
            }
        }
    }

    suspend fun applyModeAndStart(mode: SecurityMode): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val modeResponse = api.setMode(RobotModeRequest(mode.name))
            if (!modeResponse.isSuccessful) {
                throw Exception("Could not set mode (HTTP ${modeResponse.code()})")
            }

            val startResponse = api.startDetection()
            if (!startResponse.isSuccessful) {
                throw Exception("Could not start detection (HTTP ${startResponse.code()})")
            }
        }
    }

    private fun EnrolledPersonInfo.toMember(): Member {
        val parsedRole = if (role.equals("owner", ignoreCase = true)) {
            PersonRole.OWNER
        } else {
            PersonRole.FAMILY_MEMBER
        }
        val icon = if (parsedRole == PersonRole.OWNER) {
            android.R.drawable.ic_menu_myplaces
        } else {
            android.R.drawable.ic_menu_camera
        }
        return Member(
            name = name,
            imageRes = icon,
            id = person_id,
            role = parsedRole,
        )
    }
}
