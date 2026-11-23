package com.smartexpense.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.smartexpense.domain.network.NetworkStatusProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Device-level connectivity, both one-shot and observed for the app shell. */
class AndroidNetworkStatusProvider(
    context: Context
) : NetworkStatusProvider {

    private val appContext = context.applicationContext
    private val manager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _connected = MutableStateFlow(hasInternet())
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    /** Starts observing connectivity for the lifetime of the process. */
    fun start() {
        if (networkCallback != null) return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = refresh()
            override fun onLost(network: Network) = refresh()
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) = refresh()
        }
        manager.registerDefaultNetworkCallback(callback)
        networkCallback = callback
    }

    private fun refresh() {
        _connected.value = hasInternet()
    }

    override fun isConnected(): Boolean = hasInternet()

    private fun hasInternet(): Boolean {
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
