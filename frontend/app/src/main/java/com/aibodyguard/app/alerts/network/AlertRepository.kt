package com.aibodyguard.app.alerts.network

import com.google.gson.Gson
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException

class AlertRepository(
    private val alertApi: AlertApi
) {

    private val gson = Gson()

    suspend fun create(title: String, description: String, userEmail: String): Result<AlertDto> {
        return execute { alertApi.create(CreateAlertRequest(title, description, userEmail)) }
    }

    suspend fun list(userEmail: String): Result<List<AlertDto>> {
        return execute { alertApi.list(userEmail) }
    }

    private suspend fun <T> execute(request: suspend () -> Response<T>): Result<T> {
        return try {
            val response = request()
            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Result.failure(IllegalStateException("Server returned an empty response"))
                Result.success(body)
            } else {
                Result.failure(
                    IllegalStateException(extractErrorMessage(response.code(), response.errorBody()))
                )
            }
        } catch (exception: IOException) {
            Result.failure(
                IllegalStateException(
                    "Could not reach the AI Bodyguard backend. Start the Spring server and confirm the base URL in RetrofitClient."
                )
            )
        } catch (exception: Exception) {
            Result.failure(IllegalStateException(exception.message ?: "Alert request failed"))
        }
    }

    private fun extractErrorMessage(statusCode: Int, errorBody: ResponseBody?): String {
        val rawBody = runCatching { errorBody?.string()?.trim().orEmpty() }.getOrDefault("")
        if (rawBody.isBlank()) return "Alert request failed (HTTP $statusCode)"

        val parsed = runCatching {
            gson.fromJson(rawBody, com.aibodyguard.app.network.ApiErrorResponse::class.java)
        }.getOrNull()

        return parsed?.message ?: parsed?.detail ?: parsed?.error ?: rawBody
    }
}
