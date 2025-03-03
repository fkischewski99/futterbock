package services.login

interface LoginAndRegister {
    suspend fun login(email: String, password: String)
    suspend fun register(email: String, password: String, group: String)
    fun isAuthenticated(): Boolean
    suspend fun logout()
    suspend fun getCustomUserGroup(): String
    suspend fun deleteCurrentUser()
}