package com.example.adaptivesecurity.context.listeners

import com.example.adaptivesecurity.context.models.ContextData
import com.example.adaptivesecurity.context.models.RiskLevel
interface ContextListener {
    fun onContextChanged(contextData: ContextData)
    fun onRiskLevelChanged(newRiskLevel: RiskLevel, previousRiskLevel: RiskLevel)
}