package data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import model.Participant

@Dao
interface ParticipantDao : BaseDao<Participant> {
    @Query("SELECT * FROM participants WHERE `group` = :group")
    fun getByGroup(group: String): Flow<List<Participant>>

    @Query("SELECT * FROM participants WHERE uid = :uid")
    suspend fun getById(uid: String): Participant?

    @Query("SELECT * FROM participants WHERE firstName = :firstName AND lastName = :lastName LIMIT 1")
    suspend fun findByName(firstName: String, lastName: String): Participant?

    @Query("DELETE FROM participants WHERE uid = :uid")
    suspend fun deleteById(uid: String)
}
