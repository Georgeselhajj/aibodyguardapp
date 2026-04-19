package com.aibodyguard.app.enrollment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aibodyguard.app.enrollment.model.EnrollmentResult
import com.aibodyguard.app.enrollment.model.EnrollmentStage
import com.aibodyguard.app.enrollment.model.EnrollmentUiState
import com.aibodyguard.app.enrollment.model.FaceAnalysisResult
import com.aibodyguard.app.enrollment.model.PersonRole
import com.aibodyguard.app.enrollment.network.EnrollmentRepository
import com.aibodyguard.app.network.RobotRetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manages the full face-enrollment flow:
 *
 *   1. Guided camera capture → [onFaceAnalysisResult]
 *   2. Per-stage auto-advance when [EnrollmentStage.targetCount] is reached
 *   3. Upload batches to the Pi via [EnrollmentRepository] → [uploadImages]
 */
class FaceEnrollmentViewModel(
    private val repository: EnrollmentRepository =
        EnrollmentRepository(RobotRetrofitClient.enrollmentApi),
) : ViewModel() {

    // ------------------------------------------------------------------ state

    private val _uiState = MutableStateFlow(EnrollmentUiState())
    val uiState: StateFlow<EnrollmentUiState> = _uiState.asStateFlow()

    /** Accumulated base64 JPEG images — kept in memory until upload. */
    private val _capturedImages: MutableList<String> = mutableListOf()

    /**
     * Minimum milliseconds between captures within a single stage.
     * 600 ms gives ~1.7 fps capture rate — fast enough for 48 samples in
     * ~28 s of total guided capture time.
     */
    private val captureIntervalMs = 600L
    private var lastCaptureTimeMs = 0L

    // ------------------------------------------------------------------ init

    fun init(name: String, role: PersonRole) {
        _uiState.update { it.copy(personName = name, personRole = role) }
    }

    // ------------------------------------------------------------------ camera callbacks

    /**
     * Called by the CameraX ImageAnalyzer on every processed frame.
     * Runs on an executor thread — all state mutations are safe because
     * [MutableStateFlow.update] is thread-safe.
     */
    fun onFaceAnalysisResult(result: FaceAnalysisResult) {
        val state = _uiState.value
        if (state.isUploading || state.allStagesDone) return

        when (result) {
            is FaceAnalysisResult.NoFace, is FaceAnalysisResult.MultipleFaces -> {
                _uiState.update { it.copy(faceDetected = false, poseInWindow = false) }
            }

            is FaceAnalysisResult.FaceDetected -> {
                val inWindow = state.stage.poseMatches(result.yaw, result.pitch)
                _uiState.update { it.copy(faceDetected = true, poseInWindow = inWindow) }

                if (inWindow && result.imageBase64.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    if (now - lastCaptureTimeMs >= captureIntervalMs) {
                        lastCaptureTimeMs = now
                        captureFrame(result.imageBase64)
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ capture logic

    private fun captureFrame(base64Jpeg: String) {
        val state       = _uiState.value
        val currentStage = state.stage
        val currentCount = state.capturedPerStage[currentStage] ?: 0

        if (currentCount >= currentStage.targetCount) return

        _capturedImages.add(base64Jpeg)

        val newCount = currentCount + 1
        val updatedMap = state.capturedPerStage.toMutableMap().apply {
            put(currentStage, newCount)
        }

        if (newCount >= currentStage.targetCount) {
            // Advance to next stage or mark capture complete
            val stages    = EnrollmentStage.values()
            val nextStage = stages.getOrNull(currentStage.ordinal + 1)

            _uiState.update {
                it.copy(
                    capturedPerStage = updatedMap,
                    stage            = nextStage ?: currentStage,
                    poseInWindow     = false,
                )
            }
        } else {
            _uiState.update { it.copy(capturedPerStage = updatedMap) }
        }
    }

    // ------------------------------------------------------------------ upload

    /**
     * Upload all captured images to the Pi.
     * Called automatically when [EnrollmentUiState.allStagesDone] becomes true,
     * or can be triggered manually via the UI confirm button.
     */
    fun uploadImages() {
        val state = _uiState.value
        if (state.isUploading || _capturedImages.isEmpty()) return

        val personId = buildPersonId(state.personName, state.personRole)

        _uiState.update { it.copy(isUploading = true, uploadProgress = 0f, error = null) }

        viewModelScope.launch {
            val result = repository.enroll(
                personId   = personId,
                name       = state.personName,
                role       = state.personRole,
                images     = _capturedImages.toList(),
                onProgress = { progress ->
                    _uiState.update { it.copy(uploadProgress = progress) }
                },
            )

            result.fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            result = EnrollmentResult.Success(
                                personId        = response.person_id ?: personId,
                                personName      = response.name ?: state.personName,
                                samplesProcessed = response.samples_processed,
                            ),
                        )
                    }
                },
                onFailure = { throwable ->
                    _uiState.update {
                        it.copy(
                            isUploading = false,
                            error       = throwable.message ?: "Upload failed.",
                        )
                    }
                },
            )
        }
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Retry upload after a transient failure.
     */
    fun retryUpload() {
        _uiState.update { it.copy(error = null) }
        uploadImages()
    }

    /**
     * Reset the entire enrollment session (e.g. user taps "Start over").
     */
    fun reset() {
        _capturedImages.clear()
        lastCaptureTimeMs = 0L
        _uiState.update { state ->
            EnrollmentUiState(
                personName = state.personName,
                personRole = state.personRole,
            )
        }
    }

    private fun buildPersonId(name: String, role: PersonRole): String {
        val sanitized = name.trim().lowercase().replace(Regex("[^a-z0-9]"), "_")
        val prefix    = if (role == PersonRole.OWNER) "owner" else "family"
        return "${prefix}_${sanitized}".take(32)
    }
}
