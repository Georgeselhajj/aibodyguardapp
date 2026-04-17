package com.aibodyguard.app

import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.aibodyguard.app.dashboard.DashboardActivity
import com.aibodyguard.app.network.AuthRepository
import com.aibodyguard.app.network.AuthResponse
import com.aibodyguard.app.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private enum class AuthMode {
        LOGIN,
        REGISTER
    }

    private val authRepository = AuthRepository(RetrofitClient.authApi)

    private lateinit var sessionManager: SessionManager
    private lateinit var feedbackText: TextView
    private lateinit var authCard: MaterialCardView
    private lateinit var authCardTitle: TextView
    private lateinit var authCardSubtitle: TextView
    private lateinit var modeToggleGroup: MaterialButtonToggleGroup
    private lateinit var loginModeButton: MaterialButton
    private lateinit var registerModeButton: MaterialButton
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordInputLayout: TextInputLayout
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var authProgressIndicator: LinearProgressIndicator
    private lateinit var authActionButton: MaterialButton
    private lateinit var sessionCard: MaterialCardView
    private lateinit var sessionEmailText: TextView
    private lateinit var sessionRoleText: TextView
    private lateinit var logoutButton: MaterialButton

    private var authMode = AuthMode.LOGIN
    private var authJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        if (sessionManager.getSession() != null) {
            openDashboard()
            return
        }

        setContentView(R.layout.activity_main)
        bindViews()
        setupListeners()
        updateAuthMode(AuthMode.LOGIN)
        showUnauthenticatedState(clearMessage = true)
    }

    private fun bindViews() {
        feedbackText = findViewById(R.id.feedbackText)
        authCard = findViewById(R.id.authCard)
        authCardTitle = findViewById(R.id.authCardTitle)
        authCardSubtitle = findViewById(R.id.authCardSubtitle)
        modeToggleGroup = findViewById(R.id.modeToggleGroup)
        loginModeButton = findViewById(R.id.loginModeButton)
        registerModeButton = findViewById(R.id.registerModeButton)
        emailInputLayout = findViewById(R.id.emailInputLayout)
        emailEditText = findViewById(R.id.emailEditText)
        passwordInputLayout = findViewById(R.id.passwordInputLayout)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordInputLayout = findViewById(R.id.confirmPasswordInputLayout)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        authProgressIndicator = findViewById(R.id.authProgressIndicator)
        authActionButton = findViewById(R.id.authActionButton)
        sessionCard = findViewById(R.id.sessionCard)
        sessionEmailText = findViewById(R.id.sessionEmailText)
        sessionRoleText = findViewById(R.id.sessionRoleText)
        logoutButton = findViewById(R.id.logoutButton)
    }

    private fun setupListeners() {
        modeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) {
                return@addOnButtonCheckedListener
            }

            val selectedMode = if (checkedId == R.id.registerModeButton) {
                AuthMode.REGISTER
            } else {
                AuthMode.LOGIN
            }

            updateAuthMode(selectedMode)
        }

        authActionButton.setOnClickListener {
            submitAuthentication()
        }

        logoutButton.setOnClickListener {
            sessionManager.clearSession()
            emailEditText.text?.clear()
            passwordEditText.text?.clear()
            confirmPasswordEditText.text?.clear()
            modeToggleGroup.check(R.id.loginModeButton)
            showUnauthenticatedState(clearMessage = true)
            showMessage(getString(R.string.logged_out_message), isError = false)
        }
    }

    private fun updateAuthMode(selectedMode: AuthMode) {
        authMode = selectedMode
        clearFieldErrors()

        val isRegisterMode = selectedMode == AuthMode.REGISTER

        authCardTitle.text = getString(
            if (isRegisterMode) R.string.auth_form_title_register else R.string.auth_form_title_login
        )
        authCardSubtitle.text = getString(
            if (isRegisterMode) R.string.auth_form_subtitle_register else R.string.auth_form_subtitle_login
        )
        confirmPasswordInputLayout.isVisible = isRegisterMode
        authActionButton.text = getString(
            if (isRegisterMode) R.string.auth_action_register else R.string.auth_action_login
        )
    }

    private fun submitAuthentication() {
        clearFieldErrors()

        if (!validateInputs()) {
            return
        }

        val email = emailEditText.text?.toString()?.trim().orEmpty()
        val password = passwordEditText.text?.toString().orEmpty()

        showMessage(getString(R.string.loading_auth), isError = false)
        setLoading(true)

        authJob?.cancel()
        authJob = lifecycleScope.launch {
            val result = if (authMode == AuthMode.LOGIN) {
                authRepository.login(email, password)
            } else {
                authRepository.register(email, password)
            }

            setLoading(false)

            result.onSuccess { authResponse ->
                sessionManager.saveSession(authResponse)
                openDashboard()
            }.onFailure { throwable ->
                showMessage(
                    throwable.message ?: getString(R.string.generic_auth_error),
                    isError = true
                )
            }
        }
    }

    private fun validateInputs(): Boolean {
        val email = emailEditText.text?.toString()?.trim().orEmpty()
        val password = passwordEditText.text?.toString().orEmpty()
        val confirmPassword = confirmPasswordEditText.text?.toString().orEmpty()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.error = getString(R.string.invalid_email_error)
            emailEditText.requestFocus()
            return false
        }

        if (password.isBlank()) {
            passwordInputLayout.error = getString(R.string.password_required_error)
            passwordEditText.requestFocus()
            return false
        }

        if (authMode == AuthMode.REGISTER && password.length < 6) {
            passwordInputLayout.error = getString(R.string.register_password_length_error)
            passwordEditText.requestFocus()
            return false
        }

        if (authMode == AuthMode.REGISTER && confirmPassword.isBlank()) {
            confirmPasswordInputLayout.error = getString(R.string.confirm_password_required_error)
            confirmPasswordEditText.requestFocus()
            return false
        }

        if (authMode == AuthMode.REGISTER && confirmPassword != password) {
            confirmPasswordInputLayout.error = getString(R.string.password_mismatch_error)
            confirmPasswordEditText.requestFocus()
            return false
        }

        return true
    }

    private fun showAuthenticatedState(authResponse: AuthResponse, message: String) {
        authCard.isVisible = false
        sessionCard.isVisible = true
        sessionEmailText.text = getString(R.string.session_email_value, authResponse.email)
        sessionRoleText.text = getString(
            R.string.session_role_value,
            formatRole(authResponse.role)
        )
        showMessage(message, isError = false)
    }

    private fun openDashboard() {
        startActivity(DashboardActivity.createIntent(this))
        finish()
    }

    private fun showUnauthenticatedState(clearMessage: Boolean) {
        authCard.isVisible = true
        sessionCard.isVisible = false
        setLoading(false)
        clearFieldErrors()

        if (clearMessage) {
            feedbackText.isVisible = false
            feedbackText.text = ""
        }
    }

    private fun setLoading(isLoading: Boolean) {
        authProgressIndicator.isVisible = isLoading
        authActionButton.isEnabled = !isLoading
        loginModeButton.isEnabled = !isLoading
        registerModeButton.isEnabled = !isLoading
        emailEditText.isEnabled = !isLoading
        passwordEditText.isEnabled = !isLoading
        confirmPasswordEditText.isEnabled = !isLoading
        logoutButton.isEnabled = !isLoading
    }

    private fun clearFieldErrors() {
        emailInputLayout.error = null
        passwordInputLayout.error = null
        confirmPasswordInputLayout.error = null
    }

    private fun showMessage(message: String, isError: Boolean) {
        feedbackText.isVisible = true
        feedbackText.text = message
        feedbackText.setTextColor(
            getColor(
                if (isError) {
                    R.color.guard_error
                } else {
                    R.color.guard_success
                }
            )
        )
    }

    private fun formatRole(role: String): String {
        val normalizedRole = role.lowercase(Locale.getDefault())

        return normalizedRole.replaceFirstChar { firstCharacter ->
            if (firstCharacter.isLowerCase()) {
                firstCharacter.titlecase(Locale.getDefault())
            } else {
                firstCharacter.toString()
            }
        }
    }

    override fun onDestroy() {
        authJob?.cancel()
        super.onDestroy()
    }
}
