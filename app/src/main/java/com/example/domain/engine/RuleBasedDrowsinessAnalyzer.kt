package com.example.domain.engine

import com.example.data.preferences.DriverSettings
import com.example.domain.model.DrowsinessBreakdown
import com.example.domain.model.DrowsinessState
import com.example.domain.model.FacialMetrics
import java.util.ArrayDeque

/**
 * Multimodal, temporal rule-based implementation of [DrowsinessAnalyzer].
 * Integrates:
 * 1. Eye Closure duration (distinguishing normal blinks from microsleeps/prolonged closure).
 * 2. PERCLOS (Percentage of Eye Closure over sliding temporal window).
 * 3. Blink dynamics (frequency and duration abnormalities).
 * 4. Yawning (sustained MAR > threshold over minYawnDurationSeconds, filtering out speech).
 * 5. Head Nodding (pitch angle detection and repeated drop counts).
 * 6. Face Absence / Distraction with timeout threshold.
 * 7. Weighted fusion with adaptive temporal smoothing (fast escalation, gradual decay).
 * 8. Critical override for severe prolonged closure or head drops.
 */
class RuleBasedDrowsinessAnalyzer(
    private val weights: FusionWeights = FusionWeights()
) : DrowsinessAnalyzer {

    data class FusionWeights(
        val eyeClosureWeight: Float = 0.35f,
        val perclosWeight: Float = 0.25f,
        val yawnWeight: Float = 0.15f,
        val headNoddingWeight: Float = 0.15f,
        val blinkWeight: Float = 0.10f
    )

    private data class PerclosSample(val timestamp: Long, val isClosed: Boolean)
    private data class BlinkRecord(val timestamp: Long, val durationMs: Long)

    companion object {
        private const val PERCLOS_WINDOW_MS = 30_000L
        private const val BLINK_WINDOW_MS = 60_000L
        private const val EVENT_WINDOW_MS = 60_000L
        private const val NORMAL_BLINK_MAX_MS = 400L
    }

    // 1. Eye Closure & Blink State
    @Volatile private var isEyeCurrentlyClosed: Boolean = false
    @Volatile private var eyeClosedStartTime: Long = 0L
    @Volatile private var lastBlinkDurationMs: Long = 0L
    private val blinkHistory = ArrayDeque<BlinkRecord>()

    // 2. PERCLOS Sliding Window
    private val perclosQueue = ArrayDeque<PerclosSample>()

    // 3. Yawning State
    @Volatile private var isMouthCurrentlyOpen: Boolean = false
    @Volatile private var mouthOpenStartTime: Long = 0L
    private val recentYawns = ArrayDeque<Long>()

    // 4. Head Nodding State
    @Volatile private var isHeadCurrentlyDown: Boolean = false
    @Volatile private var headDownStartTime: Long = 0L
    private val recentNods = ArrayDeque<Long>()

    // 5. Face Absence State
    @Volatile private var faceLostStartTime: Long = 0L

    // 6. Temporal Smoothing State
    @Volatile private var smoothedScore: Float = 0.0f
    @Volatile private var firstFrameTimestamp: Long = 0L

    @Synchronized
    override fun analyze(
        metrics: FacialMetrics?,
        settings: DriverSettings
    ): DrowsinessBreakdown {
        val now = metrics?.timestamp ?: System.currentTimeMillis()
        if (firstFrameTimestamp == 0L) {
            firstFrameTimestamp = now
        }

        // --- 6. FACE ABSENCE DETECTION ---
        val isFaceAbsent = metrics == null ||
                !metrics.faceDetected ||
                (!metrics.bothEyesVisible && !metrics.anyEyeVisible) ||
                metrics.isLookingAway

        val faceAbsenceScore: Float
        val faceLostDurationMs: Long
        if (isFaceAbsent) {
            if (faceLostStartTime == 0L) {
                faceLostStartTime = now
            }
            faceLostDurationMs = now - faceLostStartTime
            val timeoutMs = (settings.faceLostTimeoutSeconds * 1000f).toLong().coerceAtLeast(500L)
            faceAbsenceScore = if (faceLostDurationMs >= timeoutMs) {
                1.0f
            } else {
                (faceLostDurationMs.toFloat() / timeoutMs) * 0.30f
            }
        } else {
            faceLostStartTime = 0L
            faceLostDurationMs = 0L
            faceAbsenceScore = 0.0f
        }

        // If face is absent beyond timeout, return NO_FACE state
        val faceTimeoutExceeded = isFaceAbsent && faceLostDurationMs >= (settings.faceLostTimeoutSeconds * 1000f).toLong()
        if (faceTimeoutExceeded) {
            val trigger = if (metrics?.isLookingAway == true) "Pandangan Berpaling" else "Wajah Tidak Terdeteksi"
            return DrowsinessBreakdown(
                totalProbability = 0.0f,
                state = DrowsinessState.NO_FACE,
                eyeClosureScore = 0.0f,
                perclosScore = 0.0f,
                blinkScore = 0.0f,
                yawnScore = 0.0f,
                headNoddingScore = 0.0f,
                faceAbsenceScore = 1.0f,
                blinkCountPerMinute = 0,
                perclosValue = 0.0f,
                primaryTrigger = trigger,
                timestamp = now,
                eyeClosureDurationMs = 0L
            )
        }

        val nonNullMetrics = metrics ?: FacialMetrics(timestamp = now)

        // --- 1. EYE CLOSURE & BLINK DYNAMICS ---
        val earThreshold = settings.earSensitivity
        val isClosedInFrame = nonNullMetrics.isEyeClosed || (nonNullMetrics.eyeAspectRatio < earThreshold)
        var currentEyeClosureDuration = 0L

        if (isClosedInFrame) {
            if (!isEyeCurrentlyClosed) {
                isEyeCurrentlyClosed = true
                eyeClosedStartTime = now
            }
            currentEyeClosureDuration = now - eyeClosedStartTime
        } else {
            if (isEyeCurrentlyClosed) {
                // Completed blink (CLOSED -> OPEN)
                val blinkDuration = now - eyeClosedStartTime
                lastBlinkDurationMs = blinkDuration
                blinkHistory.addLast(BlinkRecord(now, blinkDuration))
                isEyeCurrentlyClosed = false
                eyeClosedStartTime = 0L
            }
            currentEyeClosureDuration = 0L
        }

        // Maintain blink history (last 60s)
        while (blinkHistory.isNotEmpty() && (now - blinkHistory.first.timestamp) > BLINK_WINDOW_MS) {
            blinkHistory.removeFirst()
        }

        // Calculate blink count per minute
        val observationTimeMs = (now - firstFrameTimestamp).coerceAtLeast(1000L)
        val blinkCountPerMinute: Int = if (observationTimeMs >= 30_000L) {
            (blinkHistory.size * (60_000.0 / observationTimeMs.coerceAtMost(60_000L))).toInt()
        } else {
            // Default baseline if insufficient observation window
            if (blinkHistory.isEmpty()) 18 else (blinkHistory.size * (60_000.0 / observationTimeMs)).toInt().coerceIn(10, 40)
        }

        // Calculate Eye Closure Score
        val minEyeClosedMs = (settings.minEyeClosedSeconds * 1000f).toLong().coerceAtLeast(800L)
        val eyeClosureScore: Float = when {
            !isEyeCurrentlyClosed -> {
                // Decay quickly when open, retain small residual if recent blink was abnormal (> 450ms)
                if (lastBlinkDurationMs > NORMAL_BLINK_MAX_MS && (now - (blinkHistory.lastOrNull()?.timestamp ?: 0L)) < 2000L) {
                    0.25f
                } else {
                    0.0f
                }
            }
            currentEyeClosureDuration <= NORMAL_BLINK_MAX_MS -> {
                // Normal blink in progress: do NOT immediately activate drowsiness
                (currentEyeClosureDuration.toFloat() / NORMAL_BLINK_MAX_MS) * 0.15f
            }
            currentEyeClosureDuration < minEyeClosedMs -> {
                // Prolonged closure / microsleep escalating
                0.20f + 0.80f * ((currentEyeClosureDuration - NORMAL_BLINK_MAX_MS).toFloat() / (minEyeClosedMs - NORMAL_BLINK_MAX_MS))
            }
            else -> 1.0f
        }

        // Abnormal blink detection
        val slowBlinkScore = if (lastBlinkDurationMs > NORMAL_BLINK_MAX_MS) {
            ((lastBlinkDurationMs - NORMAL_BLINK_MAX_MS).toFloat() / 500f).coerceIn(0.0f, 1.0f)
        } else 0.0f
        val flutterScore = if (blinkCountPerMinute > 32) {
            ((blinkCountPerMinute - 32).toFloat() / 18f).coerceIn(0.0f, 1.0f)
        } else 0.0f
        val staringScore = if (observationTimeMs > 25_000L && blinkCountPerMinute < 8) {
            ((8 - blinkCountPerMinute).toFloat() / 6f).coerceIn(0.0f, 1.0f)
        } else 0.0f
        val blinkScore = maxOf(slowBlinkScore, maxOf(flutterScore, staringScore)).coerceIn(0.0f, 1.0f)

        // --- 2. PERCLOS SLIDING WINDOW ---
        perclosQueue.addLast(PerclosSample(now, isClosedInFrame))
        while (perclosQueue.isNotEmpty() && (now - perclosQueue.first.timestamp) > PERCLOS_WINDOW_MS) {
            perclosQueue.removeFirst()
        }
        val closedCount = perclosQueue.count { it.isClosed }
        val perclosValue = if (perclosQueue.isNotEmpty()) closedCount.toFloat() / perclosQueue.size else 0.0f
        val perclosScore: Float = when {
            perclosValue < 0.12f -> (perclosValue / 0.12f) * 0.15f
            perclosValue < 0.25f -> 0.15f + ((perclosValue - 0.12f) / 0.13f) * 0.50f
            else -> (0.65f + ((perclosValue - 0.25f) / 0.20f) * 0.35f).coerceAtMost(1.0f)
        }

        // --- 4. YAWNING DETECTION ---
        val marThreshold = settings.marSensitivity
        val isMouthOpenInFrame = nonNullMetrics.mouthAspectRatio > marThreshold || nonNullMetrics.isYawning
        val minYawnDurationMs = (settings.minYawnDurationSeconds * 1000f).toLong().coerceAtLeast(1000L)
        var currentMouthDuration = 0L

        if (isMouthOpenInFrame) {
            if (!isMouthCurrentlyOpen) {
                isMouthCurrentlyOpen = true
                mouthOpenStartTime = now
            }
            currentMouthDuration = now - mouthOpenStartTime
        } else {
            if (isMouthCurrentlyOpen) {
                val mouthDuration = now - mouthOpenStartTime
                // Only consider as confirmed yawn if sustained > minYawnDurationSeconds
                if (mouthDuration >= minYawnDurationMs) {
                    recentYawns.addLast(now)
                }
                isMouthCurrentlyOpen = false
                mouthOpenStartTime = 0L
            }
            currentMouthDuration = 0L
        }

        while (recentYawns.isNotEmpty() && (now - recentYawns.first) > EVENT_WINDOW_MS) {
            recentYawns.removeFirst()
        }
        val isCurrentlyYawning = isMouthCurrentlyOpen && currentMouthDuration >= minYawnDurationMs
        val recentYawnCount = recentYawns.size + (if (isCurrentlyYawning) 1 else 0)

        val yawnScore: Float = when {
            isCurrentlyYawning -> 0.95f
            recentYawns.isNotEmpty() -> if (recentYawns.size >= 2) 0.85f else 0.50f
            isMouthCurrentlyOpen && currentMouthDuration < minYawnDurationMs -> {
                // Short mouth opening (speaking / smiling) does not trigger drowsiness
                (currentMouthDuration.toFloat() / minYawnDurationMs) * 0.10f
            }
            else -> 0.0f
        }

        // --- 5. HEAD NODDING DETECTION ---
        val nodPitchThreshold = settings.headNodPitchSensitivity
        val isNoddingInFrame = nonNullMetrics.headPitch < nodPitchThreshold || nonNullMetrics.isHeadNodding
        val minHeadDownMs = (settings.minHeadDownSeconds * 1000f).toLong().coerceAtLeast(800L)
        var currentHeadDownDuration = 0L

        if (isNoddingInFrame) {
            if (!isHeadCurrentlyDown) {
                isHeadCurrentlyDown = true
                headDownStartTime = now
            }
            currentHeadDownDuration = now - headDownStartTime
        } else {
            if (isHeadCurrentlyDown) {
                val headDownDuration = now - headDownStartTime
                if (headDownDuration >= minHeadDownMs) {
                    recentNods.addLast(now)
                }
                isHeadCurrentlyDown = false
                headDownStartTime = 0L
            }
            currentHeadDownDuration = 0L
        }

        while (recentNods.isNotEmpty() && (now - recentNods.first) > EVENT_WINDOW_MS) {
            recentNods.removeFirst()
        }
        val isCurrentlyNodding = isHeadCurrentlyDown && currentHeadDownDuration >= minHeadDownMs

        val headNoddingScore: Float = when {
            isCurrentlyNodding -> 0.95f
            recentNods.isNotEmpty() -> if (recentNods.size >= 2) 0.85f else 0.55f
            isHeadCurrentlyDown && currentHeadDownDuration < minHeadDownMs -> {
                (currentHeadDownDuration.toFloat() / minHeadDownMs) * 0.15f
            }
            else -> 0.0f
        }

        // --- 7. WEIGHTED FUSION ---
        val rawWeightedScore = (
            (weights.eyeClosureWeight * eyeClosureScore) +
            (weights.perclosWeight * perclosScore) +
            (weights.yawnWeight * yawnScore) +
            (weights.headNoddingWeight * headNoddingScore) +
            (weights.blinkWeight * blinkScore)
        ).coerceIn(0.0f, 1.0f)

        // Elevate score floor when confirmed single indicators occur (yawn or sustained head drop)
        val indicatorFloor = maxOf(
            if (isCurrentlyYawning || recentYawns.isNotEmpty()) 0.50f else 0.0f,
            if (isCurrentlyNodding || recentNods.isNotEmpty()) 0.60f else 0.0f
        )
        val targetScore = maxOf(rawWeightedScore, indicatorFloor).coerceIn(0.0f, 1.0f)

        // Adaptive temporal smoothing: fast response on rising risk, slower decay on recovery
        val alphaUp = 0.50f
        val alphaDown = 0.10f
        smoothedScore = if (targetScore > smoothedScore) {
            smoothedScore + alphaUp * (targetScore - smoothedScore)
        } else {
            smoothedScore + alphaDown * (targetScore - smoothedScore)
        }.coerceIn(0.0f, 1.0f)

        // --- 8. CRITICAL OVERRIDE ---
        // Severe prolonged eye closure (e.g. > 1.4x minEyeClosedSeconds)
        val isCriticalEyeClosure = isEyeCurrentlyClosed && currentEyeClosureDuration >= (minEyeClosedMs * 1.4).toLong()
        // Severe head down (e.g. > 1.3x minHeadDownSeconds)
        val isCriticalHeadDown = isHeadCurrentlyDown && currentHeadDownDuration >= (minHeadDownMs * 1.3).toLong()

        val isCriticalOverride = isCriticalEyeClosure || isCriticalHeadDown
        if (isCriticalOverride) {
            smoothedScore = maxOf(smoothedScore, 0.95f)
        }

        // --- 9. STATE DETERMINATION ---
        val alertThreshold = (settings.alertThresholdPercent.toFloat() / 100f).coerceIn(0.40f, 0.90f)
        val warningThreshold = alertThreshold * 0.55f
        val criticalThreshold = maxOf(0.88f, alertThreshold + 0.12f)

        val state: DrowsinessState = when {
            isCriticalOverride || smoothedScore >= criticalThreshold -> DrowsinessState.CRITICAL
            smoothedScore >= alertThreshold -> DrowsinessState.DROWSY
            smoothedScore >= warningThreshold -> DrowsinessState.WARNING
            else -> DrowsinessState.SAFE
        }

        // --- 10. PRIMARY TRIGGER ---
        val primaryTrigger = when {
            isCriticalEyeClosure || (isEyeCurrentlyClosed && eyeClosureScore >= 0.65f) -> "Mata Terpejam Lama"
            isCriticalHeadDown || isCurrentlyNodding || (isHeadCurrentlyDown && headNoddingScore >= 0.65f) -> "Kepala Menunduk"
            isCurrentlyYawning || yawnScore >= 0.65f -> "Kuap Terdeteksi"
            state == DrowsinessState.SAFE && smoothedScore < 0.25f -> "Kondisi Normal"
            perclosScore >= 0.45f -> "PERCLOS Tinggi"
            blinkScore >= 0.45f -> "Kedipan Abnormal"
            else -> {
                // Find highest contributing factor
                val contributions = listOf(
                    "Mata Terpejam Lama" to (eyeClosureScore * weights.eyeClosureWeight),
                    "PERCLOS Tinggi" to (perclosScore * weights.perclosWeight),
                    "Kuap Terdeteksi" to (yawnScore * weights.yawnWeight),
                    "Kepala Menunduk" to (headNoddingScore * weights.headNoddingWeight),
                    "Kedipan Abnormal" to (blinkScore * weights.blinkWeight)
                )
                val topFactor = contributions.maxByOrNull { it.second }
                if (topFactor != null && topFactor.second > 0.04f) {
                    topFactor.first
                } else {
                    "Kondisi Normal"
                }
            }
        }

        return DrowsinessBreakdown(
            totalProbability = smoothedScore,
            state = state,
            eyeClosureScore = eyeClosureScore,
            perclosScore = perclosScore,
            blinkScore = blinkScore,
            yawnScore = yawnScore,
            headNoddingScore = headNoddingScore,
            faceAbsenceScore = faceAbsenceScore,
            blinkCountPerMinute = blinkCountPerMinute,
            perclosValue = perclosValue,
            primaryTrigger = primaryTrigger,
            timestamp = now,
            eyeClosureDurationMs = currentEyeClosureDuration,
            cabinAirScore = 100,
            recentYawnCount = recentYawnCount,
            lastBlinkDurationMs = lastBlinkDurationMs
        )
    }

    override fun processFrame(
        metrics: FacialMetrics?,
        settings: DriverSettings
    ): DrowsinessAnalyzer.AnalysisResult {
        val breakdown = analyze(metrics, settings)
        return DrowsinessAnalyzer.AnalysisResult(
            state = breakdown.state,
            breakdown = breakdown,
            score = (breakdown.totalProbability * 100).toInt()
        )
    }

    // --- 11. RESET ---
    @Synchronized
    override fun reset() {
        isEyeCurrentlyClosed = false
        eyeClosedStartTime = 0L
        lastBlinkDurationMs = 0L
        blinkHistory.clear()

        perclosQueue.clear()

        isMouthCurrentlyOpen = false
        mouthOpenStartTime = 0L
        recentYawns.clear()

        isHeadCurrentlyDown = false
        headDownStartTime = 0L
        recentNods.clear()

        faceLostStartTime = 0L

        smoothedScore = 0.0f
        firstFrameTimestamp = 0L
    }
}
