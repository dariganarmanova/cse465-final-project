package com.example.adaptivesecurity.context.listeners

import com.example.adaptivesecurity.context.models.ContextData
import com.example.adaptivesecurity.context.models.RiskLevel
import com.example.adaptivesecurity.context.models.KeyboardContext
interface ContextListener {
    fun onContextChanged(contextData: ContextData)
    fun onRiskLevelChanged(newRiskLevel: RiskLevel, previousRiskLevel: RiskLevel)
    fun onKeyboardAnomalyDetected(keyboardContext: KeyboardContext) {
        // Default implementation - can be overridden by implementing classes
    }
}