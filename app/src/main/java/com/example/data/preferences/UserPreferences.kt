package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "driver_preferences")

data class DriverSettings(
    val alertThresholdPercent: Int = 70,
    val soundAlarmEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val screenFlashingEnabled: Boolean = true,
    val cooldownSeconds: Int = 5,
    val googleSpreadsheetId: String = "",
    val googleAppsScriptUrl: String = "",
    val showCameraOverlay: Boolean = true,
    val earSensitivity: Float = 0.22f,
    val minEyeClosedSeconds: Float = 1.5f,
    val marSensitivity: Float = 0.55f,
    val minYawnDurationSeconds: Float = 2.0f,
    val headNodPitchSensitivity: Float = -15.0f,
    val minHeadDownSeconds: Float = 1.2f,
    val faceLostTimeoutSeconds: Float = 3.0f,
    val alarmSoundTone: String = "EMERGENCY_BEEP",
    val driverName: String = "Fleet Operator",
    val driverEmail: String = "operator@fleet.local",
    val vehicleId: String = "VEHICLE-01",
    val gpsEnabled: Boolean = true,
    val isLoggedIn: Boolean = false,
    val authProvider: String = "LOCAL",
    val autoSyncToSheets: Boolean = false
) {
    // Backwards compatibility properties
    val soundAlertEnabled: Boolean get() = soundAlarmEnabled
    val vibrationAlertEnabled: Boolean get() = vibrationEnabled
    val alertSensitivity: Float get() = (alertThresholdPercent / 70.0f)
    val googleSheetsWebhookUrl: String get() = googleAppsScriptUrl
    val showFaceMeshOverlay: Boolean get() = showCameraOverlay
    val earThreshold: Float get() = earSensitivity
    val marThreshold: Float get() = marSensitivity
}

class UserPreferences(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private object PreferencesKeys {
        val ALERT_THRESHOLD_PERCENT = intPreferencesKey("alert_threshold_percent")
        val SOUND_ALARM_ENABLED = booleanPreferencesKey("sound_alarm_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SCREEN_FLASHING_ENABLED = booleanPreferencesKey("screen_flashing_enabled")
        val COOLDOWN_SECONDS = intPreferencesKey("cooldown_seconds")
        val GOOGLE_SPREADSHEET_ID = stringPreferencesKey("google_spreadsheet_id")
        val GOOGLE_APPS_SCRIPT_URL = stringPreferencesKey("google_apps_script_url")
        val SHOW_CAMERA_OVERLAY = booleanPreferencesKey("show_camera_overlay")
        val EAR_SENSITIVITY = floatPreferencesKey("ear_sensitivity")
        val MIN_EYE_CLOSED_SECONDS = floatPreferencesKey("min_eye_closed_seconds")
        val MAR_SENSITIVITY = floatPreferencesKey("mar_sensitivity")
        val MIN_YAWN_DURATION_SECONDS = floatPreferencesKey("min_yawn_duration_seconds")
        val HEAD_NOD_PITCH_SENSITIVITY = floatPreferencesKey("head_nod_pitch_sensitivity")
        val MIN_HEAD_DOWN_SECONDS = floatPreferencesKey("min_head_down_seconds")
        val FACE_LOST_TIMEOUT_SECONDS = floatPreferencesKey("face_lost_timeout_seconds")
        val ALARM_SOUND_TONE = stringPreferencesKey("alarm_sound_tone")
        val DRIVER_NAME = stringPreferencesKey("driver_name")
        val DRIVER_EMAIL = stringPreferencesKey("driver_email")
        val VEHICLE_ID = stringPreferencesKey("vehicle_id")
        val GPS_ENABLED = booleanPreferencesKey("gps_enabled")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val AUTH_PROVIDER = stringPreferencesKey("auth_provider")
        val AUTO_SYNC_SHEETS = booleanPreferencesKey("auto_sync_sheets")
    }

    val driverSettingsFlow: Flow<DriverSettings> = context.dataStore.data.map { prefs ->
        DriverSettings(
            alertThresholdPercent = prefs[PreferencesKeys.ALERT_THRESHOLD_PERCENT] ?: 70,
            soundAlarmEnabled = prefs[PreferencesKeys.SOUND_ALARM_ENABLED] ?: true,
            vibrationEnabled = prefs[PreferencesKeys.VIBRATION_ENABLED] ?: true,
            screenFlashingEnabled = prefs[PreferencesKeys.SCREEN_FLASHING_ENABLED] ?: true,
            cooldownSeconds = prefs[PreferencesKeys.COOLDOWN_SECONDS] ?: 5,
            googleSpreadsheetId = prefs[PreferencesKeys.GOOGLE_SPREADSHEET_ID] ?: "",
            googleAppsScriptUrl = prefs[PreferencesKeys.GOOGLE_APPS_SCRIPT_URL] ?: "",
            showCameraOverlay = prefs[PreferencesKeys.SHOW_CAMERA_OVERLAY] ?: true,
            earSensitivity = prefs[PreferencesKeys.EAR_SENSITIVITY] ?: 0.22f,
            minEyeClosedSeconds = prefs[PreferencesKeys.MIN_EYE_CLOSED_SECONDS] ?: 1.5f,
            marSensitivity = prefs[PreferencesKeys.MAR_SENSITIVITY] ?: 0.55f,
            minYawnDurationSeconds = prefs[PreferencesKeys.MIN_YAWN_DURATION_SECONDS] ?: 2.0f,
            headNodPitchSensitivity = prefs[PreferencesKeys.HEAD_NOD_PITCH_SENSITIVITY] ?: -15.0f,
            minHeadDownSeconds = prefs[PreferencesKeys.MIN_HEAD_DOWN_SECONDS] ?: 1.2f,
            faceLostTimeoutSeconds = prefs[PreferencesKeys.FACE_LOST_TIMEOUT_SECONDS] ?: 3.0f,
            alarmSoundTone = prefs[PreferencesKeys.ALARM_SOUND_TONE] ?: "EMERGENCY_BEEP",
            driverName = prefs[PreferencesKeys.DRIVER_NAME] ?: "Fleet Operator",
            driverEmail = prefs[PreferencesKeys.DRIVER_EMAIL] ?: "operator@fleet.local",
            vehicleId = prefs[PreferencesKeys.VEHICLE_ID] ?: "VEHICLE-01",
            gpsEnabled = prefs[PreferencesKeys.GPS_ENABLED] ?: true,
            isLoggedIn = prefs[PreferencesKeys.IS_LOGGED_IN] ?: false,
            authProvider = prefs[PreferencesKeys.AUTH_PROVIDER] ?: "LOCAL",
            autoSyncToSheets = prefs[PreferencesKeys.AUTO_SYNC_SHEETS] ?: false
        )
    }

    // Expose as StateFlow for modern reactive consumption
    val preferencesStateFlow: StateFlow<DriverSettings> = driverSettingsFlow
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = DriverSettings()
        )

    suspend fun updateDriverProfile(
        name: String,
        email: String = "operator@fleet.local",
        vehicleId: String,
        isLoggedIn: Boolean = true,
        authProvider: String = "LOCAL"
    ) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.DRIVER_NAME] = name
            prefs[PreferencesKeys.DRIVER_EMAIL] = email
            prefs[PreferencesKeys.VEHICLE_ID] = vehicleId
            prefs[PreferencesKeys.IS_LOGGED_IN] = isLoggedIn
            prefs[PreferencesKeys.AUTH_PROVIDER] = authProvider
        }
    }

    suspend fun updateAlertSettings(
        sound: Boolean,
        vibration: Boolean,
        flashing: Boolean = true,
        thresholdPercent: Int = 70,
        cooldownSeconds: Int = 5,
        tone: String = "EMERGENCY_BEEP"
    ) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SOUND_ALARM_ENABLED] = sound
            prefs[PreferencesKeys.VIBRATION_ENABLED] = vibration
            prefs[PreferencesKeys.SCREEN_FLASHING_ENABLED] = flashing
            prefs[PreferencesKeys.ALERT_THRESHOLD_PERCENT] = thresholdPercent
            prefs[PreferencesKeys.COOLDOWN_SECONDS] = cooldownSeconds
            prefs[PreferencesKeys.ALARM_SOUND_TONE] = tone
        }
    }

    suspend fun updateThresholds(
        ear: Float = 0.22f,
        minEyeSeconds: Float = 1.5f,
        mar: Float = 0.55f,
        minYawnSeconds: Float = 2.0f,
        headPitch: Float = -15.0f,
        minHeadSeconds: Float = 1.2f,
        faceLostSeconds: Float = 3.0f
    ) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.EAR_SENSITIVITY] = ear
            prefs[PreferencesKeys.MIN_EYE_CLOSED_SECONDS] = minEyeSeconds
            prefs[PreferencesKeys.MAR_SENSITIVITY] = mar
            prefs[PreferencesKeys.MIN_YAWN_DURATION_SECONDS] = minYawnSeconds
            prefs[PreferencesKeys.HEAD_NOD_PITCH_SENSITIVITY] = headPitch
            prefs[PreferencesKeys.MIN_HEAD_DOWN_SECONDS] = minHeadSeconds
            prefs[PreferencesKeys.FACE_LOST_TIMEOUT_SECONDS] = faceLostSeconds
        }
    }

    suspend fun updateCloudAuditConfig(
        spreadsheetId: String,
        webhookUrl: String,
        autoSync: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.GOOGLE_SPREADSHEET_ID] = spreadsheetId
            prefs[PreferencesKeys.GOOGLE_APPS_SCRIPT_URL] = webhookUrl
            prefs[PreferencesKeys.AUTO_SYNC_SHEETS] = autoSync
        }
    }

    suspend fun updateGpsAndOverlay(gpsEnabled: Boolean, showOverlay: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.GPS_ENABLED] = gpsEnabled
            prefs[PreferencesKeys.SHOW_CAMERA_OVERLAY] = showOverlay
        }
    }

    // Backwards compatibility functions
    suspend fun updateDriverInfo(driverName: String, vehicleId: String) =
        updateDriverProfile(driverName, "operator@fleet.local", vehicleId)

    suspend fun updateAlertSettings(sound: Boolean, vibration: Boolean, sensitivity: Float) =
        updateAlertSettings(sound, vibration, true, (sensitivity * 70).toInt())

    suspend fun updateSheetsConfig(webhookUrl: String, autoSync: Boolean) =
        updateCloudAuditConfig("", webhookUrl, autoSync)

    suspend fun updateOverlaySetting(show: Boolean) =
        updateGpsAndOverlay(true, show)
}
