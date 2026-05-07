package services.login

class OfflineLoginAndRegister : LoginAndRegister {

    override suspend fun login(email: String, password: String) {
        // No-op
    }

    override suspend fun register(email: String, password: String, group: String) {
        // No-op
    }

    override fun isAuthenticated(): Boolean = true

    override suspend fun logout() {
        // No-op
    }

    override suspend fun getCustomUserGroup(): String = "offline_local"

    override suspend fun deleteCurrentUser() {
        // No-op
    }
}
