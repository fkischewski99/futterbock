package data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import model.Event

@Dao
interface EventDao : BaseDao<Event> {
    @Query("SELECT * FROM events WHERE `group` = :group")
    fun getByGroup(group: String): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE uid = :uid")
    suspend fun getById(uid: String): Event?

    @Query("DELETE FROM events WHERE uid = :uid")
    suspend fun deleteById(uid: String)
}
