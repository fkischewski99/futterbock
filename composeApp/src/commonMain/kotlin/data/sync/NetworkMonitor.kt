package data.sync

import kotlinx.coroutines.flow.StateFlow

interface NetworkMonitor {
    val isOnline: StateFlow<Boolean>
}

expect class NetworkMonitorImpl() : NetworkMonitor
