package com.aibodyguard.app.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aibodyguard.app.dashboard.ui.DashboardRoute
import com.aibodyguard.app.ui.theme.AIBodyguardTheme

class DashboardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AIBodyguardTheme {
                DashboardRoute(
                    onAddTrustedMember = {
                        Toast.makeText(
                            this,
                            "Add trusted member flow coming soon.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            }
        }
    }

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, DashboardActivity::class.java)
    }
}
