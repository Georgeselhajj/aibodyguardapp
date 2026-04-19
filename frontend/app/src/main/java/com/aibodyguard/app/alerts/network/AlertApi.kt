package com.aibodyguard.app.alerts.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface AlertApi {

    @POST("api/alerts")
    suspend fun create(@Body request: CreateAlertRequest): Response<AlertDto>

    @GET("api/alerts")
    suspend fun list(@Query("userEmail") userEmail: String): Response<List<AlertDto>>
}
