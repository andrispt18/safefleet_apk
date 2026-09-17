package com.example.alert

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.domain.model.DrowsinessState
import java.util.concurrent.atomic.AtomicBoolean

class AlertManager(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null
    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val isAlarmActive = AtomicBoolean(false)

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        } catch (_: Exception) {
            // Audio policy or permission fallback
        }
    }

    fun handleState(state: DrowsinessState) {
        when (state) {
            DrowsinessState.CRITICAL, DrowsinessState.DROWSY -> {
                triggerCriticalAlert()
            }
            DrowsinessState.WARNING -> {
                triggerWarningBeep()
            }
            DrowsinessState.SAFE, DrowsinessState.NO_FACE -> {
                stopAlert()
            }
        }
    }

    fun triggerWarningBeep() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
            vibrate(150L)
        } catch (_: Exception) {
        }
    }

    fun triggerCriticalAlert() {
        if (!isAlarmActive.getAndSet(true)) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1200)
                vibrateCritical()
            } catch (_: Exception) {
            }
        }
    }

    fun stopAlert() {
        if (isAlarmActive.getAndSet(false)) {
            try {
                toneGenerator?.stopTone()
                vibrator?.cancel()
            } catch (_: Exception) {
            }
        }
    }

    private fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {
        }
    }

    private fun vibrateCritical() {
        try {
            val pattern = longArrayOf(0, 300, 200, 300, 200, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (_: Exception) {
        }
    }

    fun release() {
        stopAlert()
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {
        }
    }
}
