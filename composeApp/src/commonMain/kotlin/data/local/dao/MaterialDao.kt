package data.local.dao

import androidx.room.Dao
import androidx.room.Query
import model.Material

@Dao
interface MaterialDao : BaseDao<Material> {
    @Query("SELECT * FROM materials")
    suspend fun getAll(): List<Material>

    @Query("SELECT * FROM materials WHERE eventId = :eventId")
    suspend fun getByEventId(eventId: String): List<Material>

    @Query("DELETE FROM materials WHERE uid = :uid")
    suspend fun deleteById(uid: String)
}
