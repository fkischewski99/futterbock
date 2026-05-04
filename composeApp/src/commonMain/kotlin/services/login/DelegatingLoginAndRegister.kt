package services.login

import data.AppMode
import data.AppModeHolder

class DelegatingLoginAndRegister(
    private val appModeHolder: AppModeHolder,
    private val firebase: FirebaseLoginAndRegister,
    private val offline: OfflineLoginAndRegister
) : LoginAndRegister {

    private val active: LoginAndRegister
        get() = when (appModeHolder.mode.value) {
            AppMode.OFFLINE_ONLY -> offline
            else -> firebase
        }

    override suspend fun login(email: String, password: String) = active.login(email, password)
    override suspend fun register(email: String, password: String, group: String) = active.register(email, password, group)
    override fun isAuthenticated(): Boolean = active.isAuthenticated()
    override suspend fun logout() = active.logout()
    override suspend fun getCustomUserGroup(): String = active.getCustomUserGroup()
    override suspend fun deleteCurrentUser() = active.deleteCurrentUser()
}
