package data.local.dao

import androidx.room.Dao
import androidx.room.Query
import model.MultiDayShoppingList

@Dao
interface MultiDayShoppingListDao : BaseDao<MultiDayShoppingList> {
    @Query("SELECT * FROM multi_day_shopping_lists WHERE eventId = :eventId")
    suspend fun getByEventId(eventId: String): MultiDayShoppingList?

    @Query("DELETE FROM multi_day_shopping_lists WHERE eventId = :eventId")
    suspend fun deleteByEventId(eventId: String)
}
