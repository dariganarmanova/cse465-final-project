// File: collectors/SimpleKeystrokeCollector.kt
package com.example.adaptivesecurity.context.collectors

import android.content.Context
import android.content.SharedPreferences
import android.view.KeyEvent
import com.example.adaptivesecurity.context.models.*
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * SIMPLE KEYSTROKE COLLECTOR
 * Matches the pattern of your existing collectors (no interface)
 *
 * HOW IT WORKS:
 * 1. Captures every key press and release event
 * 2. Measures timing patterns (how long keys are held, time between keys)
 * 3. Learns your normal typing pattern over time
 * 4. Detects when typing patterns are different (possible intruder)
 */
class SimpleKeystrokeCollector(
    private val context: Context
) {

    // Storage for saving your typing patterns
    private val prefs: SharedPreferences = context.getSharedPreferences("simple_keystroke", Context.MODE_PRIVATE)

    // List to store recent keystrokes
    private val keystrokes = mutableListOf<Keystroke>()
    private val maxHistory = 100 // Keep last 100 keystrokes

    // YOUR PERSONAL TYPING BASELINE (learned over time)
    private var baselineDwellTime = 0.0      // Your average key hold time
    private var baselineFlightTime = 0.0     // Your average time between keys
    private var dwellVariance = 0.0          // How much your dwell time varies
    private var flightVariance = 0.0         // How much your flight time varies
    private var keystrokeCount = 0           // Total keystrokes collected

    // Detection settings (you can adjust these)
    private val anomalyThreshold = 2.5       // How sensitive detection is (lower = more sensitive)
    private val minKeystrokesForBaseline = 50 // Need 50 keystrokes before detecting anomalies

    init {
        loadBaseline() // Load your saved typing patterns when starting
    }

    /**
     * MAIN ANALYSIS FUNCTION
     * Called to analyze current typing and detect anomalies
     * Matches your existing collector pattern: collectXXXContext()
     */
    suspend fun collectKeystrokeContext(): KeyboardContext {
        return withContext(Dispatchers.Default) {

            // Need at least 2 keystrokes to analyze
            if (keystrokes.size < 2) {
                return@withContext createEmptyContext()
            }

            // Get recent keystrokes to analyze
            val recentKeystrokes = keystrokes.takeLast(20)

            // STEP 1: Calculate anomaly score (how different from normal)
            val anomalyScore = calculateSimpleAnomalyScore(recentKeystrokes)

            // STEP 2: Determine if this is anomalous
            val isAnomalous = anomalyScore > anomalyThreshold

            // STEP 3: Update your baseline with new data
            updateBaseline(recentKeystrokes)

            // STEP 4: Return analysis results
            KeyboardContext(
                typingPattern = analyzePattern(recentKeystrokes),
                anomalyScore = anomalyScore / 3.0, // Normalize to 0-1 range
                isAnomalous = isAnomalous,
                recentKeystrokes = recentKeystrokes,
                sessionDuration = calculateSessionDuration(),
                averageWpm = calculateWPM()
            )
        }
    }

    /**
     * ALTERNATIVE METHOD NAME (for consistency)
     * Same as collectKeystrokeContext() but shorter name
     */
    suspend fun collectContext(): KeyboardContext {
        return collectKeystrokeContext()
    }

    /**
     * KEYSTROKE CAPTURE
     * This is called every time a key is pressed or released
     */
    fun addKeystroke(keyEvent: KeyEvent) {
        when (keyEvent.action) {
            KeyEvent.ACTION_DOWN -> handleKeyDown(keyEvent)  // Key pressed
            KeyEvent.ACTION_UP -> handleKeyUp(keyEvent)      // Key released
        }
    }

    /**
     * HANDLE KEY PRESS
     * Records when a key starts being pressed
     */
    private fun handleKeyDown(keyEvent: KeyEvent) {
        // Remove any incomplete keystroke for this key (in case of duplicates)
        keystrokes.removeAll { it.keyCode == keyEvent.keyCode && it.releaseTime == 0L }

        // Create new keystroke record
        val keystroke = Keystroke(
            keyCode = keyEvent.keyCode,           // Which key (A, B, Space, etc.)
            pressTime = keyEvent.eventTime,       // When it was pressed
            releaseTime = 0,                      // Will be filled when released
            pressure = 0f,                        // Touch pressure (not used in statistical method)
            x = 0f, y = 0f                       // Touch coordinates (not used)
        )

        keystrokes.add(keystroke)
        maintainHistory() // Keep only recent keystrokes
    }

    /**
     * HANDLE KEY RELEASE
     * Records when a key stops being pressed (completes the timing measurement)
     */
    private fun handleKeyUp(keyEvent: KeyEvent) {
        // Find the matching key press event
        val index = keystrokes.indexOfLast {
            it.keyCode == keyEvent.keyCode && it.releaseTime == 0L
        }

        if (index != -1) {
            // Complete the keystroke timing
            keystrokes[index] = keystrokes[index].copy(releaseTime = keyEvent.eventTime)
            keystrokeCount++
        }
    }

    /**
     * MAINTAIN HISTORY
     * Keep only recent keystrokes to avoid memory issues
     */
    private fun maintainHistory() {
        while (keystrokes.size > maxHistory) {
            keystrokes.removeAt(0)
        }
    }

    /**
     * ANOMALY DETECTION - THE CORE ALGORITHM
     * This is where the magic happens - detecting when typing is different
     */
    private fun calculateSimpleAnomalyScore(recentKeystrokes: List<Keystroke>): Double {
        // Only analyze complete keystrokes (have both press and release time)
        val completeKeystrokes = recentKeystrokes.filter { it.releaseTime > 0 }

        // Need baseline and data to compare
        if (completeKeystrokes.size < 2 || !hasBaseline()) {
            return 0.0 // No anomaly if no baseline yet
        }

        var totalAnomalyScore = 0.0
        var scoreCount = 0

        // ===== DWELL TIME ANALYSIS =====
        // How long keys are held down
        val dwellTimes = completeKeystrokes.map { it.dwellTime.toDouble() }
        val avgDwell = dwellTimes.average()

        if (baselineDwellTime > 0 && dwellVariance > 0) {
            // Calculate how different current dwell time is from your normal
            val dwellDeviation = abs(avgDwell - baselineDwellTime)
            val dwellStdDev = sqrt(dwellVariance)

            if (dwellStdDev > 0) {
                // Z-score: how many standard deviations away from normal
                totalAnomalyScore += dwellDeviation / dwellStdDev
                scoreCount++
            }
        }

        // ===== FLIGHT TIME ANALYSIS =====
        // Time between consecutive key presses
        val flightTimes = mutableListOf<Double>()
        for (i in 1 until completeKeystrokes.size) {
            val flightTime = (completeKeystrokes[i].pressTime - completeKeystrokes[i-1].pressTime).toDouble()
            if (flightTime > 0) flightTimes.add(flightTime)
        }

        if (flightTimes.isNotEmpty() && baselineFlightTime > 0 && flightVariance > 0) {
            // Calculate how different current flight time is from your normal
            val avgFlight = flightTimes.average()
            val flightDeviation = abs(avgFlight - baselineFlightTime)
            val flightStdDev = sqrt(flightVariance)

            if (flightStdDev > 0) {
                // Z-score for flight times
                totalAnomalyScore += flightDeviation / flightStdDev
                scoreCount++
            }
        }

        // Return average anomaly score
        return if (scoreCount > 0) totalAnomalyScore / scoreCount else 0.0
    }

    /**
     * BASELINE LEARNING
     * Gradually learns your normal typing patterns
     */
    private fun updateBaseline(recentKeystrokes: List<Keystroke>) {
        val completeKeystrokes = recentKeystrokes.filter { it.releaseTime > 0 }
        if (completeKeystrokes.size < 2) return

        // Calculate current session's patterns
        val dwellTimes = completeKeystrokes.map { it.dwellTime.toDouble() }
        val flightTimes = mutableListOf<Double>()

        for (i in 1 until completeKeystrokes.size) {
            val flightTime = (completeKeystrokes[i].pressTime - completeKeystrokes[i-1].pressTime).toDouble()
            if (flightTime > 0) flightTimes.add(flightTime)
        }

        // LEARNING ALGORITHM: Exponential Moving Average
        val learningRate = 0.1 // How fast to adapt (0.1 = slow, gradual learning)
        val newDwellAvg = dwellTimes.average()
        val newFlightAvg = if (flightTimes.isNotEmpty()) flightTimes.average() else baselineFlightTime

        if (baselineDwellTime == 0.0) {
            // FIRST TIME: Initialize baseline with current data
            baselineDwellTime = newDwellAvg
            baselineFlightTime = newFlightAvg
        } else {
            // UPDATE: Blend new data with existing baseline
            baselineDwellTime = learningRate * newDwellAvg + (1 - learningRate) * baselineDwellTime
            baselineFlightTime = learningRate * newFlightAvg + (1 - learningRate) * baselineFlightTime
        }

        // Update variance (how much your timing varies)
        if (dwellTimes.size > 1) {
            val newDwellVariance = dwellTimes.map { (it - baselineDwellTime).pow(2) }.average()
            dwellVariance = learningRate * newDwellVariance + (1 - learningRate) * dwellVariance
        }

        if (flightTimes.size > 1) {
            val newFlightVariance = flightTimes.map { (it - baselineFlightTime).pow(2) }.average()
            flightVariance = learningRate * newFlightVariance + (1 - learningRate) * flightVariance
        }

        // Save baseline every 20 keystrokes
        if (keystrokeCount % 20 == 0) {
            saveBaseline()
        }
    }

    /**
     * CHECK IF BASELINE IS READY
     * Need enough data before we can detect anomalies
     */
    private fun hasBaseline(): Boolean {
        return keystrokeCount >= minKeystrokesForBaseline && baselineDwellTime > 0
    }

    /**
     * TYPING PATTERN ANALYSIS
     * Detailed analysis of current typing patterns
     */
    private fun analyzePattern(keystrokes: List<Keystroke>): TypingPattern {
        val completeKeystrokes = keystrokes.filter { it.releaseTime > 0 }

        if (completeKeystrokes.isEmpty()) {
            return TypingPattern(
                dwellTimes = emptyList(),
                flightTimes = emptyList(),
                rhythm = TypingRhythm(0.0, 0.0, 0.0, 0.0, 0.0),
                pressure = emptyList(),
                accuracy = 1.0
            )
        }

        // Extract timing patterns
        val dwellTimes = completeKeystrokes.map { it.dwellTime.toDouble() }
        val flightTimes = mutableListOf<Double>()

        for (i in 1 until completeKeystrokes.size) {
            val flightTime = (completeKeystrokes[i].pressTime - completeKeystrokes[i-1].pressTime).toDouble()
            if (flightTime > 0) flightTimes.add(flightTime)
        }

        // Calculate rhythm metrics
        val avgDwell = if (dwellTimes.isNotEmpty()) dwellTimes.average() else 0.0
        val avgFlight = if (flightTimes.isNotEmpty()) flightTimes.average() else 0.0

        val dwellVar = if (dwellTimes.size > 1) {
            dwellTimes.map { (it - avgDwell).pow(2) }.average()
        } else 0.0

        val flightVar = if (flightTimes.size > 1) {
            flightTimes.map { (it - avgFlight).pow(2) }.average()
        } else 0.0

        // Consistency score (lower variance = more consistent = higher score)
        val consistency = if (dwellVar + flightVar > 0) {
            1.0 / (1.0 + (dwellVar + flightVar) / 1000.0)
        } else 1.0

        return TypingPattern(
            dwellTimes = dwellTimes,
            flightTimes = flightTimes,
            rhythm = TypingRhythm(
                averageDwellTime = avgDwell,
                averageFlightTime = avgFlight,
                dwellTimeVariance = dwellVar,
                flightTimeVariance = flightVar,
                rhythmConsistency = consistency
            ),
            pressure = emptyList(),
            accuracy = calculateAccuracy()
        )
    }

    /**
     * CALCULATE TYPING SPEED (Words Per Minute)
     */
    private fun calculateWPM(): Double {
        val validKeystrokes = keystrokes.filter { it.releaseTime > 0 }
        if (validKeystrokes.size < 5) return 0.0

        val timeSpan = (validKeystrokes.last().releaseTime - validKeystrokes.first().pressTime) / 1000.0 / 60.0
        return if (timeSpan > 0) (validKeystrokes.size / 5.0) / timeSpan else 0.0
    }

    /**
     * CALCULATE TYPING ACCURACY
     * Based on backspace usage (more backspaces = lower accuracy)
     */
    private fun calculateAccuracy(): Double {
        val backspaceCount = keystrokes.count { it.keyCode == KeyEvent.KEYCODE_DEL }
        val totalKeys = keystrokes.size
        return if (totalKeys > 0) max(0.0, 1.0 - (backspaceCount * 2.0 / totalKeys)) else 1.0
    }

    /**
     * CALCULATE SESSION DURATION
     */
    private fun calculateSessionDuration(): Long {
        return if (keystrokes.isNotEmpty()) {
            keystrokes.last().pressTime - keystrokes.first().pressTime
        } else 0
    }

    /**
     * CREATE EMPTY CONTEXT (when no data available)
     */
    private fun createEmptyContext(): KeyboardContext {
        return KeyboardContext(
            typingPattern = TypingPattern(
                dwellTimes = emptyList(),
                flightTimes = emptyList(),
                rhythm = TypingRhythm(0.0, 0.0, 0.0, 0.0, 0.0),
                pressure = emptyList(),
                accuracy = 1.0
            ),
            anomalyScore = 0.0,
            isAnomalous = false,
            recentKeystrokes = emptyList(),
            sessionDuration = 0,
            averageWpm = 0.0
        )
    }

    /**
     * SAVE YOUR BASELINE TO STORAGE
     * So it persists between app restarts
     */
    private fun saveBaseline() {
        prefs.edit()
            .putFloat("baseline_dwell", baselineDwellTime.toFloat())
            .putFloat("baseline_flight", baselineFlightTime.toFloat())
            .putFloat("dwell_variance", dwellVariance.toFloat())
            .putFloat("flight_variance", flightVariance.toFloat())
            .putInt("keystroke_count", keystrokeCount)
            .apply()
    }

    /**
     * LOAD YOUR BASELINE FROM STORAGE
     * Restores your learned patterns when app starts
     */
    private fun loadBaseline() {
        baselineDwellTime = prefs.getFloat("baseline_dwell", 0f).toDouble()
        baselineFlightTime = prefs.getFloat("baseline_flight", 0f).toDouble()
        dwellVariance = prefs.getFloat("dwell_variance", 0f).toDouble()
        flightVariance = prefs.getFloat("flight_variance", 0f).toDouble()
        keystrokeCount = prefs.getInt("keystroke_count", 0)
    }

    /**
     * RESET BASELINE (for testing or new user)
     */
    fun resetBaseline() {
        baselineDwellTime = 0.0
        baselineFlightTime = 0.0
        dwellVariance = 0.0
        flightVariance = 0.0
        keystrokeCount = 0
        keystrokes.clear()
        prefs.edit().clear().apply()
    }

    /**
     * DEBUG INFORMATION
     * Shows current status for testing/debugging
     */
    fun getDebugInfo(): String {
        return """
            📊 KEYSTROKE DEBUG INFO
            Keystrokes Collected: $keystrokeCount
            Has Baseline: ${hasBaseline()}
            Baseline Dwell: ${String.format("%.1f", baselineDwellTime)}ms
            Baseline Flight: ${String.format("%.1f", baselineFlightTime)}ms
            Recent Keystrokes: ${keystrokes.size}
            Current WPM: ${String.format("%.1f", calculateWPM())}
        """.trimIndent()
    }
}