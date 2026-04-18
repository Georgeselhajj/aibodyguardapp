package com.aibodyguard.app.network

import com.aibodyguard.app.enrollment.network.EnrollmentApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit instance that points at the Raspberry Pi enrollment server
 * (Flask on port 5000).
 *
 * Set the Pi's LAN IP once via [setPiIpAddress] before first use.
 * Default: 192.168.1.100 — replace with your Pi's actual address.
 */
object RobotRetrofitClient {

    // ------------------------------------------------------------------ config

    /** Mutable so it can be updated from a settings screen later. */
    private var piIpAddress: String = "192.168.1.100"
    private const val PI_PORT       = 5000

    fun setPiIpAddress(ip: String) {
        require(ip.isNotBlank()) { "IP address must not be blank" }
        piIpAddress = ip.trim()
        // Invalidate cached instance so next access rebuilds with the new URL
        _enrollmentApi = null
    }

    fun getPiBaseUrl(): String = "http://$piIpAddress:$PI_PORT/"

    // ------------------------------------------------------------------ HTTP client

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            // Large write timeout: 48 images × ~60 KB each ≈ 3 MB payload over WiFi
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    // BASIC avoids logging multi-megabyte base64 bodies
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()
    }

    // ------------------------------------------------------------------ lazy API instances

    private var _enrollmentApi: EnrollmentApi? = null

    val enrollmentApi: EnrollmentApi
        get() = _enrollmentApi ?: buildRetrofit()
            .create(EnrollmentApi::class.java)
            .also { _enrollmentApi = it }

    // ------------------------------------------------------------------ builder

    private fun buildRetrofit(): Retrofit =
        Retrofit.Builder()
            .baseUrl(getPiBaseUrl())
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
}
