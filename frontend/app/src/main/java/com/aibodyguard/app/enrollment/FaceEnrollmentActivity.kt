package com.aibodyguard.app.enrollment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aibodyguard.app.enrollment.model.EnrollmentResult
import com.aibodyguard.app.enrollment.model.PersonRole
import com.aibodyguard.app.enrollment.ui.EnrollmentSuccessScreen
import com.aibodyguard.app.enrollment.ui.FaceEnrollmentRoute
import com.aibodyguard.app.ui.theme.AIBodyguardTheme

/**
 * Host activity for the face enrollment flow.
 *
 * Start it with [createIntent] from the dashboard:
 *
 *   startActivity(
 *       FaceEnrollmentActivity.createIntent(context, name = "Sara", role = PersonRole.FAMILY_MEMBER)
 *   )
 *
 * On success the activity finishes with [RESULT_OK] and the extra
 * [EXTRA_ENROLLED_NAME] set to the enrolled person's display name.
 */
class FaceEnrollmentActivity : ComponentActivity() {

    private var showSuccess = false
    private var successName = ""
    private var successSamples = 0
    private var enrolledRole: PersonRole = PersonRole.FAMILY_MEMBER

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val name = intent.getStringExtra(EXTRA_NAME) ?: "Person"
        val role = intent.getStringExtra(EXTRA_ROLE)
            ?.let { runCatching { PersonRole.valueOf(it) }.getOrNull() }
            ?: PersonRole.FAMILY_MEMBER
        enrolledRole = role

        setContent {
            AIBodyguardTheme {
                if (showSuccess) {
                    EnrollmentSuccessScreen(
                        name    = successName,
                        samples = successSamples,
                        onDone  = {
                            setResult(RESULT_OK, Intent().apply {
                                putExtra(EXTRA_ENROLLED_NAME, successName)
                                putExtra(EXTRA_ENROLLED_ROLE, enrolledRole.name)
                            })
                            finish()
                        },
                    )
                } else {
                    FaceEnrollmentRoute(
                        name  = name,
                        role  = role,
                        onEnrollmentComplete = { result ->
                            successName    = result.personName
                            successSamples = result.samplesProcessed
                            showSuccess    = true
                        },
                        onCancel = { finish() },
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_NAME          = "extra_enrollment_name"
        const val EXTRA_ROLE          = "extra_enrollment_role"
        const val EXTRA_ENROLLED_NAME = "extra_enrolled_name"
        const val EXTRA_ENROLLED_ROLE = "extra_enrolled_role"

        fun createIntent(
            context: Context,
            name:    String,
            role:    PersonRole = PersonRole.FAMILY_MEMBER,
        ): Intent = Intent(context, FaceEnrollmentActivity::class.java).apply {
            putExtra(EXTRA_NAME, name)
            putExtra(EXTRA_ROLE, role.name)
        }
    }
}
