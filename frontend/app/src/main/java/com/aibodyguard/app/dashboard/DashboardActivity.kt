package com.aibodyguard.app.dashboard

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import com.aibodyguard.app.MainActivity
import com.aibodyguard.app.SessionManager
import com.aibodyguard.app.dashboard.ui.DashboardRoute
import com.aibodyguard.app.enrollment.FaceEnrollmentActivity
import com.aibodyguard.app.enrollment.model.PersonRole
import com.aibodyguard.app.ui.theme.AIBodyguardTheme

class DashboardActivity : ComponentActivity() {

    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var sessionManager: SessionManager

    // Result launcher — receives RESULT_OK when enrollment succeeds
    private val enrollmentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val enrolledName = result.data?.getStringExtra(
                FaceEnrollmentActivity.EXTRA_ENROLLED_NAME
            ) ?: return@registerForActivityResult
            val enrolledRole = result.data
                ?.getStringExtra(FaceEnrollmentActivity.EXTRA_ENROLLED_ROLE)
                ?.let { runCatching { PersonRole.valueOf(it) }.getOrNull() }
                ?: PersonRole.FAMILY_MEMBER
            dashboardViewModel.onMemberEnrolled(enrolledName, enrolledRole)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        dashboardViewModel = ViewModelProvider(this)[DashboardViewModel::class.java]
        sessionManager = SessionManager(this)
        dashboardViewModel.bindUser(sessionManager.getEmail())

        setContent {
            AIBodyguardTheme {
                var showEnrollDialog by remember { mutableStateOf(false) }
                var showAddAlertDialog by remember { mutableStateOf(false) }
                var pendingThreatName by remember { mutableStateOf<String?>(null) }
                var showAddThreatDialog by remember { mutableStateOf(false) }

                val pickThreatPhotos = rememberLauncherForActivityResult(
                    ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
                ) { uris: List<Uri> ->
                    val name = pendingThreatName
                    pendingThreatName = null
                    if (name != null && uris.isNotEmpty()) {
                        dashboardViewModel.onAddThreat(name, uris.map { it.toString() })
                    }
                }

                DashboardRoute(
                    viewModel          = dashboardViewModel,
                    onAddTrustedMember = { showEnrollDialog = true },
                    onAddAlert         = { showAddAlertDialog = true },
                    onAddThreat        = { showAddThreatDialog = true },
                    onLogout           = {
                        sessionManager.clearSession()
                        startActivity(android.content.Intent(this, MainActivity::class.java))
                        finish()
                    },
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

                if (showAddAlertDialog) {
                    AddAlertDialog(
                        onDismiss = { showAddAlertDialog = false },
                        onConfirm = { title, description ->
                            showAddAlertDialog = false
                            dashboardViewModel.createAlert(title, description)
                        },
                    )
                }

                if (showAddThreatDialog) {
                    AddThreatDialog(
                        onDismiss = { showAddThreatDialog = false },
                        onConfirm = { name ->
                            showAddThreatDialog = false
                            pendingThreatName = name
                            pickThreatPhotos.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
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
// Dialogs
// ----------------------------------------------------------------

@Composable
private fun AddAlertDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val titleValid = title.trim().length >= 2
    val descriptionValid = description.trim().length >= 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Alert", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    placeholder = { Text("e.g. Front gate motion") },
                    singleLine = true,
                    isError = title.isNotEmpty() && !titleValid,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("What did the robot observe?") },
                    isError = description.isNotEmpty() && !descriptionValid,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (titleValid && descriptionValid) {
                        onConfirm(title.trim(), description.trim())
                    }
                },
                enabled = titleValid && descriptionValid,
            ) {
                Text("Save Alert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun AddThreatDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val nameValid = name.trim().length >= 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Known Threat", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Person's name") },
                    placeholder = { Text("e.g. Unknown intruder") },
                    singleLine = true,
                    isError = name.isNotEmpty() && !nameValid,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Next, pick up to 10 photos of this person from your gallery.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (nameValid) onConfirm(name.trim()) },
                enabled = nameValid,
            ) {
                Text("Pick Photos")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

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
