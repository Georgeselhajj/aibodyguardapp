package com.aibodyguard.app.network

import com.google.gson.Gson
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.IOException

class AuthRepository(
    private val authApi: AuthApi
) {

    private val gson = Gson()

    suspend fun login(email: String, password: String): Result<AuthResponse> {
        return execute { authApi.login(LoginRequest(email, password)) }
    }

    suspend fun register(email: String, password: String): Result<AuthResponse> {
        return execute { authApi.register(RegisterRequest(email, password)) }
    }

    private suspend fun execute(request: suspend () -> Response<AuthResponse>): Result<AuthResponse> {
        return try {
            val response = request()

            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Result.failure(IllegalStateException("Server returned an empty response"))

                Result.success(body)
            } else {
                Result.failure(
                    IllegalStateException(
                        extractErrorMessage(response.code(), response.errorBody())
                    )
                )
            }
        } catch (exception: IOException) {
            Result.failure(
                IllegalStateException(
                    "Could not reach the AI Bodyguard backend. Start the Spring server and confirm the base URL in RetrofitClient."
                )
            )
        } catch (exception: Exception) {
            Result.failure(
                IllegalStateException(exception.message ?: "Authentication request failed")
            )
        }
    }

    private fun extractErrorMessage(statusCode: Int, errorBody: ResponseBody?): String {
        val rawBody = runCatching {
            errorBody?.string()?.trim().orEmpty()
        }.getOrDefault("")

        if (rawBody.isBlank()) {
            return "Authentication failed (HTTP $statusCode)"
        }

        val parsedBody = runCatching {
            gson.fromJson(rawBody, ApiErrorResponse::class.java)
        }.getOrNull()

        return parsedBody?.message
            ?: parsedBody?.detail
            ?: parsedBody?.error
            ?: rawBody
    }
}
