package com.example.adaptivesecurity.context.collectors


import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.adaptivesecurity.context.models.NetworkContext
import com.example.adaptivesecurity.context.models.NetworkType  // Added import
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class NetworkContextCollector(private val context: Context) {

    private val connectivityManager: ConnectivityManager? by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    fun collectNetworkContext(): NetworkContext? {
        connectivityManager ?: return null
        val activeNetwork = connectivityManager!!.activeNetwork ?: return null
        val capabilities = connectivityManager!!.getNetworkCapabilities(activeNetwork) ?: return null

        return NetworkContext(
            networkType = getNetworkType(capabilities),
            isSecureNetwork = isSecureNetwork(capabilities)
        )
    }

    fun monitorNetworkChanges(): Flow<NetworkContext> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(collectNetworkContext() ?: NetworkContext(NetworkType.NONE, false))
            }

            override fun onLost(network: Network) {
                trySend(NetworkContext(NetworkType.NONE, false))
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                trySend(
                    NetworkContext(
                        networkType = getNetworkType(capabilities),
                        isSecureNetwork = isSecureNetwork(capabilities)
                    )
                )
            }
        }

        connectivityManager?.registerNetworkCallback(
            NetworkRequest.Builder().build(),
            callback
        )

        // Initial state
        trySend(collectNetworkContext() ?: NetworkContext(NetworkType.NONE, false))

        awaitClose {
            connectivityManager?.unregisterNetworkCallback(callback)
        }
    }

    private fun getNetworkType(capabilities: NetworkCapabilities): NetworkType {
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_TEMPORARILY_NOT_METERED))
                    NetworkType.CELLULAR_5G
                else
                    NetworkType.CELLULAR_4G
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.OTHER
        }
    }

    private fun isSecureNetwork(capabilities: NetworkCapabilities): Boolean {
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
    }
}
