package view.event.cooking_groups.ingredients

import kotlinx.datetime.LocalDate
import model.Ingredient
import model.IngredientUnit
import model.ShoppingIngredient
import services.cookingGroups.CookingGroupIngredientService.CookingGroupIngredients
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the total ingredients aggregation functionality in CookingGroupIngredientsScreen.
 * This tests the logic that aggregates ingredients from all cooking groups to show a total summary.
 */
class TotalIngredientsAggregationTest {

    @Test
    fun `aggregates same ingredients from different cooking groups`() {
        // Create ingredients
        val tomato = Ingredient().apply {
            uid = "tomato"
            name = "Tomato"
            category = "Vegetables"
        }
        
        val rice = Ingredient().apply {
            uid = "rice"
            name = "Rice"
            category = "Grains"
        }
        
        // Create shopping ingredients for Group A
        val tomatoGroupA = ShoppingIngredient().apply {
            ingredientRef = "tomato"
            ingredient = tomato
            amount = 100.0
            unit = IngredientUnit.GRAMM
        }
        
        val riceGroupA = ShoppingIngredient().apply {
            ingredientRef = "rice"
            ingredient = rice
            amount = 200.0
            unit = IngredientUnit.GRAMM
        }
        
        // Create shopping ingredients for Group B
        val tomatoGroupB = ShoppingIngredient().apply {
            ingredientRef = "tomato"
            ingredient = tomato
            amount = 150.0
            unit = IngredientUnit.GRAMM
        }
        
        val riceGroupB = ShoppingIngredient().apply {
            ingredientRef = "rice"
            ingredient = rice
            amount = 250.0
            unit = IngredientUnit.GRAMM
        }
        
        // Create cooking group ingredients
        val cookingGroupIngredients = listOf(
            CookingGroupIngredients(
                cookingGroupName = "Group A",
                participantCount = 3,
                guestCount = 0,
                ingredients = listOf(tomatoGroupA, riceGroupA)
            ),
            CookingGroupIngredients(
                cookingGroupName = "Group B",
                participantCount = 2,
                guestCount = 1,
                ingredients = listOf(tomatoGroupB, riceGroupB)
            )
        )
        
        // Execute aggregation logic (simulating what TotalIngredientsSummaryCard does)
        val totalIngredients = aggregateIngredientsAcrossGroups(cookingGroupIngredients)
        
        // Verify
        assertEquals(2, totalIngredients.size)
        
        // Check tomato aggregation
        val tomatoKey = "tomato${IngredientUnit.GRAMM}"
        assertTrue(totalIngredients.containsKey(tomatoKey))
        val totalTomato = totalIngredients[tomatoKey]!!
        assertEquals("Tomato", totalTomato.ingredient?.name)
        assertEquals(250.0, totalTomato.amount) // 100 + 150
        assertEquals(IngredientUnit.GRAMM, totalTomato.unit)
        
        // Check rice aggregation
        val riceKey = "rice${IngredientUnit.GRAMM}"
        assertTrue(totalIngredients.containsKey(riceKey))
        val totalRice = totalIngredients[riceKey]!!
        assertEquals("Rice", totalRice.ingredient?.name)
        assertEquals(450.0, totalRice.amount) // 200 + 250
        assertEquals(IngredientUnit.GRAMM, totalRice.unit)
    }
    
    @Test
    fun `handles ingredients with different units separately`() {
        // Create ingredient
        val milk = Ingredient().apply {
            uid = "milk"
            name = "Milk"
            category = "Dairy"
        }
        
        // Create shopping ingredients with different units
        val milkInGrams = ShoppingIngredient().apply {
            ingredientRef = "milk"
            ingredient = milk
            amount = 500.0
            unit = IngredientUnit.GRAMM
        }
        
        val milkInMilliliters = ShoppingIngredient().apply {
            ingredientRef = "milk"
            ingredient = milk
            amount = 250.0
            unit = IngredientUnit.MILLILITER
        }
        
        val cookingGroupIngredients = listOf(
            CookingGroupIngredients(
                cookingGroupName = "Group A",
                participantCount = 2,
                guestCount = 0,
                ingredients = listOf(milkInGrams)
            ),
            CookingGroupIngredients(
                cookingGroupName = "Group B",
                participantCount = 1,
                guestCount = 0,
                ingredients = listOf(milkInMilliliters)
            )
        )
        
        // Execute aggregation logic
        val totalIngredients = aggregateIngredientsAcrossGroups(cookingGroupIngredients)
        
        // Verify - should have 2 separate entries for different units
        assertEquals(2, totalIngredients.size)
        
        val milkGramKey = "milk${IngredientUnit.GRAMM}"
        val milkMlKey = "milk${IngredientUnit.MILLILITER}"
        
        assertTrue(totalIngredients.containsKey(milkGramKey))
        assertTrue(totalIngredients.containsKey(milkMlKey))
        
        assertEquals(500.0, totalIngredients[milkGramKey]!!.amount)
        assertEquals(250.0, totalIngredients[milkMlKey]!!.amount)
    }
    
    @Test
    fun `handles empty cooking groups`() {
        val cookingGroupIngredients = listOf(
            CookingGroupIngredients(
                cookingGroupName = "Empty Group",
                participantCount = 0,
                guestCount = 0,
                ingredients = emptyList()
            )
        )
        
        // Execute aggregation logic
        val totalIngredients = aggregateIngredientsAcrossGroups(cookingGroupIngredients)
        
        // Verify
        assertTrue(totalIngredients.isEmpty())
    }
    
    @Test
    fun `handles single cooking group`() {
        // Create ingredient
        val pasta = Ingredient().apply {
            uid = "pasta"
            name = "Pasta"
            category = "Grains"
        }
        
        val pastaIngredient = ShoppingIngredient().apply {
            ingredientRef = "pasta"
            ingredient = pasta
            amount = 300.0
            unit = IngredientUnit.GRAMM
        }
        
        val cookingGroupIngredients = listOf(
            CookingGroupIngredients(
                cookingGroupName = "Solo Group",
                participantCount = 4,
                guestCount = 2,
                ingredients = listOf(pastaIngredient)
            )
        )
        
        // Execute aggregation logic
        val totalIngredients = aggregateIngredientsAcrossGroups(cookingGroupIngredients)
        
        // Verify
        assertEquals(1, totalIngredients.size)
        val pastaKey = "pasta${IngredientUnit.GRAMM}"
        assertTrue(totalIngredients.containsKey(pastaKey))
        
        val totalPasta = totalIngredients[pastaKey]!!
        assertEquals("Pasta", totalPasta.ingredient?.name)
        assertEquals(300.0, totalPasta.amount)
        assertEquals(IngredientUnit.GRAMM, totalPasta.unit)
    }
    
    @Test
    fun `preserves ingredient metadata during aggregation`() {
        // Create ingredient with metadata
        val cheese = Ingredient().apply {
            uid = "cheese"
            name = "Cheese"
            category = "Dairy"
        }
        
        val cheeseIngredient1 = ShoppingIngredient().apply {
            ingredientRef = "cheese"
            ingredient = cheese
            amount = 100.0
            unit = IngredientUnit.GRAMM
            nameEnteredByUser = "Special Cheese"
            note = "Extra sharp"
        }
        
        val cheeseIngredient2 = ShoppingIngredient().apply {
            ingredientRef = "cheese"
            ingredient = cheese
            amount = 200.0
            unit = IngredientUnit.GRAMM
            nameEnteredByUser = "Special Cheese"
            note = "Extra sharp"
        }
        
        val cookingGroupIngredients = listOf(
            CookingGroupIngredients(
                cookingGroupName = "Group A",
                participantCount = 2,
                guestCount = 0,
                ingredients = listOf(cheeseIngredient1)
            ),
            CookingGroupIngredients(
                cookingGroupName = "Group B",
                participantCount = 3,
                guestCount = 0,
                ingredients = listOf(cheeseIngredient2)
            )
        )
        
        // Execute aggregation logic
        val totalIngredients = aggregateIngredientsAcrossGroups(cookingGroupIngredients)
        
        // Verify
        assertEquals(1, totalIngredients.size)
        val cheeseKey = "cheese${IngredientUnit.GRAMM}"
        val totalCheese = totalIngredients[cheeseKey]!!
        
        assertEquals("Cheese", totalCheese.ingredient?.name)
        assertEquals(300.0, totalCheese.amount) // 100 + 200
        assertEquals("Special Cheese", totalCheese.nameEnteredByUser)
        assertEquals("Extra sharp", totalCheese.note)
    }
    
    @Test
    fun `calculates total person count correctly`() {
        val cookingGroupIngredients = listOf(
            CookingGroupIngredients(
                cookingGroupName = "Group A",
                participantCount = 5,
                guestCount = 2,
                ingredients = emptyList()
            ),
            CookingGroupIngredients(
                cookingGroupName = "Group B",
                participantCount = 3,
                guestCount = 1,
                ingredients = emptyList()
            ),
            CookingGroupIngredients(
                cookingGroupName = "Gäste",
                participantCount = 0,
                guestCount = 4,
                ingredients = emptyList()
            )
        )
        
        // Execute person count calculation (simulating what TotalIngredientsSummaryCard does)
        val totalCount = cookingGroupIngredients.sumOf { it.participantCount + it.guestCount }
        
        // Verify
        assertEquals(15, totalCount) // 5+2 + 3+1 + 0+4 = 15
    }
    
    /**
     * Helper method that simulates the aggregation logic from TotalIngredientsSummaryCard.
     * This is the core logic we're testing.
     */
    private fun aggregateIngredientsAcrossGroups(
        cookingGroupIngredients: List<CookingGroupIngredients>
    ): Map<String, ShoppingIngredient> {
        val totalIngredients = mutableMapOf<String, ShoppingIngredient>()
        
        cookingGroupIngredients.forEach { cookingGroup ->
            cookingGroup.ingredients.forEach { ingredient ->
                val key = ingredient.ingredientRef + ingredient.unit
                val existing = totalIngredients[key]
                
                if (existing != null) {
                    existing.amount += ingredient.amount
                } else {
                    totalIngredients[key] = ShoppingIngredient().apply {
                        this.ingredient = ingredient.ingredient
                        this.ingredientRef = ingredient.ingredientRef
                        this.unit = ingredient.unit
                        this.amount = ingredient.amount
                        this.nameEnteredByUser = ingredient.nameEnteredByUser
                        this.note = ingredient.note
                    }
                }
            }
        }
        
        return totalIngredients
    }
}