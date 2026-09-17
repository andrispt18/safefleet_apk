package com.example.domain.model

data class DrowsinessBreakdown(
    val totalProbability: Float = 0.0f,
    val state: DrowsinessState = DrowsinessState.SAFE,
    val eyeClosureScore: Float = 0.0f,
    val perclosScore: Float = 0.0f,
    val blinkScore: Float = 0.0f,
    val yawnScore: Float = 0.0f,
    val headNoddingScore: Float = 0.0f,
    val faceAbsenceScore: Float = 0.0f,
    val blinkCountPerMinute: Int = 18,
    val perclosValue: Float = 0.0f,
    val primaryTrigger: String = "Kondisi Normal",
    val timestamp: Long = System.currentTimeMillis(),
    val eyeClosureDurationMs: Long = 0L,
    val cabinAirScore: Int = 100,
    val recentYawnCount: Int = 0,
    val lastBlinkDurationMs: Long = 0L
) {
    // Backwards compatibility accessors
    val drowsinessScore: Int get() = (totalProbability * 100).toInt()
    val perclos: Float get() = perclosValue
    val blinksPerMinute: Int get() = blinkCountPerMinute
    val isHeadNodding: Boolean get() = headNoddingScore > 0.5f
    val yawnCountRecent: Int get() = recentYawnCount
}
