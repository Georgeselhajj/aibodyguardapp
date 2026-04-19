package com.aibodyguard.app.enrollment.network

import com.aibodyguard.app.enrollment.model.EnrolledPersonInfo
import com.aibodyguard.app.enrollment.model.EnrollmentRequest
import com.aibodyguard.app.enrollment.model.EnrollmentResponse
import com.aibodyguard.app.enrollment.model.PersonRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository layer that wraps [EnrollmentApi].
 *
 * All methods:
 * - Switch to [Dispatchers.IO] automatically.
 * - Return a sealed [Result] so the ViewModel never touches raw HTTP details.
 * - Retry up to [maxRetries] times on transient failures.
 */
class EnrollmentRepository(private val api: EnrollmentApi) {

    private val maxRetries = 2

    // ------------------------------------------------------------------

    /**
     * Upload captured images to the Pi and enroll the person.
     *
     * @param personId  Unique identifier (e.g. "family_1")
     * @param name      Display name shown in the app
     * @param role      [PersonRole]
     * @param images    List of base64-encoded JPEG strings (20–60 recommended)
     * @param onProgress Called with 0f…1f as batches are uploaded
     */
    suspend fun enroll(
        personId:   String,
        name:       String,
        role:       PersonRole,
        images:     List<String>,
        onProgress: (Float) -> Unit = {},
    ): Result<EnrollmentResponse> = withContext(Dispatchers.IO) {

        // Keep request payloads manageable, but avoid a tiny final batch because
        // the Pi enrollment server rejects requests smaller than 10 images.
        val batches      = buildUploadBatches(images)
        var lastResponse: EnrollmentResponse? = null

        batches.forEachIndexed { batchIdx, batch ->
            val request = EnrollmentRequest(
                person_id = personId,
                name      = name,
                role      = role.apiValue,
                images    = batch,
            )

            var attempt = 0
            var success = false
            var lastError: Exception? = null

            while (attempt <= maxRetries && !success) {
                try {
                    val response = api.enrollPerson(request)
                    if (response.isSuccessful) {
                        lastResponse = response.body()
                        success = true
                    } else {
                        lastError = Exception(
                            "HTTP ${response.code()}: ${response.errorBody()?.string()}"
                        )
                    }
                } catch (e: Exception) {
                    lastError = e
                }
                attempt++
            }

            if (!success) {
                return@withContext Result.failure(
                    lastError ?: Exception("Upload failed for batch $batchIdx")
                )
            }

            onProgress((batchIdx + 1).toFloat() / batches.size)
        }

        lastResponse?.let { Result.success(it) }
            ?: Result.failure(Exception("No response received from server."))
    }

    private fun buildUploadBatches(images: List<String>): List<List<String>> {
        val maxBatchSize = 20
        val initialBatches = images.chunked(maxBatchSize)

        if (initialBatches.size <= 1) {
            return initialBatches
        }

        val lastBatchSize = initialBatches.last().size
        if (lastBatchSize >= 10) {
            return initialBatches
        }

        val batchCount = initialBatches.size
        val balancedBatchSize = kotlin.math.ceil(images.size / batchCount.toDouble()).toInt()
        return images.chunked(balancedBatchSize)
    }

    // ------------------------------------------------------------------

    suspend fun listEnrolled(): Result<List<EnrolledPersonInfo>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = api.listEnrolled()
                if (response.isSuccessful) {
                    response.body() ?: emptyList()
                } else {
                    throw Exception("HTTP ${response.code()}")
                }
            }
        }

    // ------------------------------------------------------------------

    suspend fun deletePerson(personId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = api.deletePerson(personId)
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code()}")
                }
            }
        }

    // ------------------------------------------------------------------

    /** Returns true if the Pi enrollment server is reachable. */
    suspend fun isServerReachable(): Boolean = withContext(Dispatchers.IO) {
        runCatching { api.health().isSuccessful }.getOrDefault(false)
    }
}
