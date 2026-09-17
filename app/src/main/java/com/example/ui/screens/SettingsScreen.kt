package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CockpitBackground
import com.example.ui.theme.CockpitBorder
import com.example.ui.theme.CockpitSurface
import com.example.ui.theme.CockpitSurfaceVariant
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SafetySafe
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DriverMonitorViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: DriverMonitorViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.driverSettings.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var driverName by remember(settings.driverName) { mutableStateOf(settings.driverName) }
    var vehicleId by remember(settings.vehicleId) { mutableStateOf(settings.vehicleId) }
    var soundEnabled by remember(settings.soundAlertEnabled) { mutableStateOf(settings.soundAlertEnabled) }
    var vibrationEnabled by remember(settings.vibrationAlertEnabled) { mutableStateOf(settings.vibrationAlertEnabled) }
    var flashingEnabled by remember(settings.screenFlashingEnabled) { mutableStateOf(settings.screenFlashingEnabled) }
    var alertThresholdPercent by remember(settings.alertThresholdPercent) { mutableStateOf(settings.alertThresholdPercent) }
    var earThreshold by remember(settings.earSensitivity) { mutableFloatStateOf(settings.earSensitivity) }
    var minEyeClosedSeconds by remember(settings.minEyeClosedSeconds) { mutableFloatStateOf(settings.minEyeClosedSeconds) }
    var marThreshold by remember(settings.marSensitivity) { mutableFloatStateOf(settings.marSensitivity) }
    var minYawnDurationSeconds by remember(settings.minYawnDurationSeconds) { mutableFloatStateOf(settings.minYawnDurationSeconds) }
    var headPitchSensitivity by remember(settings.headNodPitchSensitivity) { mutableFloatStateOf(settings.headNodPitchSensitivity) }
    var minHeadDownSeconds by remember(settings.minHeadDownSeconds) { mutableFloatStateOf(settings.minHeadDownSeconds) }
    var faceLostTimeoutSeconds by remember(settings.faceLostTimeoutSeconds) { mutableFloatStateOf(settings.faceLostTimeoutSeconds) }
    var webhookUrl by remember(settings.googleSheetsWebhookUrl) { mutableStateOf(settings.googleSheetsWebhookUrl) }
    var autoSync by remember(settings.autoSyncToSheets) { mutableStateOf(settings.autoSyncToSheets) }
    var showOverlay by remember(settings.showFaceMeshOverlay) { mutableStateOf(settings.showFaceMeshOverlay) }

    var showGuideDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CockpitBackground)
            .testTag("settings_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "SYSTEM CONFIGURATION",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = CyanAccent,
                    letterSpacing = 1.5.sp
                )
            )
            Text(
                text = "Monitoring & Telemetry Settings",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Section: Driver Identity
            SettingsCard(title = "Driver & Vehicle Identity", icon = Icons.Default.Tune) {
                OutlinedTextField(
                    value = driverName,
                    onValueChange = { driverName = it },
                    label = { Text("Driver Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = CockpitBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = vehicleId,
                    onValueChange = { vehicleId = it },
                    label = { Text("Vehicle ID / Plate") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = CockpitBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section: Alert Preferences
            SettingsCard(title = "Alert & Sensory Feedback", icon = Icons.Default.NotificationsActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Acoustic Audio Alarm", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Audible tone alert on critical drowsiness", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { soundEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Haptic Vibration", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Vibrate smartphone when warning or drowsy", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { vibrationEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Screen Flashing Alert", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Strobe screen red during emergency critical state", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = flashingEnabled,
                        onCheckedChange = { flashingEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Alert Threshold: $alertThresholdPercent%",
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Slider(
                    value = alertThresholdPercent.toFloat(),
                    onValueChange = { alertThresholdPercent = it.toInt() },
                    valueRange = 40f..90f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanAccent,
                        activeTrackColor = CyanAccent,
                        inactiveTrackColor = CockpitBorder
                    ),
                    modifier = Modifier.testTag("alert_threshold_slider")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section: Computer Vision & Sensor Threshold Calibration
            SettingsCard(title = "Detection & Metric Thresholds", icon = Icons.Default.Tune) {
                Text(
                    text = "EAR Threshold (Eye Closure): ${String.format("%.2f", earThreshold)}",
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Slider(
                    value = earThreshold,
                    onValueChange = { earThreshold = it },
                    valueRange = 0.15f..0.35f,
                    steps = 19,
                    colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent),
                    modifier = Modifier.testTag("ear_threshold_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Min Eye Closure Duration: ${String.format("%.1fs", minEyeClosedSeconds)}",
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Slider(
                    value = minEyeClosedSeconds,
                    onValueChange = { minEyeClosedSeconds = it },
                    valueRange = 0.5f..3.5f,
                    steps = 5,
                    colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent),
                    modifier = Modifier.testTag("eye_closure_duration_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "MAR Threshold (Yawn): ${String.format("%.2f", marThreshold)}",
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Slider(
                    value = marThreshold,
                    onValueChange = { marThreshold = it },
                    valueRange = 0.40f..0.80f,
                    steps = 7,
                    colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent),
                    modifier = Modifier.testTag("mar_threshold_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Min Yawn Duration: ${String.format("%.1fs", minYawnDurationSeconds)}",
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Slider(
                    value = minYawnDurationSeconds,
                    onValueChange = { minYawnDurationSeconds = it },
                    valueRange = 1.0f..4.0f,
                    steps = 5,
                    colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent),
                    modifier = Modifier.testTag("yawn_duration_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Min Head Down Duration: ${String.format("%.1fs", minHeadDownSeconds)}",
                    color = TextPrimary,
                    fontSize = 14.sp
                )
                Slider(
                    value = minHeadDownSeconds,
                    onValueChange = { minHeadDownSeconds = it },
                    valueRange = 0.5f..3.0f,
                    steps = 4,
                    colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent),
                    modifier = Modifier.testTag("head_down_duration_slider")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section: Cloud Sync (Google Sheets Webhook)
            SettingsCard(title = "Cloud Audit (Google Sheets)", icon = Icons.Default.Share) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Private Google Sheet Sync", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Logs incidents to your personal sheet via Apps Script", color = TextSecondary, fontSize = 12.sp)
                    }
                    IconButton(onClick = { showGuideDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Setup Guide",
                            tint = CyanAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = webhookUrl,
                    onValueChange = { webhookUrl = it },
                    label = { Text("Apps Script Webhook URL") },
                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = CockpitBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("webhook_url_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Auto-Sync Incidents", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("Automatically post to sheet when drowsiness is logged", color = TextSecondary, fontSize = 12.sp)
                    }
                    Switch(
                        checked = autoSync,
                        onCheckedChange = { autoSync = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Section: Gas Sensor Abstraction Info
            SettingsCard(title = "Cabin Gas Sensor Abstraction", icon = Icons.Default.Air) {
                Text(
                    text = "Hardware Interface Status: Virtual Ready",
                    color = SafetySafe,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "The application includes a decoupled GasSensor abstraction contract. External sensors (such as MQ-135, MQ-7, or SGP30) can be plugged in via Bluetooth Low Energy (BLE) or USB serial without modifying core detection logic.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Settings Button
            Button(
                onClick = {
                    viewModel.saveFullConfiguration(
                        driverName = driverName,
                        vehicleId = vehicleId,
                        soundAlarm = soundEnabled,
                        vibration = vibrationEnabled,
                        flashing = flashingEnabled,
                        alertThresholdPercent = alertThresholdPercent,
                        earThreshold = earThreshold,
                        minEyeClosedSeconds = minEyeClosedSeconds,
                        marThreshold = marThreshold,
                        minYawnDurationSeconds = minYawnDurationSeconds,
                        headPitchSensitivity = headPitchSensitivity,
                        minHeadDownSeconds = minHeadDownSeconds,
                        faceLostTimeoutSeconds = faceLostTimeoutSeconds,
                        webhookUrl = webhookUrl,
                        autoSync = autoSync
                    )
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Configuration saved & applied successfully")
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyanAccent,
                    contentColor = CockpitBackground
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_settings_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAVE CONFIGURATION",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (showGuideDialog) {
            GoogleAppsScriptGuideDialog(onDismiss = { showGuideDialog = false })
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CockpitSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CockpitBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
