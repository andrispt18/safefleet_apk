package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DrowsinessApplication
import com.example.alert.AlertManager
import com.example.camera.FaceDetectionResult
import com.example.data.local.DrowsinessEventEntity
import com.example.data.preferences.DriverSettings
import com.example.data.remote.GoogleSheetsSyncService
import com.example.domain.engine.DrowsinessAnalyzer
import com.example.domain.engine.RuleBasedDrowsinessAnalyzer
import com.example.domain.model.CabinAirQualityStatus
import com.example.domain.model.CabinSafetyMetrics
import com.example.domain.model.DrowsinessBreakdown
import com.example.domain.model.DrowsinessState
import com.example.domain.model.FacialMetrics
import com.example.domain.model.MonitoringStatus
import com.example.location.LocationTracker
import com.example.service.DriverMonitoringService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DriverMonitorViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DrowsinessApplication
    private val database = app.database
    private val eventDao = database.drowsinessEventDao()
    private val userPreferences = app.userPreferences
    private val alertManager = AlertManager(application)
    private val locationTracker = LocationTracker(application)
    private val drowsinessAnalyzer: DrowsinessAnalyzer = RuleBasedDrowsinessAnalyzer()
    private val sheetsSyncService = GoogleSheetsSyncService()

    private val _monitoringStatus = MutableStateFlow(MonitoringStatus())
    val monitoringStatus: StateFlow<MonitoringStatus> = _monitoringStatus.asStateFlow()

    private val _facialMetrics = MutableStateFlow<FacialMetrics?>(null)
    val facialMetrics: StateFlow<FacialMetrics?> = _facialMetrics.asStateFlow()

    private val _currentFaceBox = MutableStateFlow<Rect?>(null)
    val currentFaceBox: StateFlow<Rect?> = _currentFaceBox.asStateFlow()

    private val _previewDimensions = MutableStateFlow(Pair(480, 640))
    val previewDimensions: StateFlow<Pair<Int, Int>> = _previewDimensions.asStateFlow()

    private val _drowsinessState = MutableStateFlow(DrowsinessState.NO_FACE)
    val drowsinessState: StateFlow<DrowsinessState> = _drowsinessState.asStateFlow()

    private val _drowsinessBreakdown = MutableStateFlow(DrowsinessBreakdown())
    val drowsinessBreakdown: StateFlow<DrowsinessBreakdown> = _drowsinessBreakdown.asStateFlow()

    private val _cabinMetrics = MutableStateFlow(
        CabinSafetyMetrics(
            carbonMonoxidePpm = null,
            volatileOrganicCompoundsPpm = null,
            cabinAirStatus = CabinAirQualityStatus.OPTIMAL,
            cabinSafetyScore = 100,
            isHardwareConnected = false,
            sensorDeviceName = "Virtual Bus (Interface Ready)"
        )
    )
    val cabinMetrics: StateFlow<CabinSafetyMetrics> = _cabinMetrics.asStateFlow()

    val driverSettings: StateFlow<DriverSettings> = userPreferences.driverSettingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DriverSettings()
        )

    val recentEvents = eventDao.getRecentEvents(50)

    private val _isAlertActive = MutableStateFlow(false)
    val isAlertActive: StateFlow<Boolean> = _isAlertActive.asStateFlow()

    private var lastRecordedEventTimestamp: Long = 0L

    init {
        // Collect GPS updates when available
        viewModelScope.launch(Dispatchers.IO) {
            try {
                locationTracker.getLocationUpdates().collect { loc ->
                    _monitoringStatus.update { current ->
                        current.copy(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            currentSpeedKmh = loc.speedKmh
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    fun startMonitoring() {
        DriverMonitoringService.startService(getApplication())
        _monitoringStatus.update { it.copy(isMonitoring = true) }
        drowsinessAnalyzer.reset()
    }

    fun stopMonitoring() {
        DriverMonitoringService.stopService(getApplication())
        _monitoringStatus.update { it.copy(isMonitoring = false, faceDetected = false) }
        _drowsinessState.value = DrowsinessState.NO_FACE
        drowsinessAnalyzer.reset()
        alertManager.stopAlert()
        _isAlertActive.value = false
    }

    fun onFrameAnalyzed(
        result: FaceDetectionResult,
        imageWidth: Int,
        imageHeight: Int,
        rotationDegrees: Int
    ) {
        if (!_monitoringStatus.value.isMonitoring) return

        _previewDimensions.value = Pair(imageWidth, imageHeight)
        _currentFaceBox.value = result.faceBoundingBox
        _facialMetrics.value = result.metrics

        val analysis = drowsinessAnalyzer.processFrame(result.metrics, driverSettings.value)
        _drowsinessState.value = analysis.state
        _drowsinessBreakdown.value = analysis.breakdown

        _monitoringStatus.update {
            it.copy(
                faceDetected = result.hasFace,
                frameCount = it.frameCount + 1
            )
        }

        // Trigger or silence alerts
        if (analysis.state == DrowsinessState.DROWSY || analysis.state == DrowsinessState.CRITICAL) {
            _isAlertActive.value = true
            alertManager.triggerCriticalAlert()
            recordEventIfNeeded(analysis.state, analysis.score, analysis.breakdown, result.metrics)
        } else if (analysis.state == DrowsinessState.WARNING) {
            alertManager.triggerWarningBeep()
            recordEventIfNeeded(analysis.state, analysis.score, analysis.breakdown, result.metrics)
        } else {
            if (_drowsinessState.value != DrowsinessState.DROWSY && _drowsinessState.value != DrowsinessState.CRITICAL) {
                alertManager.stopAlert()
                _isAlertActive.value = false
            }
        }
    }

    private fun recordEventIfNeeded(
        state: DrowsinessState,
        score: Int,
        breakdown: DrowsinessBreakdown,
        metrics: FacialMetrics?
    ) {
        val now = System.currentTimeMillis()
        // Throttle recording to DB every 5 seconds per event
        if (now - lastRecordedEventTimestamp < 5000L) return
        lastRecordedEventTimestamp = now

        viewModelScope.launch(Dispatchers.IO) {
            val status = _monitoringStatus.value
            val entity = DrowsinessEventEntity(
                timestamp = now,
                drowsinessPercentage = score.toFloat(),
                status = state.label,
                reason = when (state) {
                    DrowsinessState.CRITICAL -> "Critical Drowsiness Emergency - Prolonged Closure"
                    DrowsinessState.DROWSY -> "Driver Drowsiness Alert - High Fatigue"
                    DrowsinessState.WARNING -> "Fatigue Warning - Yawning / Eye Closures"
                    else -> "Normal Driver Monitoring"
                },
                latitude = status.latitude,
                longitude = status.longitude,
                gpsAccuracy = status.gpsAccuracyMeters,
                uploadStatus = "PENDING",
                speedKmh = status.currentSpeedKmh,
                ear = metrics?.ear ?: 0.35f,
                mar = metrics?.mar ?: 0.15f,
                perclos = breakdown.perclos
            )
            val eventId = eventDao.insert(entity)

            // Auto-sync if configured
            val settings = driverSettings.value
            if (settings.autoSyncToSheets && settings.googleSheetsWebhookUrl.isNotBlank()) {
                val success = sheetsSyncService.syncEvent(
                    webhookUrl = settings.googleSheetsWebhookUrl,
                    driverName = settings.driverName,
                    vehicleId = settings.vehicleId,
                    event = entity.copy(id = eventId)
                )
                if (success) {
                    eventDao.markAsSynced(listOf(eventId))
                }
            }
        }
    }

    fun dismissAlert() {
        alertManager.stopAlert()
        _isAlertActive.value = false
    }

    fun saveFullConfiguration(
        driverName: String,
        vehicleId: String,
        soundAlarm: Boolean,
        vibration: Boolean,
        flashing: Boolean,
        alertThresholdPercent: Int,
        earThreshold: Float,
        minEyeClosedSeconds: Float,
        marThreshold: Float,
        minYawnDurationSeconds: Float,
        headPitchSensitivity: Float,
        minHeadDownSeconds: Float,
        faceLostTimeoutSeconds: Float,
        webhookUrl: String,
        autoSync: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.updateDriverProfile(
                name = driverName,
                vehicleId = vehicleId
            )
            userPreferences.updateAlertSettings(
                sound = soundAlarm,
                vibration = vibration,
                flashing = flashing,
                thresholdPercent = alertThresholdPercent
            )
            userPreferences.updateThresholds(
                ear = earThreshold,
                minEyeSeconds = minEyeClosedSeconds,
                mar = marThreshold,
                minYawnSeconds = minYawnDurationSeconds,
                headPitch = headPitchSensitivity,
                minHeadSeconds = minHeadDownSeconds,
                faceLostSeconds = faceLostTimeoutSeconds
            )
            userPreferences.updateCloudAuditConfig(
                spreadsheetId = "",
                webhookUrl = webhookUrl,
                autoSync = autoSync
            )
        }
    }

    fun saveDriverSettings(
        name: String,
        vehicleId: String,
        sound: Boolean,
        vibration: Boolean,
        sensitivity: Float,
        webhookUrl: String,
        autoSync: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            userPreferences.updateDriverInfo(name, vehicleId)
            userPreferences.updateAlertSettings(sound, vibration, sensitivity)
            userPreferences.updateSheetsConfig(webhookUrl, autoSync)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            eventDao.clearAll()
        }
    }

    fun syncPendingEventsToSheets(onComplete: (Int, Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val settings = driverSettings.value
            if (settings.googleSheetsWebhookUrl.isBlank()) {
                onComplete(0, false)
                return@launch
            }
            val unsynced = eventDao.getUnsyncedEvents()
            if (unsynced.isEmpty()) {
                onComplete(0, true)
                return@launch
            }

            val syncedIds = mutableListOf<Long>()
            for (event in unsynced) {
                val ok = sheetsSyncService.syncEvent(
                    webhookUrl = settings.googleSheetsWebhookUrl,
                    driverName = settings.driverName,
                    vehicleId = settings.vehicleId,
                    event = event
                )
                if (ok) {
                    syncedIds.add(event.id)
                }
            }

            if (syncedIds.isNotEmpty()) {
                eventDao.markAsSynced(syncedIds)
            }
            onComplete(syncedIds.size, syncedIds.isNotEmpty())
        }
    }

    override fun onCleared() {
        super.onCleared()
        alertManager.release()
    }
}
