package com.example.domain.engine

import com.example.data.preferences.DriverSettings
import com.example.domain.model.DrowsinessState
import com.example.domain.model.FacialMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RuleBasedDrowsinessAnalyzerTest {

    private lateinit var analyzer: RuleBasedDrowsinessAnalyzer
    private val defaultSettings = DriverSettings(
        alertThresholdPercent = 70,
        earSensitivity = 0.22f,
        minEyeClosedSeconds = 1.5f,
        marSensitivity = 0.55f,
        minYawnDurationSeconds = 2.0f,
        headNodPitchSensitivity = -15.0f,
        minHeadDownSeconds = 1.2f,
        faceLostTimeoutSeconds = 3.0f
    )

    @Before
    fun setUp() {
        analyzer = RuleBasedDrowsinessAnalyzer()
        analyzer.reset()
    }

    private fun createFrame(
        timestamp: Long,
        ear: Float = 0.35f,
        isEyeClosed: Boolean = false,
        mar: Float = 0.10f,
        isYawning: Boolean = false,
        pitch: Float = 0.0f,
        isNodding: Boolean = false,
        faceDetected: Boolean = true,
        bothEyesVisible: Boolean = true,
        isLookingAway: Boolean = false
    ): FacialMetrics {
        return FacialMetrics(
            timestamp = timestamp,
            faceDetected = faceDetected,
            leftEyeDetected = bothEyesVisible,
            rightEyeDetected = bothEyesVisible,
            bothEyesVisible = bothEyesVisible,
            anyEyeVisible = bothEyesVisible,
            leftEyeOpenProb = if (isEyeClosed) 0.05f else 0.95f,
            rightEyeOpenProb = if (isEyeClosed) 0.05f else 0.95f,
            avgEyeOpenProb = if (isEyeClosed) 0.05f else 0.95f,
            eyeAspectRatio = ear,
            isEyeClosed = isEyeClosed,
            mouthAspectRatio = mar,
            isYawning = isYawning,
            headPitch = pitch,
            headYaw = 0.0f,
            headRoll = 0.0f,
            isHeadNodding = isNodding,
            isLookingAway = isLookingAway
        )
    }

    // 1. Normal Open Eyes
    @Test
    fun testNormalOpenEyes() {
        var breakdown = analyzer.analyze(null, defaultSettings)
        var t = 1000L
        for (i in 0 until 30) {
            val frame = createFrame(timestamp = t, ear = 0.35f, isEyeClosed = false)
            breakdown = analyzer.analyze(frame, defaultSettings)
            t += 100L
        }

        assertEquals(DrowsinessState.SAFE, breakdown.state)
        assertEquals(0.0f, breakdown.eyeClosureScore, 0.01f)
        assertEquals(0.0f, breakdown.perclosValue, 0.01f)
        assertEquals("Kondisi Normal", breakdown.primaryTrigger)
        assertTrue("Total probability should be low", breakdown.totalProbability < 0.20f)
    }

    // 2. Short Blink
    @Test
    fun testShortBlink() {
        var t = 1000L
        // 1 second open eyes
        for (i in 0 until 10) {
            analyzer.analyze(createFrame(timestamp = t, ear = 0.35f), defaultSettings)
            t += 100L
        }

        // Short blink: closed for 200ms (2 frames: 100ms each)
        analyzer.analyze(createFrame(timestamp = t, ear = 0.08f, isEyeClosed = true), defaultSettings)
        t += 100L
        analyzer.analyze(createFrame(timestamp = t, ear = 0.08f, isEyeClosed = true), defaultSettings)
        t += 100L

        // Reopen eyes
        val afterBlink = analyzer.analyze(createFrame(timestamp = t, ear = 0.35f, isEyeClosed = false), defaultSettings)

        // Normal short blink must NOT trigger drowsiness
        assertEquals(DrowsinessState.SAFE, afterBlink.state)
        assertTrue("Last blink duration should be around 200ms", afterBlink.lastBlinkDurationMs in 150L..300L)
        assertFalse("State must not be critical for a normal blink", afterBlink.state.isCritical)
    }

    // 3. Prolonged Eye Closure
    @Test
    fun testProlongedEyeClosure() {
        var t = 1000L
        // Baseline open
        for (i in 0 until 10) {
            analyzer.analyze(createFrame(timestamp = t, ear = 0.35f), defaultSettings)
            t += 100L
        }

        // Sustained closed eyes for 2.6 seconds (exceeds minEyeClosedSeconds * 1.5)
        var breakdown = analyzer.analyze(createFrame(timestamp = t, ear = 0.05f, isEyeClosed = true), defaultSettings)
        for (i in 0 until 26) {
            t += 100L
            breakdown = analyzer.analyze(createFrame(timestamp = t, ear = 0.05f, isEyeClosed = true), defaultSettings)
        }

        assertTrue("Eye closure duration should exceed 2000ms", breakdown.eyeClosureDurationMs >= 2400L)
        assertTrue("Eye closure score should be high", breakdown.eyeClosureScore >= 0.90f)
        assertTrue(
            "State should be DROWSY or CRITICAL",
            breakdown.state == DrowsinessState.DROWSY || breakdown.state == DrowsinessState.CRITICAL
        )
        assertEquals("Mata Terpejam Lama", breakdown.primaryTrigger)
    }

    // 4. High PERCLOS
    @Test
    fun testHighPerclos() {
        var t = 1000L
        // Alternating pattern with heavy closure over 30 seconds
        var breakdown = analyzer.analyze(createFrame(timestamp = t, ear = 0.35f), defaultSettings)
        for (i in 0 until 100) {
            // 60% closed samples over the window
            val isClosed = (i % 5) < 3
            val ear = if (isClosed) 0.08f else 0.35f
            breakdown = analyzer.analyze(createFrame(timestamp = t, ear = ear, isEyeClosed = isClosed), defaultSettings)
            t += 300L
        }

        assertTrue("PERCLOS value should be high (> 0.40)", breakdown.perclosValue >= 0.40f)
        assertTrue("PERCLOS score should be high (> 0.60)", breakdown.perclosScore >= 0.60f)
        assertTrue("Total probability should be elevated", breakdown.totalProbability >= 0.40f)
    }

    // 5. Yawning (Speech vs Confirmed Yawn)
    @Test
    fun testYawning() {
        var t = 1000L
        // Short mouth opening (talking): 400ms duration
        analyzer.analyze(createFrame(timestamp = t, mar = 0.70f), defaultSettings)
        t += 200L
        analyzer.analyze(createFrame(timestamp = t, mar = 0.70f), defaultSettings)
        t += 200L
        val talkingBreakdown = analyzer.analyze(createFrame(timestamp = t, mar = 0.15f), defaultSettings)

        // Talking should not trigger confirmed yawn count
        assertEquals(0, talkingBreakdown.recentYawnCount)
        assertTrue("Brief mouth open score should be negligible", talkingBreakdown.yawnScore < 0.20f)

        // Sustained yawn for 2.4 seconds (> minYawnDurationSeconds = 2.0s)
        var yawnBreakdown = talkingBreakdown
        for (i in 0 until 24) {
            t += 100L
            yawnBreakdown = analyzer.analyze(createFrame(timestamp = t, mar = 0.70f, isYawning = true), defaultSettings)
        }

        assertTrue("Yawn score should be high", yawnBreakdown.yawnScore >= 0.80f)
        assertTrue("Recent yawn count should be at least 1", yawnBreakdown.recentYawnCount >= 1)
        assertEquals("Kuap Terdeteksi", yawnBreakdown.primaryTrigger)
    }

    // 6. Head Nodding
    @Test
    fun testHeadNod() {
        var t = 1000L
        // Baseline normal posture
        for (i in 0 until 10) {
            analyzer.analyze(createFrame(timestamp = t, pitch = 0f), defaultSettings)
            t += 100L
        }

        // Head down pitch = -25f sustained for 1.6 seconds (> minHeadDownSeconds = 1.2s)
        var nodBreakdown = analyzer.analyze(createFrame(timestamp = t, pitch = -25f), defaultSettings)
        for (i in 0 until 16) {
            t += 100L
            nodBreakdown = analyzer.analyze(createFrame(timestamp = t, pitch = -25f, isNodding = true), defaultSettings)
        }

        assertTrue("Head nodding score should be high", nodBreakdown.headNoddingScore >= 0.80f)
        assertTrue(
            "State should escalate to WARNING, DROWSY, or CRITICAL",
            nodBreakdown.state != DrowsinessState.SAFE
        )
        assertEquals("Kepala Menunduk", nodBreakdown.primaryTrigger)
    }

    // 7. Face Lost
    @Test
    fun testFaceLost() {
        var t = 1000L
        // Initial normal frames
        analyzer.analyze(createFrame(timestamp = t), defaultSettings)
        t += 100L

        // Face lost for brief moment (500ms): should NOT immediately trigger NO_FACE or CRITICAL
        var briefLostBreakdown = analyzer.analyze(createFrame(timestamp = t, faceDetected = false, bothEyesVisible = false), defaultSettings)
        for (i in 0 until 5) {
            t += 100L
            briefLostBreakdown = analyzer.analyze(createFrame(timestamp = t, faceDetected = false, bothEyesVisible = false), defaultSettings)
        }
        assertFalse("Brief face lost should not trigger NO_FACE yet", briefLostBreakdown.state == DrowsinessState.NO_FACE)

        // Face lost sustained for 3.5 seconds (> faceLostTimeoutSeconds = 3.0s)
        var timeoutBreakdown = briefLostBreakdown
        for (i in 0 until 35) {
            t += 100L
            timeoutBreakdown = analyzer.analyze(createFrame(timestamp = t, faceDetected = false, bothEyesVisible = false), defaultSettings)
        }

        assertEquals(DrowsinessState.NO_FACE, timeoutBreakdown.state)
        assertEquals(1.0f, timeoutBreakdown.faceAbsenceScore, 0.01f)
        assertEquals("Wajah Tidak Terdeteksi", timeoutBreakdown.primaryTrigger)
    }

    // 8. Recovery to SAFE
    @Test
    fun testRecoveryToSafe() {
        var t = 1000L
        // Escalate with prolonged eye closure
        for (i in 0 until 20) {
            analyzer.analyze(createFrame(timestamp = t, ear = 0.05f, isEyeClosed = true), defaultSettings)
            t += 100L
        }

        // Now driver recovers and stays fully alert for 12 seconds
        var recoveryBreakdown = analyzer.analyze(createFrame(timestamp = t, ear = 0.35f, isEyeClosed = false), defaultSettings)
        for (i in 0 until 120) {
            t += 100L
            recoveryBreakdown = analyzer.analyze(createFrame(timestamp = t, ear = 0.35f, isEyeClosed = false), defaultSettings)
        }

        assertEquals(DrowsinessState.SAFE, recoveryBreakdown.state)
        assertEquals("Kondisi Normal", recoveryBreakdown.primaryTrigger)
        assertTrue("Smoothed score should decay back to safe range", recoveryBreakdown.totalProbability < 0.30f)
    }

    // 9. Reset Clears All State
    @Test
    fun testResetClearsAllTemporalState() {
        var t = 1000L
        // Cause high score
        for (i in 0 until 20) {
            analyzer.analyze(createFrame(timestamp = t, ear = 0.05f, isEyeClosed = true), defaultSettings)
            t += 100L
        }

        // Reset
        analyzer.reset()

        // Next single normal frame
        val fresh = analyzer.analyze(createFrame(timestamp = t, ear = 0.35f, isEyeClosed = false), defaultSettings)
        assertEquals(DrowsinessState.SAFE, fresh.state)
        assertEquals(0.0f, fresh.totalProbability, 0.01f)
        assertEquals(0.0f, fresh.eyeClosureScore, 0.01f)
        assertEquals(0.0f, fresh.perclosValue, 0.01f)
        assertEquals("Kondisi Normal", fresh.primaryTrigger)
    }

    // 10. State Transition: SAFE -> WARNING
    @Test
    fun testTransitionSafeToWarning() {
        var t = 1000L
        // Baseline SAFE
        val initial = analyzer.analyze(createFrame(timestamp = t, ear = 0.35f), defaultSettings)
        assertEquals(DrowsinessState.SAFE, initial.state)

        // Moderate fatigue: intermittent head nod and mild eye closure
        var warningBreakdown = initial
        for (i in 0 until 14) {
            t += 100L
            warningBreakdown = analyzer.analyze(
                createFrame(timestamp = t, ear = 0.20f, isEyeClosed = false, pitch = -22f, isNodding = true),
                defaultSettings
            )
        }

        assertTrue(
            "State should escalate to WARNING or higher from SAFE",
            warningBreakdown.state == DrowsinessState.WARNING || warningBreakdown.state == DrowsinessState.DROWSY
        )
    }

    // 11. State Transition: WARNING -> DROWSY
    @Test
    fun testTransitionWarningToDrowsy() {
        var t = 1000L
        // First establish WARNING via sustained yawning
        var warningBreakdown = analyzer.analyze(createFrame(timestamp = t, mar = 0.70f, isYawning = true), defaultSettings)
        for (i in 0 until 24) {
            t += 100L
            warningBreakdown = analyzer.analyze(createFrame(timestamp = t, mar = 0.70f, isYawning = true), defaultSettings)
        }
        assertTrue(
            "State should reach at least WARNING",
            warningBreakdown.state == DrowsinessState.WARNING || warningBreakdown.state == DrowsinessState.DROWSY
        )

        // Now escalate with closed eyes
        var drowsyBreakdown = warningBreakdown
        for (i in 0 until 24) {
            t += 100L
            drowsyBreakdown = analyzer.analyze(createFrame(timestamp = t, ear = 0.05f, isEyeClosed = true), defaultSettings)
        }

        assertTrue(
            "State should reach DROWSY or CRITICAL",
            drowsyBreakdown.state == DrowsinessState.DROWSY || drowsyBreakdown.state == DrowsinessState.CRITICAL
        )
    }

    // 12. State Transition: DROWSY -> CRITICAL
    @Test
    fun testTransitionDrowsyToCritical() {
        var t = 1000L
        // Continuous eye closure for > 2.8s
        var criticalBreakdown = analyzer.analyze(createFrame(timestamp = t, ear = 0.02f, isEyeClosed = true), defaultSettings)
        for (i in 0 until 28) {
            t += 100L
            criticalBreakdown = analyzer.analyze(createFrame(timestamp = t, ear = 0.02f, isEyeClosed = true), defaultSettings)
        }

        assertEquals(DrowsinessState.CRITICAL, criticalBreakdown.state)
        assertTrue("Critical state should have score >= 70%", criticalBreakdown.drowsinessScore >= 70)
        assertTrue("State isCritical property should be true", criticalBreakdown.state.isCritical)
    }

    // 13. State Transition: CRITICAL -> SAFE
    @Test
    fun testTransitionCriticalToSafe() {
        var t = 1000L
        // Put in CRITICAL
        for (i in 0 until 30) {
            analyzer.analyze(createFrame(timestamp = t, ear = 0.02f, isEyeClosed = true), defaultSettings)
            t += 100L
        }

        // Driver awakens, opens eyes fully and maintains alert posture for extended period
        var safeBreakdown = analyzer.analyze(createFrame(timestamp = t, ear = 0.35f, isEyeClosed = false), defaultSettings)
        for (i in 0 until 120) {
            t += 100L
            safeBreakdown = analyzer.analyze(createFrame(timestamp = t, ear = 0.35f, isEyeClosed = false, pitch = 0f), defaultSettings)
        }

        assertEquals(DrowsinessState.SAFE, safeBreakdown.state)
        assertFalse("State should not be critical anymore", safeBreakdown.state.isCritical)
    }

    // 14. Weighted Score Components Verification
    @Test
    fun testWeightedScoreCalculation() {
        var t = 1000L
        val breakdown = analyzer.analyze(createFrame(timestamp = t, ear = 0.35f), defaultSettings)
        assertTrue("Weights must produce a normalized probability in [0.0, 1.0]", breakdown.totalProbability in 0.0f..1.0f)
        assertTrue("Drowsiness score must be in range [0, 100]", breakdown.drowsinessScore in 0..100)
    }
}
