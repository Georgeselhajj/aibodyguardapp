package com.aibodyguard.app.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.aibodyguard.app.dashboard.model.Member
import com.aibodyguard.app.enrollment.FaceEnrollmentActivity
import com.aibodyguard.app.enrollment.model.PersonRole
import com.aibodyguard.app.dashboard.ui.DashboardRoute
import com.aibodyguard.app.ui.theme.AIBodyguardTheme

class DashboardActivity : ComponentActivity() {

    private lateinit var dashboardViewModel: DashboardViewModel

    // Result launcher — receives RESULT_OK when enrollment succeeds
    private val enrollmentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val enrolledName = result.data?.getStringExtra(
                FaceEnrollmentActivity.EXTRA_ENROLLED_NAME
            ) ?: return@registerForActivityResult
            dashboardViewModel.onMemberEnrolled(enrolledName)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]

        setContent {
            AIBodyguardTheme {
                var showEnrollDialog by remember { mutableStateOf(false) }

                DashboardRoute(
                    viewModel         = dashboardViewModel,
                    onAddTrustedMember = { showEnrollDialog = true },
                )

                if (showEnrollDialog) {
                    EnrollMemberDialog(
                        onDismiss = { showEnrollDialog = false },
                        onConfirm = { name, role ->
                            showEnrollDialog = false
                            enrollmentLauncher.launch(
                                FaceEnrollmentActivity.createIntent(this, name, role)
                            )
                        },
                    )
                }
            }
        }
    }

    companion object {
        fun createIntent(context: android.content.Context) =
            android.content.Intent(context, DashboardActivity::class.java)
    }
}

// ----------------------------------------------------------------
// Enrollment setup dialog — collects name + role before opening camera
// ----------------------------------------------------------------

@Composable
private fun EnrollMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, role: PersonRole) -> Unit,
) {
    var name         by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(PersonRole.FAMILY_MEMBER) }
    val nameError    = name.isNotEmpty() && name.trim().length < 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Trusted Member", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Full name") },
                    placeholder   = { Text("e.g. Sara") },
                    singleLine    = true,
                    isError       = nameError,
                    supportingText = if (nameError) {
                        { Text("Name must be at least 2 characters") }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    "Role",
                    style     = MaterialTheme.typography.labelMedium,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PersonRole.values().forEach { role ->
                        val selected = role == selectedRole
                        FilledTonalButton(
                            onClick  = { selectedRole = role },
                            modifier = Modifier.weight(1f),
                            colors   = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (selected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Text(
                                text  = role.displayName,
                                color = if (selected)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick  = { if (name.trim().length >= 2) onConfirm(name.trim(), selectedRole) },
                enabled  = name.trim().length >= 2,
            ) {
                Text("Continue to Camera")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
