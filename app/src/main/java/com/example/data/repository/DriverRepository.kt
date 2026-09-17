package com.example.data.repository

import com.example.data.local.DrowsinessEventDao
import com.example.data.local.DrowsinessEventEntity
import com.example.data.preferences.DriverSettings
import com.example.data.preferences.UserPreferences
import com.example.data.remote.GoogleSheetsSyncService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

class DriverRepository(
    private val eventDao: DrowsinessEventDao,
    private val preferences: UserPreferences,
    private val sheetsSyncService: GoogleSheetsSyncService = GoogleSheetsSyncService()
) {

    val driverSettingsFlow: Flow<DriverSettings> = preferences.driverSettingsFlow
    val driverSettingsStateFlow: StateFlow<DriverSettings> = preferences.preferencesStateFlow

    val allEvents: Flow<List<DrowsinessEventEntity>> = eventDao.getAllEvents()
    val pendingUploadCount: Flow<Int> = eventDao.getPendingCount()

    fun getRecentEvents(limit: Int): Flow<List<DrowsinessEventEntity>> = eventDao.getRecentEvents(limit)

    suspend fun recordEvent(event: DrowsinessEventEntity): Long = eventDao.insert(event)

    suspend fun updateEvent(event: DrowsinessEventEntity) = eventDao.update(event)

    suspend fun deleteEvent(event: DrowsinessEventEntity) = eventDao.delete(event)

    suspend fun clearAllEvents() = eventDao.clearAll()

    suspend fun getPendingUploadEvents(): List<DrowsinessEventEntity> = eventDao.getPendingUploadEvents()

    suspend fun updateUploadStatus(id: Long, status: String, error: String? = null) =
        eventDao.updateUploadStatus(id, status, error)

    suspend fun syncPendingEvents(): Pair<Int, Boolean> {
        val settings = driverSettingsStateFlow.value
        val webhookUrl = settings.googleAppsScriptUrl
        if (webhookUrl.isBlank()) return Pair(0, false)

        val pending = eventDao.getPendingUploadEvents()
        if (pending.isEmpty()) return Pair(0, true)

        var syncedCount = 0
        for (event in pending) {
            val success = sheetsSyncService.syncEvent(
                webhookUrl = webhookUrl,
                driverName = settings.driverName,
                vehicleId = settings.vehicleId,
                event = event
            )
            if (success) {
                eventDao.updateUploadStatus(event.id, "UPLOADED", null)
                syncedCount++
            } else {
                eventDao.updateUploadStatus(event.id, "FAILED", "Network/Webhook timeout")
            }
        }
        return Pair(syncedCount, syncedCount > 0)
    }

    suspend fun saveDriverSettings(settings: DriverSettings) {
        preferences.updateDriverProfile(
            name = settings.driverName,
            email = settings.driverEmail,
            vehicleId = settings.vehicleId,
            isLoggedIn = settings.isLoggedIn,
            authProvider = settings.authProvider
        )
        preferences.updateAlertSettings(
            sound = settings.soundAlarmEnabled,
            vibration = settings.vibrationEnabled,
            flashing = settings.screenFlashingEnabled,
            thresholdPercent = settings.alertThresholdPercent,
            cooldownSeconds = settings.cooldownSeconds,
            tone = settings.alarmSoundTone
        )
        preferences.updateCloudAuditConfig(
            spreadsheetId = settings.googleSpreadsheetId,
            webhookUrl = settings.googleAppsScriptUrl,
            autoSync = settings.autoSyncToSheets
        )
        preferences.updateGpsAndOverlay(
            gpsEnabled = settings.gpsEnabled,
            showOverlay = settings.showCameraOverlay
        )
    }
}
