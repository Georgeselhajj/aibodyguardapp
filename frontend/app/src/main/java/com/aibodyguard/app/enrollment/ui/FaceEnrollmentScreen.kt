package com.aibodyguard.app.enrollment.ui

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Base64
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aibodyguard.app.enrollment.FaceEnrollmentViewModel
import com.aibodyguard.app.enrollment.model.EnrollmentResult
import com.aibodyguard.app.enrollment.model.EnrollmentStage
import com.aibodyguard.app.enrollment.model.EnrollmentUiState
import com.aibodyguard.app.enrollment.model.FaceAnalysisResult
import com.aibodyguard.app.enrollment.model.PersonRole
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// ============================================================
// Entry point
// ============================================================

@Composable
fun FaceEnrollmentRoute(
    name:                  String,
    role:                  PersonRole,
    onEnrollmentComplete:  (EnrollmentResult.Success) -> Unit,
    onCancel:              () -> Unit,
    viewModel:             FaceEnrollmentViewModel = viewModel(),
) {
    val context  = LocalContext.current
    val uiState  by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(name, role) { viewModel.init(name, role) }

    // Auto-upload when all stages are captured
    LaunchedEffect(uiState.allStagesDone) {
        if (uiState.allStagesDone && !uiState.isUploading && uiState.result == null) {
            viewModel.uploadImages()
        }
    }

    // Navigate on success
    LaunchedEffect(uiState.result) {
        (uiState.result as? EnrollmentResult.Success)?.let { onEnrollmentComplete(it) }
    }

    // ---- Permission handling ----
    var cameraGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> cameraGranted = granted }

    LaunchedEffect(Unit) {
        if (!cameraGranted) permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }

    if (!cameraGranted) {
        CameraPermissionDeniedScreen(
            onRequest = { permissionLauncher.launch(android.Manifest.permission.CAMERA) },
            onCancel  = onCancel,
        )
        return
    }

    FaceEnrollmentContent(
        uiState         = uiState,
        onFaceResult    = viewModel::onFaceAnalysisResult,
        onRetryUpload   = viewModel::retryUpload,
        onReset         = viewModel::reset,
        onCancel        = onCancel,
    )
}

// ============================================================
// Main content screen
// ============================================================

@Composable
private fun FaceEnrollmentContent(
    uiState:        EnrollmentUiState,
    onFaceResult:   (FaceAnalysisResult) -> Unit,
    onRetryUpload:  () -> Unit,
    onReset:        () -> Unit,
    onCancel:       () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {

        // ---- Camera preview (full screen background) ----
        CameraPreviewView(
            modifier     = Modifier.fillMaxSize(),
            onFaceResult = onFaceResult,
            active       = !uiState.isUploading && uiState.result == null,
        )

        // ---- Dark overlay + oval guide ----
        if (!uiState.isUploading && uiState.result == null) {
            FaceGuideOverlay(
                faceDetected = uiState.faceDetected,
                poseInWindow = uiState.poseInWindow,
                stage        = uiState.stage,
            )
        }

        // ---- Cancel / back button ----
        if (!uiState.isUploading && uiState.result == null) {
            IconButton(
                onClick  = onCancel,
                modifier = Modifier
                    .padding(top = 48.dp, start = 8.dp)
                    .align(Alignment.TopStart),
            ) {
                Icon(
                    imageVector        = Icons.Default.ArrowBack,
                    contentDescription = "Cancel",
                    tint               = Color.White,
                    modifier           = Modifier.size(28.dp),
                )
            }
        }

        // ---- Bottom instruction panel ----
        if (!uiState.isUploading && uiState.result == null) {
            InstructionPanel(
                uiState  = uiState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // ---- Upload progress overlay ----
        if (uiState.isUploading) {
            UploadProgressOverlay(progress = uiState.uploadProgress)
        }

        // ---- Error overlay ----
        if (uiState.error != null && !uiState.isUploading) {
            ErrorOverlay(
                message       = uiState.error,
                onRetry       = onRetryUpload,
                onStartOver   = onReset,
                onCancel      = onCancel,
            )
        }
    }
}

// ============================================================
// CameraX preview + ML Kit analyzer
// ============================================================

@Composable
private fun CameraPreviewView(
    modifier:     Modifier,
    onFaceResult: (FaceAnalysisResult) -> Unit,
    active:       Boolean,
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView    = remember { PreviewView(context) }
    val executor       = remember { Executors.newSingleThreadExecutor() }
    val analyzer       = remember { FaceAnalyzer(onFaceResult) }

    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    LaunchedEffect(active) {
        val future          = ProcessCameraProvider.getInstance(context)
        val cameraProvider  = suspendCoroutine { cont ->
            future.addListener(
                { cont.resume(future.get()) },
                ContextCompat.getMainExecutor(context),
            )
        }

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(480, 640))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(executor, analyzer) }

        cameraProvider.unbindAll()
        if (active) {
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                imageAnalysis,
            )
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

// ============================================================
// Face guide overlay (Canvas — oval hole + ring)
// ============================================================

@Composable
private fun FaceGuideOverlay(
    faceDetected: Boolean,
    poseInWindow: Boolean,
    stage:        EnrollmentStage,
) {
    val ringColor by animateColorAsState(
        targetValue = when {
            poseInWindow  -> Color(0xFF4CAF50)          // green — correct pose
            faceDetected  -> Color(0xFFFFEB3B)           // yellow — face found, wrong angle
            else          -> Color.White.copy(alpha = 0.6f)
        },
        animationSpec = tween(200),
        label = "ringColor",
    )

    Box(modifier = Modifier.fillMaxSize()) {

        // Dark vignette + oval cutout
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val cx         = size.width  / 2f
            val cy         = size.height / 2f
            val ovalW      = size.width  * 0.72f
            val ovalH      = ovalW * 1.38f
            val left       = cx - ovalW / 2f
            val top        = cy - ovalH / 2f - size.height * 0.04f

            // Semi-transparent dark overlay
            drawRect(Color.Black.copy(alpha = 0.52f))

            // Clear the oval area (face window)
            drawOval(
                color     = Color.Transparent,
                topLeft   = androidx.compose.ui.geometry.Offset(left, top),
                size      = androidx.compose.ui.geometry.Size(ovalW, ovalH),
                blendMode = BlendMode.Clear,
            )

            // Coloured guide ring
            drawOval(
                color   = ringColor,
                topLeft = androidx.compose.ui.geometry.Offset(left - 2.dp.toPx(), top - 2.dp.toPx()),
                size    = androidx.compose.ui.geometry.Size(ovalW + 4.dp.toPx(), ovalH + 4.dp.toPx()),
                style   = Stroke(width = 3.dp.toPx()),
            )
        }

        // Direction arrow hint (positioned at edge of oval)
        StageDirectionArrow(
            stage    = stage,
            active   = !poseInWindow && !faceDetected.not(),
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun StageDirectionArrow(
    stage:    EnrollmentStage,
    active:   Boolean,
    modifier: Modifier,
) {
    val (icon, alignment) = when (stage) {
        EnrollmentStage.CENTER -> return  // no arrow for center
        EnrollmentStage.LEFT   -> Icons.Default.KeyboardArrowLeft  to Alignment.CenterStart
        EnrollmentStage.RIGHT  -> Icons.Default.KeyboardArrowRight to Alignment.CenterEnd
        EnrollmentStage.UP     -> Icons.Default.KeyboardArrowUp    to Alignment.TopCenter
        EnrollmentStage.DOWN   -> Icons.Default.KeyboardArrowDown  to Alignment.BottomCenter
    }

    Box(
        modifier     = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Render arrow indicator near the oval edge based on alignment
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 120.dp),
            contentAlignment = alignment,
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.18f),
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = stage.instruction,
                    tint               = Color.White,
                    modifier           = Modifier
                        .padding(6.dp)
                        .size(36.dp),
                )
            }
        }
    }
}

// ============================================================
// Bottom instruction panel
// ============================================================

@Composable
private fun InstructionPanel(
    uiState:  EnrollmentUiState,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color    = Color.Black.copy(alpha = 0.72f),
        shape    = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

            // Stage instruction text
            Text(
                text       = uiState.stage.instruction,
                color      = Color.White,
                fontSize   = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign  = TextAlign.Center,
            )
            Text(
                text     = uiState.stage.subInstruction,
                color    = Color.White.copy(alpha = 0.72f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )

            // Face status hint
            val statusText = when {
                uiState.poseInWindow  -> "Capturing…"
                uiState.faceDetected  -> "Adjust your angle"
                else                  -> "No face detected — move closer"
            }
            val statusColor = when {
                uiState.poseInWindow  -> Color(0xFF4CAF50)
                uiState.faceDetected  -> Color(0xFFFFEB3B)
                else                  -> Color(0xFFFF5252)
            }
            Text(
                text      = statusText,
                color     = statusColor,
                fontSize  = 13.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(Modifier.height(4.dp))

            // Per-stage progress dots row
            StageProgressRow(
                capturedPerStage = uiState.capturedPerStage,
                currentStage     = uiState.stage,
            )

            // Overall linear progress bar
            val overallProgress by animateFloatAsState(
                targetValue   = uiState.overallProgress,
                animationSpec = tween(300),
                label         = "overallProgress",
            )
            LinearProgressIndicator(
                progress      = { overallProgress },
                modifier      = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color         = Color(0xFF4CAF50),
                trackColor    = Color.White.copy(alpha = 0.2f),
            )

            Text(
                text      = "${uiState.totalCaptured} / ${uiState.totalTarget} images",
                color     = Color.White.copy(alpha = 0.60f),
                fontSize  = 12.sp,
            )
        }
    }
}

@Composable
private fun StageProgressRow(
    capturedPerStage: Map<EnrollmentStage, Int>,
    currentStage:     EnrollmentStage,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        EnrollmentStage.values().forEach { stage ->
            val captured = capturedPerStage[stage] ?: 0
            val done     = captured >= stage.targetCount
            val isCurrent = stage == currentStage

            val color = when {
                done      -> Color(0xFF4CAF50)
                isCurrent -> Color(0xFFFFEB3B)
                else      -> Color.White.copy(alpha = 0.3f)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(width = if (isCurrent) 24.dp else 10.dp, height = 10.dp)
                        .clip(CircleShape)
                        .background(color),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text     = stage.name.lowercase().replaceFirstChar { it.uppercase() },
                    color    = color,
                    fontSize = 9.sp,
                )
            }
        }
    }
}

// ============================================================
// Upload overlay
// ============================================================

@Composable
private fun UploadProgressOverlay(progress: Float) {
    Box(
        modifier          = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment  = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            CircularProgressIndicator(
                progress      = { progress },
                modifier      = Modifier.size(72.dp),
                color         = Color(0xFF4CAF50),
                trackColor    = Color.White.copy(alpha = 0.2f),
                strokeWidth   = 6.dp,
            )
            Text(
                text       = "Uploading to robot…",
                color      = Color.White,
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text  = "${(progress * 100).toInt()}%",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
            )
        }
    }
}

// ============================================================
// Error overlay
// ============================================================

@Composable
private fun ErrorOverlay(
    message:     String,
    onRetry:     () -> Unit,
    onStartOver: () -> Unit,
    onCancel:    () -> Unit,
) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1C1C1E),
        ) {
            Column(
                modifier            = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text       = "Upload Failed",
                    color      = Color.White,
                    fontSize   = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text      = message,
                    color     = Color.White.copy(alpha = 0.72f),
                    fontSize  = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(
                        onClick = onStartOver,
                        colors  = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                        ),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Start Over", color = Color.White)
                    }
                    Button(
                        onClick = onRetry,
                        colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    ) {
                        Text("Retry Upload", color = Color.White)
                    }
                }
                FilledTonalButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.08f),
                    ),
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}

// ============================================================
// Camera permission denied screen
// ============================================================

@Composable
private fun CameraPermissionDeniedScreen(
    onRequest: () -> Unit,
    onCancel:  () -> Unit,
) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier            = Modifier.padding(32.dp),
        ) {
            Text(
                "Camera Permission Required",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                "The camera is needed to capture your face for enrollment.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRequest) { Text("Grant Permission") }
            FilledTonalButton(onClick = onCancel) { Text("Cancel") }
        }
    }
}

// ============================================================
// Success screen (shown after ViewModel navigates via LaunchedEffect)
// ============================================================

@Composable
fun EnrollmentSuccessScreen(
    name:     String,
    samples:  Int,
    onDone:   () -> Unit,
) {
    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier            = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector        = Icons.Default.CheckCircle,
                contentDescription = null,
                tint               = Color(0xFF4CAF50),
                modifier           = Modifier.size(80.dp),
            )
            Text(
                "$name Enrolled",
                color      = Color.White,
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
            )
            Text(
                "$samples face samples stored on the robot.",
                color     = Color.White.copy(alpha = 0.7f),
                fontSize  = 14.sp,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onDone,
                colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            ) {
                Text("Done", color = Color.White)
            }
        }
    }
}

// ============================================================
// FaceAnalyzer — CameraX ImageAnalysis.Analyzer
// ============================================================

/**
 * Processes each camera frame:
 * 1. Runs ML Kit face detection (fast, on-device)
 * 2. Reports pose angles (yaw / pitch) to the ViewModel
 * 3. Encodes the current frame to base64 JPEG at most once per [captureIntervalMs]
 *    so the ViewModel has image data ready when the pose is in-window
 */
class FaceAnalyzer(
    private val onResult:          (FaceAnalysisResult) -> Unit,
    private val captureIntervalMs: Long = 500L,
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.20f)
            .build()
    )

    private var lastCaptureMs = 0L

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) { imageProxy.close(); return }

        val inputImage = InputImage.fromMediaImage(
            mediaImage, imageProxy.imageInfo.rotationDegrees
        )

        val now           = System.currentTimeMillis()
        val shouldCapture = (now - lastCaptureMs) >= captureIntervalMs

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                when {
                    faces.isEmpty() ->
                        onResult(FaceAnalysisResult.NoFace)

                    faces.size > 1 ->
                        onResult(FaceAnalysisResult.MultipleFaces)

                    else -> {
                        val face    = faces[0]
                        val base64  = if (shouldCapture) {
                            lastCaptureMs = now
                            imageProxy.toBase64Jpeg()
                        } else ""

                        onResult(
                            FaceAnalysisResult.FaceDetected(
                                yaw         = face.headEulerAngleY,
                                pitch       = face.headEulerAngleX,
                                roll        = face.headEulerAngleZ,
                                imageBase64 = base64,
                            )
                        )
                    }
                }
            }
            .addOnFailureListener { onResult(FaceAnalysisResult.NoFace) }
            .addOnCompleteListener { imageProxy.close() }
    }
}

// ============================================================
// ImageProxy → Base64 JPEG helper
// ============================================================

/**
 * Converts a YUV_420_888 ImageProxy frame to a rotated, down-scaled
 * Base64 JPEG string suitable for transmission to the Pi.
 *
 * Output: ~480×640 portrait JPEG at quality 75 ≈ 45–60 KB before base64.
 */
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun ImageProxy.toBase64Jpeg(quality: Int = 75): String {
    val mediaImage = this.image ?: return ""

    // 1. YUV_420_888 → NV21 bytes
    val yBuffer = mediaImage.planes[0].buffer
    val uBuffer = mediaImage.planes[1].buffer
    val vBuffer = mediaImage.planes[2].buffer
    val ySize   = yBuffer.remaining()
    val uSize   = uBuffer.remaining()
    val vSize   = vBuffer.remaining()
    val nv21    = ByteArray(ySize + uSize + vSize)
    yBuffer.get(nv21, 0, ySize)
    vBuffer.get(nv21, ySize, vSize)          // V plane first for NV21
    uBuffer.get(nv21, ySize + vSize, uSize)

    // 2. NV21 → JPEG
    val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
    val jpegOut  = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, this.width, this.height), quality, jpegOut)
    val jpegBytes = jpegOut.toByteArray()

    // 3. Rotate + scale to portrait 480×640 (or smaller)
    val raw     = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
    val matrix  = Matrix().apply { postRotate(imageInfo.rotationDegrees.toFloat()) }
    val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
    raw.recycle()

    // Scale so the longer side ≤ 640 px (keeps file size manageable)
    val maxDim   = 640
    val scaleFactor = maxDim.toFloat() / maxOf(rotated.width, rotated.height)
    val scaled   = if (scaleFactor < 1f) {
        Bitmap.createScaledBitmap(
            rotated,
            (rotated.width  * scaleFactor).toInt(),
            (rotated.height * scaleFactor).toInt(),
            true,
        )
    } else rotated

    val finalOut = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, quality, finalOut)
    if (scaled !== rotated) scaled.recycle()
    rotated.recycle()

    return Base64.encodeToString(finalOut.toByteArray(), Base64.NO_WRAP)
}
