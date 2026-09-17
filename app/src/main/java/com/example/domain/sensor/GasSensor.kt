package com.example.domain.sensor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Gas sensor reading model representing vehicle cabin telemetry.
 * Contains:
 * - Carbon Monoxide (CO in PPM)
 * - Carbon Dioxide (CO2 in PPM)
 * - Volatile Organic Compounds (VOC in PPM / AQI)
 * - Timestamp of acquisition
 * - Hardware availability indicator
 */
data class GasSensorReading(
    val co: Float? = null,
    val co2: Float? = null,
    val voc: Float? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val sensorAvailable: Boolean = false,
    val deviceIdentifier: String? = null
)

/**
 * Clean architectural abstraction for external vehicle cabin gas sensors (CO, CO2, VOC).
 * Enables plug-and-play integrations via Bluetooth Low Energy (BLE), USB-OTG, or CAN-bus hardware.
 */
interface GasSensor {
    val isConnected: Boolean
    val hardwareIdentifier: String?
    fun observeReadings(): Flow<GasSensorReading>
    suspend fun connect(): Boolean
    suspend fun disconnect()
}

/**
 * Default decoupled implementation when no external gas hardware is attached.
 * Strictly avoids generating fake sensor values as real telemetry.
 */
class DisconnectedGasSensor : GasSensor {
    override val isConnected: Boolean = false
    override val hardwareIdentifier: String? = null

    override fun observeReadings(): Flow<GasSensorReading> = flowOf(
        GasSensorReading(
            co = null,
            co2 = null,
            voc = null,
            timestamp = System.currentTimeMillis(),
            sensorAvailable = false,
            deviceIdentifier = null
        )
    )

    override suspend fun connect(): Boolean = false
    override suspend fun disconnect() {}
}
