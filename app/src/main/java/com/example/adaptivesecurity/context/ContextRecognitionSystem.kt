package com.example.adaptivesecurity.context

import com.example.adaptivesecurity.context.listeners.ContextListener
import com.example.adaptivesecurity.context.models.ContextData

interface ContextRecognitionSystem {
    suspend fun getCurrentContext(): ContextData
    suspend fun startContextMonitoring()
    suspend fun stopContextMonitoring()
    fun registerContextListener(listener: ContextListener)
    fun unregisterContextListener(listener: ContextListener)
}