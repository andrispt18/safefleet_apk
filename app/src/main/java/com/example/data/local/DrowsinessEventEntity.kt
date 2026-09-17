package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "drowsiness_events")
data class DrowsinessEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val drowsinessPercentage: Float = 0f,
    val status: String = "SAFE", // SAFE, WARNING, DROWSY, CRITICAL
    val reason: String = "Normal monitoring",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val gpsAccuracy: Float? = null,
    val uploadStatus: String = "PENDING", // PENDING, UPLOADED, FAILED
    val uploadError: String? = null,
    val speedKmh: Float = 0f,
    val ear: Float = 0.35f,
    val mar: Float = 0.15f,
    val perclos: Float = 0f
) {
    // Backwards compatibility properties
    val score: Int get() = drowsinessPercentage.toInt()
    val state: String get() = status
    val isSyncedToSheets: Boolean get() = uploadStatus == "UPLOADED"
}
