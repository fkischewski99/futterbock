package services.cookingGroups

import data.EventRepository
import data.FakeEventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import model.Ingredient
import model.IngredientUnit
import model.Meal
import model.ParticipantTime
import model.Recipe
import model.RecipeSelection
import model.ShoppingIngredient
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import services.shoppingList.CalculateShoppingList
import view.shared.HelperFunctions
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CookingGroupIngredientServiceTest : KoinTest {

    private lateinit var fakeRepo: FakeEventRepository
    private lateinit var cookingGroupService: CookingGroupIngredientService
    private lateinit var calculateShoppingList: CalculateShoppingList

    private val testModule = module {
        single<EventRepository> { FakeEventRepository().also { fakeRepo = it } }
        single { CalculateShoppingList(get()) }
        single { CookingGroupIngredientService(get()) }
    }

    @BeforeTest
    fun setup() {
        stopKoin()
        startKoin { modules(testModule) }

        calculateShoppingList = get()
        cookingGroupService = get()

        Dispatchers.setMain(StandardTestDispatcher())
    }

    @Test
    fun `calculates ingredients for single cooking group`() = runTest {
        val eventId = "event1"
        val testDate = LocalDate(2024, 1, 15)
        
        // Create ingredient
        val tomato = Ingredient().apply {
            uid = "tomato"
            name = "Tomato"
            category = "Vegetables"
        }
        
        // Create shopping ingredient
        val shoppingIngredient = ShoppingIngredient().apply {
            ingredientRef = "tomato"
            amount = 100.0
            unit = IngredientUnit.GRAMM
            ingredient = tomato
        }
        
        // Create recipe
        val recipe = Recipe().apply {
            uid = "recipe1"
            shoppingIngredients = listOf(shoppingIngredient)
        }
        
        // Create recipe selection with participants from one cooking group
        val recipeSelection = RecipeSelection().apply {
            this.recipe = recipe
            eaterIds = mutableSetOf("p1", "p2")
        }
        
        // Create meal
        val meal = Meal(day = HelperFunctions.getInstant(testDate)).apply {
            recipeSelections = listOf(recipeSelection)
        }
        
        // Create participants with same cooking group
        val participants = listOf(
            createParticipantTime("p1", "Group A"),
            createParticipantTime("p2", "Group A")
        )
        
        // Setup fake repo
        fakeRepo.mealsForEvent = mutableListOf(meal)
        fakeRepo.participants = mapOf(
            "p1" to participants[0].participant!!,
            "p2" to participants[1].participant!!
        )
        fakeRepo.participantTimes = participants
        
        // Execute
        val result = cookingGroupService.calculateIngredientsPerCookingGroup(
            eventId = eventId,
            date = testDate,
            meals = listOf(meal),
            participants = participants
        )
        
        // Verify
        assertEquals(1, result.size)
        assertTrue(result.containsKey("Group A"))
        val groupAIngredients = result["Group A"]!!
        assertEquals(1, groupAIngredients.size)
        assertEquals("Tomato", groupAIngredients[0].ingredient?.name)
        assertEquals(200.0, groupAIngredients[0].amount) // 100g * 2 participants
    }

    @Test
    fun `calculates ingredients for multiple cooking groups`() = runTest {
        val eventId = "event2"
        val testDate = LocalDate(2024, 1, 15)
        
        // Create ingredients
        val rice = Ingredient().apply {
            uid = "rice"
            name = "Rice"
            category = "Grains"
        }
        
        val beans = Ingredient().apply {
            uid = "beans"
            name = "Beans"
            category = "Legumes"
        }
        
        // Create shopping ingredients
        val riceIngredient = ShoppingIngredient().apply {
            ingredientRef = "rice"
            amount = 50.0
            unit = IngredientUnit.GRAMM
            ingredient = rice
        }
        
        val beansIngredient = ShoppingIngredient().apply {
            ingredientRef = "beans"
            amount = 30.0
            unit = IngredientUnit.GRAMM
            ingredient = beans
        }
        
        // Create recipes
        val riceRecipe = Recipe().apply {
            uid = "rice-recipe"
            shoppingIngredients = listOf(riceIngredient)
        }
        
        val beansRecipe = Recipe().apply {
            uid = "beans-recipe"
            shoppingIngredients = listOf(beansIngredient)
        }
        
        // Create recipe selections for different cooking groups
        val riceSelection = RecipeSelection().apply {
            this.recipe = riceRecipe
            eaterIds = mutableSetOf("p1", "p2") // Group A
        }
        
        val beansSelection = RecipeSelection().apply {
            this.recipe = beansRecipe
            eaterIds = mutableSetOf("p3", "p4") // Group B
        }
        
        // Create meal
        val meal = Meal(day = HelperFunctions.getInstant(testDate)).apply {
            recipeSelections = listOf(riceSelection, beansSelection)
        }
        
        // Create participants with different cooking groups
        val participants = listOf(
            createParticipantTime("p1", "Group A"),
            createParticipantTime("p2", "Group A"),
            createParticipantTime("p3", "Group B"),
            createParticipantTime("p4", "Group B")
        )
        
        // Setup fake repo
        fakeRepo.mealsForEvent = mutableListOf(meal)
        fakeRepo.participants = participants.associate { it.participantRef to it.participant!! }
        
        // Execute
        val result = cookingGroupService.calculateIngredientsPerCookingGroup(
            eventId = eventId,
            date = testDate,
            meals = listOf(meal),
            participants = participants
        )
        
        // Verify
        assertEquals(2, result.size)
        
        // Group A should have rice
        assertTrue(result.containsKey("Group A"))
        val groupAIngredients = result["Group A"]!!
        assertEquals(1, groupAIngredients.size)
        assertEquals("Rice", groupAIngredients[0].ingredient?.name)
        assertEquals(100.0, groupAIngredients[0].amount) // 50g * 2 participants
        
        // Group B should have beans
        assertTrue(result.containsKey("Group B"))
        val groupBIngredients = result["Group B"]!!
        assertEquals(1, groupBIngredients.size)
        assertEquals("Beans", groupBIngredients[0].ingredient?.name)
        assertEquals(60.0, groupBIngredients[0].amount) // 30g * 2 participants
    }

    @Test
    fun `handles participants without cooking group assignment`() = runTest {
        val eventId = "event3"
        val testDate = LocalDate(2024, 1, 15)
        
        // Create ingredient
        val pasta = Ingredient().apply {
            uid = "pasta"
            name = "Pasta"
            category = "Grains"
        }
        
        val pastaIngredient = ShoppingIngredient().apply {
            ingredientRef = "pasta"
            amount = 75.0
            unit = IngredientUnit.GRAMM
            ingredient = pasta
        }
        
        val recipe = Recipe().apply {
            uid = "pasta-recipe"
            shoppingIngredients = listOf(pastaIngredient)
        }
        
        val recipeSelection = RecipeSelection().apply {
            this.recipe = recipe
            eaterIds = mutableSetOf("p1", "p2")
        }
        
        val meal = Meal(day = HelperFunctions.getInstant(testDate)).apply {
            recipeSelections = listOf(recipeSelection)
        }
        
        // Create participants without cooking group (should go to "Andere")
        val participants = listOf(
            createParticipantTime("p1", ""), // No cooking group
            createParticipantTime("p2", "")  // No cooking group
        )
        
        // Setup fake repo
        fakeRepo.mealsForEvent = mutableListOf(meal)
        fakeRepo.participants = participants.associate { it.participantRef to it.participant!! }
        
        // Execute
        val result = cookingGroupService.calculateIngredientsPerCookingGroup(
            eventId = eventId,
            date = testDate,
            meals = listOf(meal),
            participants = participants
        )
        
        // Verify
        assertEquals(1, result.size)
        assertTrue(result.containsKey("Andere"))
        val andereIngredients = result["Andere"]!!
        assertEquals(1, andereIngredients.size)
        assertEquals("Pasta", andereIngredients[0].ingredient?.name)
        assertEquals(150.0, andereIngredients[0].amount) // 75g * 2 participants
    }

    @Test
    fun `calculates ingredients for guests separately`() = runTest {
        val eventId = "event4"
        val testDate = LocalDate(2024, 1, 15)
        
        // Create ingredient
        val bread = Ingredient().apply {
            uid = "bread"
            name = "Bread"
            category = "Grains"
        }
        
        val breadIngredient = ShoppingIngredient().apply {
            ingredientRef = "bread"
            amount = 25.0
            unit = IngredientUnit.GRAMM
            ingredient = bread
        }
        
        val recipe = Recipe().apply {
            uid = "bread-recipe"
            shoppingIngredients = listOf(breadIngredient)
        }
        
        val recipeSelection = RecipeSelection().apply {
            this.recipe = recipe
            eaterIds = mutableSetOf("p1") // 1 participant
            guestCount = 3 // 3 guests
        }
        
        val meal = Meal(day = HelperFunctions.getInstant(testDate)).apply {
            recipeSelections = listOf(recipeSelection)
        }
        
        val participants = listOf(
            createParticipantTime("p1", "Group A")
        )
        
        // Setup fake repo
        fakeRepo.mealsForEvent = mutableListOf(meal)
        fakeRepo.participants = participants.associate { it.participantRef to it.participant!! }
        
        // Execute
        val result = cookingGroupService.calculateIngredientsPerCookingGroup(
            eventId = eventId,
            date = testDate,
            meals = listOf(meal),
            participants = participants
        )
        
        // Verify
        assertEquals(2, result.size)
        
        // Group A should have bread for 1 participant
        assertTrue(result.containsKey("Group A"))
        val groupAIngredients = result["Group A"]!!
        assertEquals(1, groupAIngredients.size)
        assertEquals("Bread", groupAIngredients[0].ingredient?.name)
        assertEquals(25.0, groupAIngredients[0].amount) // 25g * 1 participant
        
        // Guests should have bread for 3 guests
        assertTrue(result.containsKey("Gäste"))
        val guestIngredients = result["Gäste"]!!
        assertEquals(1, guestIngredients.size)
        assertEquals("Bread", guestIngredients[0].ingredient?.name)
        assertEquals(75.0, guestIngredients[0].amount) // 25g * 3 guests
    }

    @Test
    fun `getCookingGroupIngredientsForDay returns structured data`() = runTest {
        val eventId = "event5"
        val testDate = LocalDate(2024, 1, 15)
        
        // Create ingredient
        val cheese = Ingredient().apply {
            uid = "cheese"
            name = "Cheese"
            category = "Dairy"
        }
        
        val cheeseIngredient = ShoppingIngredient().apply {
            ingredientRef = "cheese"
            amount = 40.0
            unit = IngredientUnit.GRAMM
            ingredient = cheese
        }
        
        val recipe = Recipe().apply {
            uid = "cheese-recipe"
            shoppingIngredients = listOf(cheeseIngredient)
        }
        
        val recipeSelection = RecipeSelection().apply {
            this.recipe = recipe
            eaterIds = mutableSetOf("p1", "p2")
            guestCount = 2
        }
        
        val meal = Meal(day = HelperFunctions.getInstant(testDate)).apply {
            recipeSelections = listOf(recipeSelection)
        }
        
        val participants = listOf(
            createParticipantTime("p1", "Group A"),
            createParticipantTime("p2", "Group A")
        )
        
        // Setup fake repo
        fakeRepo.mealsForEvent = mutableListOf(meal)
        fakeRepo.participants = participants.associate { it.participantRef to it.participant!! }
        
        // Execute
        val result = cookingGroupService.getCookingGroupIngredientsForDay(
            eventId = eventId,
            date = testDate,
            meals = listOf(meal),
            participants = participants
        )
        
        // Verify
        assertEquals(2, result.size)
        
        // Find Group A
        val groupA = result.find { it.cookingGroupName == "Group A" }!!
        assertEquals(2, groupA.participantCount)
        assertEquals(0, groupA.guestCount)
        assertEquals(1, groupA.ingredients.size)
        assertEquals("Cheese", groupA.ingredients[0].ingredient?.name)
        assertEquals(80.0, groupA.ingredients[0].amount) // 40g * 2 participants
        
        // Find Guests
        val guests = result.find { it.cookingGroupName == "Gäste" }!!
        assertEquals(0, guests.participantCount)
        assertEquals(2, guests.guestCount) // Total guest count from all meals
        assertEquals(1, guests.ingredients.size)
        assertEquals("Cheese", guests.ingredients[0].ingredient?.name)
        assertEquals(80.0, guests.ingredients[0].amount) // 40g * 2 guests
    }

    @Test
    fun `handles empty meals list`() = runTest {
        val eventId = "event6"
        val testDate = LocalDate(2024, 1, 15)
        
        val participants = listOf(
            createParticipantTime("p1", "Group A")
        )
        
        // Execute with empty meals
        val result = cookingGroupService.calculateIngredientsPerCookingGroup(
            eventId = eventId,
            date = testDate,
            meals = emptyList(),
            participants = participants
        )
        
        // Verify
        assertEquals(1, result.size)
        assertTrue(result.containsKey("Group A"))
        val groupAIngredients = result["Group A"]!!
        assertTrue(groupAIngredients.isEmpty())
    }

    @Test
    fun `handles empty participants list`() = runTest {
        val eventId = "event7"
        val testDate = LocalDate(2024, 1, 15)
        
        // Create meal with guest count only
        val ingredient = Ingredient().apply {
            uid = "water"
            name = "Water"
        }
        
        val waterIngredient = ShoppingIngredient().apply {
            ingredientRef = "water"
            amount = 500.0
            unit = IngredientUnit.MILLILITER
            this.ingredient = ingredient
        }
        
        val recipe = Recipe().apply {
            uid = "water-recipe"
            shoppingIngredients = listOf(waterIngredient)
        }
        
        val recipeSelection = RecipeSelection().apply {
            this.recipe = recipe
            eaterIds = mutableSetOf() // No participants
            guestCount = 4 // Only guests
        }
        
        val meal = Meal(day = HelperFunctions.getInstant(testDate)).apply {
            recipeSelections = listOf(recipeSelection)
        }
        
        // Execute with empty participants
        val result = cookingGroupService.calculateIngredientsPerCookingGroup(
            eventId = eventId,
            date = testDate,
            meals = listOf(meal),
            participants = emptyList()
        )
        
        // Verify
        assertEquals(1, result.size)
        assertTrue(result.containsKey("Gäste"))
        val guestIngredients = result["Gäste"]!!
        assertEquals(1, guestIngredients.size)
        assertEquals("Water", guestIngredients[0].ingredient?.name)
        assertEquals(2000.0, guestIngredients[0].amount) // 500ml * 4 guests
    }

    @Test
    fun `aggregates same ingredients from different recipes within same cooking group`() = runTest {
        val eventId = "event8"
        val testDate = LocalDate(2024, 1, 15)
        
        // Create ingredient that appears in multiple recipes
        val salt = Ingredient().apply {
            uid = "salt"
            name = "Salt"
            category = "Spices"
        }
        
        // Create two different recipes both using salt
        val saltIngredient1 = ShoppingIngredient().apply {
            ingredientRef = "salt"
            amount = 5.0
            unit = IngredientUnit.GRAMM
            ingredient = salt
        }
        
        val saltIngredient2 = ShoppingIngredient().apply {
            ingredientRef = "salt"
            amount = 3.0
            unit = IngredientUnit.GRAMM
            ingredient = salt
        }
        
        val recipe1 = Recipe().apply {
            uid = "recipe1"
            shoppingIngredients = listOf(saltIngredient1)
        }
        
        val recipe2 = Recipe().apply {
            uid = "recipe2"
            shoppingIngredients = listOf(saltIngredient2)
        }
        
        val recipeSelection1 = RecipeSelection().apply {
            this.recipe = recipe1
            eaterIds = mutableSetOf("p1", "p2") // Group A
        }
        
        val recipeSelection2 = RecipeSelection().apply {
            this.recipe = recipe2
            eaterIds = mutableSetOf("p1", "p2") // Same Group A
        }
        
        val meal = Meal(day = HelperFunctions.getInstant(testDate)).apply {
            recipeSelections = listOf(recipeSelection1, recipeSelection2)
        }
        
        val participants = listOf(
            createParticipantTime("p1", "Group A"),
            createParticipantTime("p2", "Group A")
        )
        
        // Setup fake repo
        fakeRepo.mealsForEvent = mutableListOf(meal)
        fakeRepo.participants = participants.associate { it.participantRef to it.participant!! }
        
        // Execute
        val result = cookingGroupService.calculateIngredientsPerCookingGroup(
            eventId = eventId,
            date = testDate,
            meals = listOf(meal),
            participants = participants
        )
        
        // Verify
        assertEquals(1, result.size)
        assertTrue(result.containsKey("Group A"))
        val groupAIngredients = result["Group A"]!!
        assertEquals(1, groupAIngredients.size) // Salt should be aggregated
        assertEquals("Salt", groupAIngredients[0].ingredient?.name)
        assertEquals(16.0, groupAIngredients[0].amount) // (5g + 3g) * 2 participants
    }

    @Test
    fun `structured data is sorted by cooking group name`() = runTest {
        val eventId = "event9"
        val testDate = LocalDate(2024, 1, 15)
        
        // Create ingredient
        val flour = Ingredient().apply {
            uid = "flour"
            name = "Flour"
        }
        
        val flourIngredient = ShoppingIngredient().apply {
            ingredientRef = "flour"
            amount = 100.0
            unit = IngredientUnit.GRAMM
            ingredient = flour
        }
        
        val recipe = Recipe().apply {
            uid = "flour-recipe"
            shoppingIngredients = listOf(flourIngredient)
        }
        
        val recipeSelection = RecipeSelection().apply {
            this.recipe = recipe
            eaterIds = mutableSetOf("p1", "p2", "p3")
        }
        
        val meal = Meal(day = HelperFunctions.getInstant(testDate)).apply {
            recipeSelections = listOf(recipeSelection)
        }
        
        // Create participants with different cooking groups (intentionally unsorted)
        val participants = listOf(
            createParticipantTime("p1", "Zebra Group"),
            createParticipantTime("p2", "Alpha Group"),
            createParticipantTime("p3", "Beta Group")
        )
        
        // Setup fake repo
        fakeRepo.mealsForEvent = mutableListOf(meal)
        fakeRepo.participants = participants.associate { it.participantRef to it.participant!! }
        
        // Execute
        val result = cookingGroupService.getCookingGroupIngredientsForDay(
            eventId = eventId,
            date = testDate,
            meals = listOf(meal),
            participants = participants
        )
        
        // Verify sorting
        val groupNames = result.map { it.cookingGroupName }
        assertEquals(listOf("Alpha Group", "Beta Group", "Zebra Group"), groupNames)
    }

    // Helper method to create ParticipantTime with cooking group
    private fun createParticipantTime(participantId: String, cookingGroup: String): ParticipantTime {
        val participant = model.Participant().apply {
            uid = participantId
            firstName = "Participant"
            lastName = participantId
        }
        
        return ParticipantTime(
            participant = participant,
            from = Clock.System.now(),
            to = Clock.System.now(),
            uid = "pt_$participantId",
            participantRef = participantId,
            cookingGroup = cookingGroup
        )
    }
}