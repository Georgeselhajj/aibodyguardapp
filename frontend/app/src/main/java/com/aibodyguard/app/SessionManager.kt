package com.aibodyguard.app

import android.content.Context
import com.aibodyguard.app.network.AuthResponse

class SessionManager(context: Context) {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun saveSession(authResponse: AuthResponse) {
        // commit() is synchronous so the session survives an immediate app kill.
        preferences.edit()
            .putString(KEY_TOKEN, authResponse.token)
            .putString(KEY_EMAIL, authResponse.email)
            .putString(KEY_ROLE, authResponse.role)
            .commit()
    }

    fun getSession(): AuthResponse? {
        val token = preferences.getString(KEY_TOKEN, null) ?: return null
        val email = preferences.getString(KEY_EMAIL, null) ?: return null
        val role = preferences.getString(KEY_ROLE, null) ?: return null

        return AuthResponse(
            token = token,
            email = email,
            role = role
        )
    }

    fun getEmail(): String? = preferences.getString(KEY_EMAIL, null)

    fun clearSession() {
        preferences.edit().clear().commit()
    }

    private companion object {
        const val PREFERENCES_NAME = "ai_bodyguard_session"
        const val KEY_TOKEN = "token"
        const val KEY_EMAIL = "email"
        const val KEY_ROLE = "role"
    }
}
