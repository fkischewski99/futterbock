package data.local.dao

import androidx.room.Dao
import androidx.room.Query
import model.Ingredient

@Dao
interface IngredientDao : BaseDao<Ingredient> {
    @Query("SELECT * FROM ingredients")
    suspend fun getAll(): List<Ingredient>

    @Query("SELECT * FROM ingredients WHERE uid = :uid")
    suspend fun getById(uid: String): Ingredient?

    @Query("SELECT * FROM ingredients WHERE uid IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<Ingredient>
}
