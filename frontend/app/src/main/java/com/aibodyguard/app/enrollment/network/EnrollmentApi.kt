package com.aibodyguard.app.enrollment.network

import com.aibodyguard.app.enrollment.model.EnrolledPersonInfo
import com.aibodyguard.app.enrollment.model.EnrollmentRequest
import com.aibodyguard.app.enrollment.model.EnrollmentResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

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
}
