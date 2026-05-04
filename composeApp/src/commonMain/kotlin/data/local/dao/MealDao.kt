package data.local.dao

import androidx.room.Dao
import androidx.room.Query
import model.Meal

@Dao
interface MealDao : BaseDao<Meal> {
    @Query("SELECT * FROM meals WHERE eventId = :eventId")
    suspend fun getByEventId(eventId: String): List<Meal>

    @Query("SELECT * FROM meals WHERE uid = :uid AND eventId = :eventId")
    suspend fun getById(eventId: String, uid: String): Meal?

    @Query("DELETE FROM meals WHERE uid = :uid AND eventId = :eventId")
    suspend fun deleteById(eventId: String, uid: String)
}
