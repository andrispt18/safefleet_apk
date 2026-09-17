package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.camera.CameraFrameAnalyzer
import com.example.camera.FaceLandmarkDetector
import com.example.domain.model.DrowsinessState
import com.example.ui.components.DrowsinessGauge
import com.example.ui.components.EmergencyAlertOverlay
import com.example.ui.components.FaceOverlayCanvas
import com.example.ui.theme.CockpitBackground
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SafetyCritical
import com.example.ui.theme.SafetySafe
import com.example.ui.theme.SafetyWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DriverMonitorViewModel
import java.util.concurrent.Executors

@Composable
fun DashboardScreen(
    viewModel: DriverMonitorViewModel,
    onTriggerPip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val monitoringStatus by viewModel.monitoringStatus.collectAsState()
    val drowsinessState by viewModel.drowsinessState.collectAsState()
    val breakdown by viewModel.drowsinessBreakdown.collectAsState()
    val metrics by viewModel.facialMetrics.collectAsState()
    val faceBox by viewModel.currentFaceBox.collectAsState()
    val previewDims by viewModel.previewDimensions.collectAsState()
    val cabinMetrics by viewModel.cabinMetrics.collectAsState()
    val isAlertActive by viewModel.isAlertActive.collectAsState()
    val settings by viewModel.driverSettings.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasCameraPermission = perms[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            )
        }
    }

    val detector = remember { FaceLandmarkDetector() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            detector.close()
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBackground)
            .testTag("dashboard_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "COCKPIT HUD",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = CyanAccent,
                            letterSpacing = 1.5.sp
                        )
                    )
                    Text(
                        text = "${settings.driverName} • ${settings.vehicleId}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTriggerPip,
                        modifier = Modifier
                            .size(44.dp)
                            .background(CockpitSurfaceVariant, CircleShape)
                            .border(1.dp, CockpitBorder, CircleShape)
                            .testTag("pip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = "Mini HUD PiP",
                            tint = CyanAccent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Status pill
                    Box(
                        modifier = Modifier
                            .background(
                                if (monitoringStatus.isMonitoring) SafetySafe.copy(alpha = 0.2f)
                                else CockpitSurfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (monitoringStatus.isMonitoring) SafetySafe else CockpitBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (monitoringStatus.isMonitoring) "ACTIVE" else "STANDBY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = if (monitoringStatus.isMonitoring) SafetySafe else TextSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Camera Viewport with HUD Overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
                    .border(1.5.dp, CockpitBorder, RoundedCornerShape(20.dp))
            ) {
                if (hasCameraPermission && monitoringStatus.isMonitoring) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { analysis ->
                                        analysis.setAnalyzer(
                                            cameraExecutor,
                                            CameraFrameAnalyzer(detector) { result, w, h, rot ->
                                                viewModel.onFrameAnalyzed(result, w, h, rot)
                                            }
                                        )
                                    }

                                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageAnalysis
                                    )
                                } catch (_: Exception) {
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Face Overlay Reticle
                    if (settings.showFaceMeshOverlay) {
                        FaceOverlayCanvas(
                            faceBox = faceBox,
                            metrics = metrics,
                            state = drowsinessState,
                            imageWidth = previewDims.first,
                            imageHeight = previewDims.second,
                            isFrontCamera = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (!hasCameraPermission) "Camera Permission Required"
                                else "Camera feed paused (Press ENGAGE to stream)",
                                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                            )
                        }
                    }
                }

                // Corner telemetry tags on camera view
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(Color(0xAA0B111E), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = null,
                            tint = if (monitoringStatus.faceDetected) SafetySafe else SafetyWarning,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (monitoringStatus.faceDetected) "DRIVER IN SIGHT" else "SEARCHING FACE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (monitoringStatus.faceDetected) SafetySafe else SafetyWarning,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(Color(0xAA0B111E), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${monitoringStatus.currentSpeedKmh.toInt()} KM/H",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Instrument Gauge Section
            Card(
                colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DrowsinessGauge(
                        score = breakdown.drowsinessScore,
                        state = drowsinessState,
                        size = 180.dp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 4-Quadrant Telemetry Matrix
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryTile(
                            label = "EAR (EYE RATIO)",
                            value = String.format("%.2f", metrics?.ear ?: 0.35f),
                            stateColor = if ((metrics?.ear ?: 0.35f) < 0.22f) SafetyCritical else SafetySafe,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TelemetryTile(
                            label = "MAR (MOUTH)",
                            value = String.format("%.2f", metrics?.mar ?: 0.15f),
                            stateColor = if ((metrics?.mar ?: 0.15f) > 0.55f) SafetyWarning else SafetySafe,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TelemetryTile(
                            label = "PERCLOS",
                            value = "${(breakdown.perclos * 100).toInt()}%",
                            stateColor = if (breakdown.perclos > 0.4f) SafetyCritical else SafetySafe,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TelemetryTile(
                            label = "HEAD PITCH",
                            value = "${metrics?.headPitch?.toInt() ?: 0}°",
                            stateColor = if (breakdown.isHeadNodding) SafetyWarning else SafetySafe,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cabin Gas / Air Quality Tile (Decoupled abstraction ready for BLE/OTG hardware)
            Card(
                colors = CardDefaults.cardColors(containerColor = CockpitSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(CyanAccent.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, CyanAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Air,
                                contentDescription = "Cabin Air Safety",
                                tint = CyanAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "CABIN GAS & AIR SENSOR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyanAccent,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Text(
                                text = if (cabinMetrics.isHardwareConnected) "Air Quality: Optimal" else "Gas Sensor: NOT CONNECTED",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (cabinMetrics.isHardwareConnected) TextPrimary else TextSecondary
                                )
                            )
                            Text(
                                text = if (cabinMetrics.isHardwareConnected) "${cabinMetrics.sensorDeviceName ?: "Hardware Active"}" else "Awaiting external sensor hardware (BLE/USB)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(CockpitSurfaceVariant, RoundedCornerShape(8.dp))
                            .border(1.dp, CockpitBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (cabinMetrics.isHardwareConnected) "SCORE ${cabinMetrics.cabinSafetyScore}" else "STANDBY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (cabinMetrics.isHardwareConnected) SafetySafe else TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Master Engagement Action Button
            Button(
                onClick = {
                    if (monitoringStatus.isMonitoring) {
                        viewModel.stopMonitoring()
                    } else {
                        viewModel.startMonitoring()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (monitoringStatus.isMonitoring) SafetyCritical else CyanAccent,
                    contentColor = CockpitBackground
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("toggle_monitoring_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (monitoringStatus.isMonitoring) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (monitoringStatus.isMonitoring) "DISENGAGE MONITORING" else "ENGAGE MONITORING",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Emergency Alert Screen Scrim
        EmergencyAlertOverlay(
            isVisible = isAlertActive,
            score = breakdown.drowsinessScore,
            onDismissAlert = { viewModel.dismissAlert() }
        )
    }
}

@Composable
private fun TelemetryTile(
    label: String,
    value: String,
    stateColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(CockpitSurfaceVariant, RoundedCornerShape(12.dp))
            .border(1.dp, CockpitBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = stateColor,
                    fontSize = 16.sp
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.sp,
                    color = TextSecondary
                )
            )
        }
    }
}
