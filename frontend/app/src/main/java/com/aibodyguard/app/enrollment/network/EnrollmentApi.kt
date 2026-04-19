package com.aibodyguard.app.enrollment.network

import com.aibodyguard.app.dashboard.model.RobotAlertResponse
import com.aibodyguard.app.dashboard.model.RobotCommandResponse
import com.aibodyguard.app.dashboard.model.RobotModeRequest
import com.aibodyguard.app.dashboard.model.RobotStatusResponse
import com.aibodyguard.app.dashboard.model.ThreatEnrollRequest
import com.aibodyguard.app.dashboard.model.ThreatPersonResponse
import com.aibodyguard.app.enrollment.model.EnrolledPersonInfo
import com.aibodyguard.app.enrollment.model.EnrollmentRequest
import com.aibodyguard.app.enrollment.model.EnrollmentResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the Raspberry Pi enrollment server.
 * Base URL: http://<pi-ip>:5000/
 */
interface EnrollmentApi {

    /**
     * Enroll or re-enroll a person.
     * POST /api/v1/enroll
     */
    @POST("api/v1/enroll")
    suspend fun enrollPerson(
        @Body request: EnrollmentRequest,
    ): Response<EnrollmentResponse>

    /**
     * List all enrolled persons.
     * GET /api/v1/enrolled
     */
    @GET("api/v1/enrolled")
    suspend fun listEnrolled(): Response<List<EnrolledPersonInfo>>

    /**
     * Delete an enrolled person by ID.
     * DELETE /api/v1/enroll/{person_id}
     */
    @DELETE("api/v1/enroll/{person_id}")
    suspend fun deletePerson(
        @Path("person_id") personId: String,
    ): Response<Unit>

    /**
     * Health / connectivity check.
     * GET /api/v1/health
     */
    @GET("api/v1/health")
    suspend fun health(): Response<Unit>

    @GET("api/v1/status")
    suspend fun getStatus(): Response<RobotStatusResponse>

    @POST("api/v1/start")
    suspend fun startDetection(): Response<RobotCommandResponse>

    @POST("api/v1/stop")
    suspend fun stopDetection(): Response<RobotCommandResponse>

    @POST("api/v1/mode")
    suspend fun setMode(
        @Body request: RobotModeRequest,
    ): Response<RobotCommandResponse>

    @GET("api/v1/alerts")
    suspend fun getAlerts(
        @Query("limit") limit: Int = 20,
    ): Response<List<RobotAlertResponse>>

    @POST("api/v1/threats")
    suspend fun enrollThreat(
        @Body request: ThreatEnrollRequest,
    ): Response<RobotCommandResponse>

    @GET("api/v1/threats")
    suspend fun listThreats(): Response<List<ThreatPersonResponse>>

    @DELETE("api/v1/threats/{threat_id}")
    suspend fun deleteThreat(
        @Path("threat_id") threatId: String,
    ): Response<RobotCommandResponse>
}
