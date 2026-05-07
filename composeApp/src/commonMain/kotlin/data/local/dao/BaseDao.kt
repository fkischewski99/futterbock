package data.local.dao

import androidx.room.Delete
import androidx.room.Update
import androidx.room.Upsert

interface BaseDao<T> {
    @Upsert
    suspend fun insert(entity: T)

    @Upsert
    suspend fun insertAll(entities: List<T>)

    @Update
    suspend fun update(entity: T)

    @Delete
    suspend fun delete(entity: T)
}
