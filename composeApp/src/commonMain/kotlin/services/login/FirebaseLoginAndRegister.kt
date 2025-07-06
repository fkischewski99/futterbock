package services.login

import co.touchlab.kermit.Logger
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore

private const val USER_COLLECTION = "USER"

private const val STAMM = "group"

class FirebaseLoginAndRegister : LoginAndRegister {


    override suspend fun getCustomUserGroup(): String {
        if (!isAuthenticated()) {
            return ""
        }
        val userId = Firebase.auth.currentUser!!.uid
        val userDocRef = Firebase.firestore.collection(USER_COLLECTION).document(userId)
        val stamm = userDocRef.get().get<String>(STAMM)
        return stamm
    }

    override suspend fun login(email: String, password: String) {
        Firebase.auth.signInWithEmailAndPassword(
            email = email,
            password = password
        )
    }

    override suspend fun deleteCurrentUser() {
        val userId = Firebase.auth.currentUser!!.uid
        Firebase.firestore.collection(USER_COLLECTION).document(userId).delete()
        Firebase.auth.currentUser!!.delete()
    }

    override suspend fun register(email: String, password: String, group: String) {
        Firebase.auth.createUserWithEmailAndPassword(
            email = email,
            password = password
        )
        val userId = Firebase.auth.currentUser!!.uid
        val stammData = mapOf(STAMM to group)
        Firebase.firestore.collection(USER_COLLECTION).document(userId).set(stammData)
    }

    override fun isAuthenticated(): Boolean {
        return Firebase.auth.currentUser != null
    }

    override suspend fun logout() {
        Firebase.auth.signOut()
    }
}