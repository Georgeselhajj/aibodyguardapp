package com.aibodyguard.app.enrollment.model

// ---------------------------------------------------------------------------
// Domain enums
// ---------------------------------------------------------------------------

enum class PersonRole(val apiValue: String, val displayName: String) {
    OWNER("owner", "Owner"),
    FAMILY_MEMBER("family_member", "Family Member")
}

/**
 * Guided capture stages. Each stage defines the head-pose window that must be
 * satisfied before a frame is auto-captured.
 *
 * ML Kit headEulerAngleY: positive = face turned right (from viewer's POV)
 * ML Kit headEulerAngleX: positive = face tilted upward
 *
 * [minYaw]/[maxYaw] = null means "no constraint on that axis".
 */
enum class EnrollmentStage(
    val instruction:    String,
    val subInstruction: String,
    val targetCount:    Int,
    val minYaw:         Float?,
    val maxYaw:         Float?,
    val minPitch:       Float?,
    val maxPitch:       Float?,
) {
    CENTER(
        instruction    = "Look straight at the camera",
        subInstruction = "Keep your face centred and level",
        targetCount    = 12,
        minYaw = -12f, maxYaw = 12f,
        minPitch = -10f, maxPitch = 10f,
    ),
    LEFT(
        instruction    = "Turn your head left",
        subInstruction = "Slowly rotate left until the arrow turns green",
        targetCount    = 10,
        minYaw = null, maxYaw = -20f,
        minPitch = null, maxPitch = null,
    ),
    RIGHT(
        instruction    = "Turn your head right",
        subInstruction = "Slowly rotate right until the arrow turns green",
        targetCount    = 10,
        minYaw = 20f, maxYaw = null,
        minPitch = null, maxPitch = null,
    ),
    UP(
        instruction    = "Tilt your head up",
        subInstruction = "Look slightly upward",
        targetCount    = 8,
        minYaw = null, maxYaw = null,
        minPitch = 15f, maxPitch = null,
    ),
    DOWN(
        instruction    = "Tilt your head down",
        subInstruction = "Look slightly downward",
        targetCount    = 8,
        minYaw = null, maxYaw = null,
        minPitch = null, maxPitch = -15f,
    );

    /** True if the detected head pose satisfies this stage's angle window. */
    fun poseMatches(yaw: Float, pitch: Float): Boolean {
        if (minYaw   != null && yaw   < minYaw)   return false
        if (maxYaw   != null && yaw   > maxYaw)   return false
        if (minPitch != null && pitch < minPitch)  return false
        if (maxPitch != null && pitch > maxPitch)  return false
        return true
    }

    val totalTarget: Int get() = EnrollmentStage.values().sumOf { it.targetCount }  // 48
}

// ---------------------------------------------------------------------------
// UI state
// ---------------------------------------------------------------------------

data class EnrollmentUiState(
    val personName:       String          = "",
    val personRole:       PersonRole      = PersonRole.FAMILY_MEMBER,
    val stage:            EnrollmentStage = EnrollmentStage.CENTER,
    val capturedPerStage: Map<EnrollmentStage, Int> =
        EnrollmentStage.values().associateWith { 0 },
    val faceDetected:     Boolean         = false,
    val poseInWindow:     Boolean         = false,
    val isUploading:      Boolean         = false,
    val uploadProgress:   Float           = 0f,
    val result:           EnrollmentResult? = null,
    val error:            String?         = null,
) {
    val totalCaptured: Int get() = capturedPerStage.values.sum()
    val totalTarget:   Int get() = EnrollmentStage.values().sumOf { it.targetCount }
    val overallProgress: Float get() =
        if (totalTarget == 0) 0f else totalCaptured.toFloat() / totalTarget
    val stageProgress: Float get() {
        val target = stage.targetCount
        return if (target == 0) 1f
        else (capturedPerStage[stage] ?: 0).toFloat() / target
    }
    val allStagesDone: Boolean get() =
        EnrollmentStage.values().all { (capturedPerStage[it] ?: 0) >= it.targetCount }
}

sealed class EnrollmentResult {
    data class Success(
        val personId:        String,
        val personName:      String,
        val samplesProcessed: Int,
    ) : EnrollmentResult()

    data class Failure(val reason: String) : EnrollmentResult()
}

// ---------------------------------------------------------------------------
// Face analysis result (produced by the CameraX image analyzer)
// ---------------------------------------------------------------------------

sealed class FaceAnalysisResult {
    object NoFace       : FaceAnalysisResult()
    object MultipleFaces : FaceAnalysisResult()

    data class FaceDetected(
        val yaw:         Float,  // headEulerAngleY
        val pitch:       Float,  // headEulerAngleX
        val roll:        Float,  // headEulerAngleZ
        val imageBase64: String, // JPEG base64, captured when poseInWindow
    ) : FaceAnalysisResult()
}

// ---------------------------------------------------------------------------
// Network request / response DTOs
// ---------------------------------------------------------------------------

data class EnrollmentRequest(
    val person_id: String,
    val name:      String,
    val role:      String,
    val images:    List<String>,   // base64-encoded JPEG strings
)

data class EnrollmentResponse(
    val success:           Boolean,
    val person_id:         String?,
    val name:              String?,
    val role:              String?,
    val samples_processed: Int,
    val total_samples:     Int,
    val message:           String?,
    val stats:             EnrollmentStats?,
)

data class EnrollmentStats(
    val total:      Int,
    val blurry:     Int,
    val no_face:    Int,
    val decode_err: Int?,
    val valid:      Int,
)

data class EnrolledPersonInfo(
    val person_id:    String,
    val name:         String,
    val role:         String,
    val sample_count: Int,
    val enrolled_at:  String?,
)
