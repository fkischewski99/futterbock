package data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import data.sync.PendingOperation

@Dao
interface PendingOperationDao {
    @Insert
    suspend fun insert(operation: PendingOperation)

    @Query("SELECT * FROM pending_operations ORDER BY timestamp ASC")
    suspend fun getAllOrdered(): List<PendingOperation>

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_operations")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM pending_operations")
    suspend fun count(): Int
}
