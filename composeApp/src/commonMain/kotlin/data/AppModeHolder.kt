package data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppModeHolder(initialMode: AppMode) {
    private val _mode = MutableStateFlow(initialMode)
    val mode: StateFlow<AppMode> = _mode.asStateFlow()

    fun switchMode(newMode: AppMode) {
        val prefs = AppModePreferences()
        prefs.setAppMode(newMode)
        _mode.value = newMode
    }
}
