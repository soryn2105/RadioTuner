package com.sorin.radiotuner.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * (RO)
 * Expune starea conexiunii la internet ca Flow<Boolean>, folosind API-ul
 * nativ ConnectivityManager.NetworkCallback
 * — NU polling manual (adică nu verificăm "din timp în timp dacă mai avem net",
 * ci Android ne anunță instant când se schimbă starea).
 * (EN)
 * Exposes the internet connection state as Flow<Boolean>, using the native
 * ConnectivityManager.NetworkCallback API — NOT manual polling
 * (i.e. we do not check "from time to time if we still have internet",
 * but Android notifies us instantly when the state changes).
 */
class ConnectivityObserver(context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun observe(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                trySend(hasInternet)
            }
        }

        val request = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(request, callback)

        // Trimitem starea inițială imediat, ca UI-ul să nu pornească "orb".
        trySend(isCurrentlyOnline())

        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun isCurrentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}