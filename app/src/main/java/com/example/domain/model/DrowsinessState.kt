package com.example.domain.model

enum class DrowsinessState(
    val label: String,
    val description: String,
    val severityLevel: Int, // 0 = safe, 1 = warning, 2 = drowsy, 3 = critical
    val isCritical: Boolean
) {
    SAFE(
        label = "SAFE",
        description = "Driver is alert, vigilant and attentive",
        severityLevel = 0,
        isCritical = false
    ),
    WARNING(
        label = "WARNING",
        description = "Early fatigue, yawning or reduced attention detected",
        severityLevel = 1,
        isCritical = false
    ),
    DROWSY(
        label = "DROWSY",
        description = "Microsleep or prolonged eye closure detected",
        severityLevel = 2,
        isCritical = true
    ),
    CRITICAL(
        label = "CRITICAL",
        description = "Severe fatigue emergency! Immediate driver takeover required",
        severityLevel = 3,
        isCritical = true
    ),
    NO_FACE(
        label = "NO DRIVER",
        description = "Driver face not visible in cockpit camera zone",
        severityLevel = 0,
        isCritical = false
    )
}
