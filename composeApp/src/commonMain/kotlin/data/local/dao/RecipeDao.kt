package data.local.dao

import androidx.room.Dao
import androidx.room.Query
import model.Recipe

@Dao
interface RecipeDao : BaseDao<Recipe> {
    @Query("SELECT * FROM recipes")
    suspend fun getAll(): List<Recipe>

    @Query("SELECT * FROM recipes WHERE source = :source")
    suspend fun getBySource(source: String): List<Recipe>

    @Query("SELECT * FROM recipes WHERE uid = :uid")
    suspend fun getById(uid: String): Recipe?

    @Query("DELETE FROM recipes WHERE uid = :uid")
    suspend fun deleteById(uid: String)
}
