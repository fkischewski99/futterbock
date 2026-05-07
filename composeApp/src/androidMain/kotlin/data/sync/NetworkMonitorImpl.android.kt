package data.sync

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.futterbock.app.MainActivity

actual class NetworkMonitorImpl actual constructor() : NetworkMonitor {
    private val _isOnline = MutableStateFlow(checkInitialState())
    override val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val connectivityManager by lazy {
        MainActivity.appContext.getSystemService(ConnectivityManager::class.java)
    }

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOnline.value = true
            }

            override fun onLost(network: Network) {
                _isOnline.value = false
            }
        })
    }

    private fun checkInitialState(): Boolean {
        val cm = MainActivity.appContext.getSystemService(ConnectivityManager::class.java)
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
