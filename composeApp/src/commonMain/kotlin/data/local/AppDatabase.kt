package data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import data.local.dao.*
import data.sync.PendingOperation
import model.*

@Database(
    entities = [
        Event::class,
        Participant::class,
        Recipe::class,
        Ingredient::class,
        Meal::class,
        ParticipantTime::class,
        Material::class,
        MultiDayShoppingList::class,
        PendingOperation::class,
    ],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun participantDao(): ParticipantDao
    abstract fun recipeDao(): RecipeDao
    abstract fun ingredientDao(): IngredientDao
    abstract fun mealDao(): MealDao
    abstract fun participantTimeDao(): ParticipantTimeDao
    abstract fun materialDao(): MaterialDao
    abstract fun multiDayShoppingListDao(): MultiDayShoppingListDao
    abstract fun pendingOperationDao(): PendingOperationDao
}
