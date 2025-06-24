package com.example.adaptivesecurity.network.listeners

import com.example.adaptivesecurity.network.models.SecurityStatus
import com.example.adaptivesecurity.network.models.WifiNetwork

interface SecurityListener {
    fun onSecurityAnalysis(status: SecurityStatus)
    fun onNetworkScanComplete(networks: List<WifiNetwork>)
}