package com.example.domain.engine

import com.example.data.preferences.DriverSettings
import com.example.domain.model.DrowsinessBreakdown
import com.example.domain.model.DrowsinessState
import com.example.domain.model.FacialMetrics

/**
 * Contract for drowsiness evaluation engine.
 * Takes raw facial metrics and produces temporal drowsiness state and breakdown.
 */
interface DrowsinessAnalyzer {

    /**
     * Analyzes facial metrics over time given driver settings, producing a comprehensive breakdown.
     */
    fun analyze(
        metrics: FacialMetrics?,
        settings: DriverSettings = DriverSettings()
    ): DrowsinessBreakdown

    /**
     * Backwards-compatible wrapper returning AnalysisResult.
     */
    fun processFrame(
        metrics: FacialMetrics?,
        settings: DriverSettings = DriverSettings()
    ): AnalysisResult

    /**
     * Resets all internal temporal states, sliding windows, timers and smoothers.
     */
    fun reset()

    data class AnalysisResult(
        val state: DrowsinessState,
        val breakdown: DrowsinessBreakdown,
        val score: Int
    )
}
