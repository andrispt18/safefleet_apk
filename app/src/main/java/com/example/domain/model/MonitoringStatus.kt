package com.example.domain.model

data class GpsInfo(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val speedKmh: Float = 0f,
    val accuracyMeters: Float = 0f,
    val isFixAcquired: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class MonitoringStatus(
    val isMonitoring: Boolean = false,
    val isMonitoringActive: Boolean = false,
    val isCameraActive: Boolean = false,
    val isGpsActive: Boolean = false,
    val isAlarmPlaying: Boolean = false,
    val isVibrating: Boolean = false,
    val isScreenFlashing: Boolean = false,
    val sessionStartTime: Long? = null,
    val totalAlertsTriggered: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val currentSpeedKmh: Float = 0f,
    val gpsAccuracyMeters: Float = 0f,
    val pendingUploadsCount: Int = 0,
    val faceDetected: Boolean = false,
    val fps: Double = 0.0,
    val frameCount: Long = 0L,
    val isGasSensorConnected: Boolean = false,
    val gasSensorNotice: String = "Hardware gas sensor not connected (Ready for Bluetooth/USB-OTG)",
    val lastStateChangeTime: Long = System.currentTimeMillis()
) {
    val gpsInfo: GpsInfo
        get() = GpsInfo(
            latitude = latitude,
            longitude = longitude,
            speedKmh = currentSpeedKmh,
            accuracyMeters = gpsAccuracyMeters,
            isFixAcquired = latitude != null && longitude != null
        )
}
