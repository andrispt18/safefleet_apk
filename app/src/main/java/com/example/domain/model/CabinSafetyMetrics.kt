package com.example.domain.model

enum class CabinAirQualityStatus {
    OPTIMAL,
    MODERATE,
    WARNING_CO_ELEVATED,
    HAZARDOUS_VENTILATION_REQUIRED,
    DISCONNECTED
}

data class CabinSafetyMetrics(
    val carbonMonoxidePpm: Float? = null,
    val carbonDioxidePpm: Float? = null,
    val volatileOrganicCompoundsPpm: Float? = null,
    val cabinAirStatus: CabinAirQualityStatus = CabinAirQualityStatus.DISCONNECTED,
    val cabinSafetyScore: Int = 100, // 0 - 100 scale (100 = safe)
    val isHardwareConnected: Boolean = false,
    val sensorDeviceName: String? = null
)
