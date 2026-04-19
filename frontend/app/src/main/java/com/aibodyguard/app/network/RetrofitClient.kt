package com.aibodyguard.app.network

import com.aibodyguard.app.alerts.network.AlertApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // 10.0.2.2 maps the Android emulator back to the host machine.
    // Replace this with your computer's LAN IP when testing on a physical phone.
    private const val BASE_URL = "http://10.42.0.183:8080/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authApi: AuthApi by lazy { retrofit.create(AuthApi::class.java) }

    val alertApi: AlertApi by lazy { retrofit.create(AlertApi::class.java) }
}
