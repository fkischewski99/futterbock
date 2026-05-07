package model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class DailyShoppingList(
    val purchaseDate: LocalDate,
    val ingredients: List<ShoppingIngredient> = emptyList(),
    val totalItems: Int = ingredients.size
) {
    /**
     * Groups ingredients by category for display purposes
     */
    fun getIngredientsByCategory(): Map<String, List<ShoppingIngredient>> {
        return ingredients.groupBy { ingredient ->
            ingredient.ingredient?.category ?: "Sonstiges"
        }
    }
    
    /**
     * Checks if this shopping day has any items
     */
    fun hasItems(): Boolean = ingredients.isNotEmpty()
    
    /**
     * Gets the total count of completed shopping items
     */
    fun getCompletedItemsCount(): Int = ingredients.count { it.shoppingDone }
    
    /**
     * Checks if all items are completed
     */
    fun isCompleted(): Boolean = ingredients.isNotEmpty() && ingredients.all { it.shoppingDone }
}

@Serializable
@Entity(tableName = "multi_day_shopping_lists")
data class MultiDayShoppingList(
    @PrimaryKey val eventId: String,
    val dailyLists: Map<LocalDate, DailyShoppingList> = emptyMap()
) {
    /**
     * Gets all shopping days in chronological order
     */
    fun getShoppingDaysInOrder(): List<LocalDate> {
        return dailyLists.keys.sorted()
    }
    
    /**
     * Gets the next shopping day that has incomplete items
     */
    fun getNextShoppingDay(): LocalDate? {
        return getShoppingDaysInOrder().firstOrNull { date ->
            dailyLists[date]?.let { !it.isCompleted() } == true
        }
    }
    
    /**
     * Gets total number of items across all days
     */
    fun getTotalItemCount(): Int {
        return dailyLists.values.sumOf { it.totalItems }
    }
    
    /**
     * Gets total number of completed items across all days
     */
    fun getTotalCompletedCount(): Int {
        return dailyLists.values.sumOf { it.getCompletedItemsCount() }
    }
    
    /**
     * Converts to legacy single shopping list format for backward compatibility
     */
    fun toLegacyShoppingList(): List<ShoppingIngredient> {
        return dailyLists.values.flatMap { it.ingredients }
    }
    
    /**
     * Checks if any shopping day has items
     */
    fun hasAnyItems(): Boolean {
        return dailyLists.values.any { it.hasItems() }
    }
}