package services.shoppingList

import data.EventRepository
import data.FakeEventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import model.DailyShoppingList
import model.Ingredient
import model.IngredientUnit
import model.MultiDayShoppingList
import model.Meal
import model.Participant
import model.Recipe
import model.RecipeSelection
import model.ShoppingIngredient
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import view.shared.HelperFunctions
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CalculateShoppingListTest : KoinTest {

    private lateinit var fakeRepo: FakeEventRepository
    private lateinit var calculator: CalculateShoppingList

    private val testModule = module {
        single<EventRepository> { FakeEventRepository().also { fakeRepo = it } }
        single { CalculateShoppingList(get()) }
    }

    @BeforeTest
    fun setup() {
        stopKoin()
        startKoin { modules(testModule) }

        calculator = get()

        Dispatchers.setMain(StandardTestDispatcher())
    }

    @Test
    fun `calculates amount with one recipe and one ingredient`() = runTest {
        val eventId = "event1"
        val ingredient = Ingredient().apply { uid = "ing1"; name = "Tomato" }
        val selection = recipeSelectionWithIngredient(
            "ing1",
            100.0,
            IngredientUnit.GRAMM,
            listOf("u1", "u2", "u3")
        )
        selection.recipe!!.shoppingIngredients[0].ingredient = ingredient

        fakeRepo.mealsForEvent = mutableListOf(mealWithRecipe(selection))

        val result = calculator.calculate(eventId)

        assertEquals(1, result.size)
        assertEquals("Tomato", result[0].ingredient?.name)
        assertEquals(300.0, result[0].amount)
    }

    @Test
    fun `sums up same ingredient from two recipes`() = runTest {
        val eventId = "event2"
        val tomato = Ingredient().apply { uid = "ing1"; name = "Tomato" }

        val s1 =
            recipeSelectionWithIngredient("ing1", 100.0, IngredientUnit.GRAMM, listOf("a", "b"))
        val s2 =
            recipeSelectionWithIngredient("ing1", 50.0, IngredientUnit.GRAMM, listOf("x", "y", "z"))

        s1.recipe!!.shoppingIngredients[0].ingredient = tomato
        s2.recipe!!.shoppingIngredients[0].ingredient = tomato

        fakeRepo.mealsForEvent = mutableListOf(mealWithRecipe(s1), mealWithRecipe(s2))

        val result = calculator.calculate(eventId)
        assertEquals(1, result.size)
        assertEquals("Tomato", result[0].ingredient?.name)
        assertEquals(100.0 * 2 + 50.0 * 3, result[0].amount)
    }

    @Test
    fun `ingredient appears in two meals with same recipe`() = runTest {
        val eventId = "event3"
        val tomato = Ingredient().apply { uid = "ing1"; name = "Tomato" }

        val selection =
            recipeSelectionWithIngredient("ing1", 30.0, IngredientUnit.GRAMM, listOf("u1", "u2"))
        selection.recipe!!.shoppingIngredients[0].ingredient = tomato

        fakeRepo.mealsForEvent = mutableListOf(mealWithRecipe(selection), mealWithRecipe(selection))

        val result = calculator.calculate(eventId)
        assertEquals(1, result.size)
        assertEquals(30.0 * 2 * 2, result[0].amount) // 2 meals, 2 eaters each
    }

    @Test
    fun `unit conversion with HAFERL works correctly`() = runTest {
        val eventId = "event4"

        val oat = Ingredient().apply {
            uid = "ing1"
            name = "Oats"
            amountHaferl = 100.0
            unitHaferl = IngredientUnit.GRAMM
        }

        val ing = ShoppingIngredient().apply {
            ingredientRef = "ing1"
            amount = 1.0
            unit = IngredientUnit.HAFERL
            ingredient = oat
        }

        val recipe = Recipe().apply {
            uid = "rec"
            shoppingIngredients = listOf(ing)
        }

        val selection = RecipeSelection().apply {
            this.recipe = recipe
            this.eaterIds = mutableSetOf("a", "b")
        }

        fakeRepo.mealsForEvent = mutableListOf(mealWithRecipe(selection))

        val result = calculator.calculate(eventId)
        assertEquals(1, result.size)
        assertEquals(200.0, result[0].amount)
        assertEquals(IngredientUnit.GRAMM, result[0].unit)
    }

    @Test
    fun `ingredient details from existing shopping list are retained`() = runTest {
        val eventId = "event5"

        val ing = Ingredient().apply { uid = "ing1"; name = "Cheese" }

        val existing = ShoppingIngredient().apply {
            ingredientRef = "ing1"
            ingredient = ing
            note = "Extra cheese"
            unit = IngredientUnit.STUECK
        }

        val selection =
            recipeSelectionWithIngredient("ing1", 1.0, IngredientUnit.STUECK, listOf("1", "2"))
        selection.recipe!!.shoppingIngredients[0].ingredient = ing

        fakeRepo.mealsForEvent = mutableListOf(mealWithRecipe(selection))
        
        // Set up multiday shopping list with existing ingredient
        val testDate = LocalDate(2023, 1, 1)
        val dailyList = DailyShoppingList(
            purchaseDate = testDate,
            ingredients = listOf(existing)
        )
        val multiDayList = MultiDayShoppingList(
            eventId = eventId,
            dailyLists = mapOf(testDate to dailyList)
        )
        fakeRepo.multiDayShoppingLists[eventId] = multiDayList

        val result = calculator.calculate(eventId)
        assertEquals(1, result.size)
        val cheeseItem = result.find { it.ingredient?.name == "Cheese" }!!
        assertEquals("Extra cheese", cheeseItem.note)
        assertEquals(2.0, cheeseItem.amount)
    }

    @Test
    fun `handles multiple distinct ingredients correctly`() = runTest {
        val eventId = "event6"

        val ing1 = Ingredient().apply { uid = "ing1"; name = "Rice" }
        val ing2 = Ingredient().apply { uid = "ing2"; name = "Beans" }

        val s1 =
            recipeSelectionWithIngredient("ing1", 50.0, IngredientUnit.GRAMM, listOf("u1", "u2"))
        val s2 = recipeSelectionWithIngredient(
            "ing2",
            80.0,
            IngredientUnit.GRAMM,
            listOf("u1", "u2", "u3")
        )

        s1.recipe!!.shoppingIngredients[0].ingredient = ing1
        s2.recipe!!.shoppingIngredients[0].ingredient = ing2

        fakeRepo.mealsForEvent = mutableListOf(mealWithRecipe(s1), mealWithRecipe(s2))

        val result = calculator.calculate(eventId).sortedBy { it.ingredient?.name }
        assertEquals(2, result.size)
        assertEquals("Beans", result[0].ingredient?.name)
        assertEquals(240.0, result[0].amount)
        assertEquals("Rice", result[1].ingredient?.name)
        assertEquals(100.0, result[1].amount)
    }

    @Test
    fun `calculates correct total for many recipes and ingredients`() = runTest {
        val eventId = "event-big"

        val ingredientCount = 50
        val recipeCount = 20

        val allIngredients = (1..ingredientCount).map {
            Ingredient().apply {
                uid = "ing$it"
                name = "Ingredient$it"
            }
        }

        val allSelections = mutableListOf<RecipeSelection>()
        repeat(recipeCount) { rIdx ->
            val eaterIds = List((2..5).random()) { "eater${rIdx}_$it" }
            val ingredientsPerRecipe = allIngredients.shuffled().take(5)

            var calculatedShoppingIngredients = ingredientsPerRecipe.mapIndexed { i, ing ->
                ShoppingIngredient().apply {
                    ingredientRef = ing.uid
                    amount = 10.0 + i
                    unit = IngredientUnit.GRAMM
                    ingredient = ing
                }
            }

            val recipe = Recipe().apply {
                uid = "recipe$rIdx"
                shoppingIngredients = calculatedShoppingIngredients
            }

            allSelections += RecipeSelection().apply {
                this.recipe = recipe
                this.eaterIds = eaterIds.toMutableSet()
            }
        }

        fakeRepo.mealsForEvent = allSelections.map { mealWithRecipe(it) }.toMutableList()

        val result = calculator.calculate(eventId)

        // Check some overall expectations
        assertTrue(result.isNotEmpty(), "Result should not be empty")
        assertTrue(result.size <= ingredientCount, "No more unique ingredients than created")

        // Spot check a known ingredient
        val sampleIngredient = allIngredients[0]
        val totalExpected = allSelections
            .flatMap { it.recipe?.shoppingIngredients ?: emptyList() }
            .filter { it.ingredientRef == sampleIngredient.uid }
            .sumOf {
                it.amount * allSelections.first { sel ->
                    sel.recipe?.shoppingIngredients?.contains(
                        it
                    ) == true
                }.eaterIds.size
            }

        val matched = result.find { it.ingredientRef == sampleIngredient.uid }
        if (matched != null) {
            assertEquals(sampleIngredient.name, matched.ingredient?.name)
            assertEquals(totalExpected, matched.amount)
        }
    }

    @Test
    fun `calculate adjusts amounts by participant age multipliers`() = runTest {
        val eventId = "event-age-test"

        var newIngredient = Ingredient().apply { uid = "ing1"; name = "Milk" }
        val shoppingIngredient = ShoppingIngredient().apply {
            ingredientRef = "ing1"
            amount = 100.0
            unit = IngredientUnit.MILLILITER
            ingredient = newIngredient
        }
        var testRecipe = Recipe().apply {
            uid = "recipe-age-test"
            shoppingIngredients = listOf(shoppingIngredient)
        }

        val now = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()

        fun birthdateYearsAgo(yearsAgo: Int): Instant {
            val localDate = now.toLocalDateTime(timeZone).date
            val birthDate = localDate.minus(DatePeriod(years = yearsAgo))
            return HelperFunctions.getInstant(birthDate)
        }

        fakeRepo.participants = mapOf(
            "p1" to createParticipant("p1", birthdateYearsAgo(3)),
            "p2" to createParticipant("p2", birthdateYearsAgo(8)),
            "p3" to createParticipant("p3", birthdateYearsAgo(13)),
            "p4" to createParticipant("p4", birthdateYearsAgo(19)),
            "p5" to createParticipant("p5", birthdateYearsAgo(40)),
        )

        val recipeSelection = RecipeSelection().apply {
            recipe = testRecipe
            eaterIds = mutableSetOf("p1", "p2", "p3", "p4", "p5")
        }

        fakeRepo.mealsForEvent = mutableListOf(mealWithRecipe(recipeSelection))

        val result = calculator.calculate(eventId)
        assertEquals(1, result.size)
        val totalAmount = 100.0 * (0.4 + 0.7 + 1.0 + 1.2 + 1.0)
        assertEquals(totalAmount, result[0].amount, 0.0001)
    }

    @Test
    fun `calculates amount with guests only no participants`() = runTest {
        val eventId = "event-guests-only"
        val ingredient = Ingredient().apply { uid = "ing1"; name = "Pasta" }
        val selection = recipeSelectionWithIngredient(
            "ing1",
            50.0,
            IngredientUnit.GRAMM,
            listOf()
        )
        selection.guestCount = 4
        selection.recipe!!.shoppingIngredients[0].ingredient = ingredient

        fakeRepo.mealsForEvent = mutableListOf(mealWithRecipe(selection))

        val result = calculator.calculate(eventId)

        assertEquals(1, result.size)
        assertEquals("Pasta", result[0].ingredient?.name)
        assertEquals(200.0, result[0].amount)
    }

    @Test
    fun `calculates amount with participants and guests combined`() = runTest {
        val eventId = "event-participants-and-guests"
        val ingredient = Ingredient().apply { uid = "ing1"; name = "Rice" }
        val selection = recipeSelectionWithIngredient(
            "ing1",
            100.0,
            IngredientUnit.GRAMM,
            listOf("p1", "p2")
        )
        selection.guestCount = 3
        selection.recipe!!.shoppingIngredients[0].ingredient = ingredient

        fakeRepo.mealsForEvent = mutableListOf(mealWithRecipe(selection))

        val result = calculator.calculate(eventId)

        assertEquals(1, result.size)
        assertEquals("Rice", result[0].ingredient?.name)
        assertEquals(500.0, result[0].amount)
    }

    @Test
    fun `guest count is preserved when combining same ingredients from different recipes`() = runTest {
        val eventId = "event-multiple-recipes-with-guests"
        val tomato = Ingredient().apply { uid = "ing1"; name = "Tomato" }

        val s1 = recipeSelectionWithIngredient("ing1", 50.0, IngredientUnit.GRAMM, listOf("p1"))
        s1.guestCount = 2
        s1.recipe!!.shoppingIngredients[0].ingredient = tomato

        val s2 = recipeSelectionWithIngredient("ing1", 30.0, IngredientUnit.GRAMM, listOf("p2", "p3"))
        s2.guestCount = 1
        s2.recipe!!.shoppingIngredients[0].ingredient = tomato

        fakeRepo.mealsForEvent = mutableListOf(mealWithRecipe(s1), mealWithRecipe(s2))

        val result = calculator.calculate(eventId)
        assertEquals(1, result.size)
        assertEquals("Tomato", result[0].ingredient?.name)
        assertEquals((50.0 * 3) + (30.0 * 3), result[0].amount)
    }

    @Test
    fun `guest count zero works correctly backward compatibility`() = runTest {
        val eventId = "event-zero-guests"
        val ingredient = Ingredient().apply { uid = "ing1"; name = "Cheese" }
        val selection = recipeSelectionWithIngredient(
            "ing1",
            200.0,
            IngredientUnit.GRAMM,
            listOf("p1", "p2")
        )
        selection.guestCount = 0
        selection.recipe!!.shoppingIngredients[0].ingredient = ingredient

        fakeRepo.mealsForEvent = mutableListOf(mealWithRecipe(selection))

        val result = calculator.calculate(eventId)

        assertEquals(1, result.size)
        assertEquals("Cheese", result[0].ingredient?.name)
        assertEquals(400.0, result[0].amount)
    }

    @Test
    fun `guest count with age-based participant multipliers works correctly`() = runTest {
        val eventId = "event-guests-with-age-multipliers"
        
        val ingredient = Ingredient().apply { uid = "ing1"; name = "Bread" }
        val shoppingIngredient = ShoppingIngredient().apply {
            ingredientRef = "ing1"
            amount = 100.0
            unit = IngredientUnit.GRAMM
            this.ingredient = ingredient
        }
        val recipe = Recipe().apply {
            uid = "recipe-bread"
            shoppingIngredients = listOf(shoppingIngredient)
        }

        val now = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        
        fun birthdateYearsAgo(yearsAgo: Int): Instant {
            val localDate = now.toLocalDateTime(timeZone).date
            val birthDate = localDate.minus(DatePeriod(years = yearsAgo))
            return HelperFunctions.getInstant(birthDate)
        }

        fakeRepo.participants = mapOf(
            "child" to createParticipant("child", birthdateYearsAgo(5)),
            "adult" to createParticipant("adult", birthdateYearsAgo(30))
        )

        val recipeSelection = RecipeSelection().apply {
            this.recipe = recipe
            eaterIds = mutableSetOf("child", "adult")
            guestCount = 2
        }

        fakeRepo.mealsForEvent = mutableListOf(mealWithRecipe(recipeSelection))

        val result = calculator.calculate(eventId)
        assertEquals(1, result.size)
        val expectedAmount = 100.0 * (0.7 + 1.0 + 2.0)
        assertEquals(expectedAmount, result[0].amount, 0.0001)
    }

    @Test
    fun `calculateAmountsForRecipe applies multiplier correctly`() = runTest {
        // Set up one ingredient
        val ingredient = Ingredient().apply {
            uid = "ingredient-1"
            name = "Sugar"
        }

        val shoppingIngredient = ShoppingIngredient().apply {
            amount = 100.0
            unit = IngredientUnit.GRAMM
            ingredientRef = "ingredient-1"
            this.ingredient = ingredient
        }

        val recipe = Recipe().apply {
            shoppingIngredients = listOf(shoppingIngredient)
        }

        val recipeSelection = RecipeSelection().apply {
            this.recipe = recipe
        }

        val map = mutableMapOf<String, ShoppingIngredient>()

        // First call with multiplier 2
        val firstCallMap =
            calculator.calculateAmountsForRecipe(map, recipeSelection, multiplier = 2.0)
        val afterFirstCallNewAmount = firstCallMap["ingredient-1GRAMM"]?.amount ?: 0.0
        assertEquals(
            200.0,
            afterFirstCallNewAmount,
            0.01,
            "First multiplier (2.0) should yield 200g"
        )

        // Second call with multiplier 3
        val ingredientMap =
            calculator.calculateAmountsForRecipe(firstCallMap, recipeSelection, multiplier = 3.0)
        val afterSecondCall = ingredientMap["ingredient-1GRAMM"]?.amount ?: 0.0
        val afterFirstCallExistingAmount = firstCallMap["ingredient-1GRAMM"]?.amount ?: 0.0
        assertEquals(
            500.0,
            afterSecondCall,
            0.01,
            "Second multiplier (3.0) should add 300 to existing amount (500)"
        )
        assertEquals(
            200.0,
            afterFirstCallExistingAmount,
            0.01,
            "existing ingredient should not be changed"
        )

        // Third call with multiplier 4
        val thirdCallMap =
            calculator.calculateAmountsForRecipe(mutableMapOf(), recipeSelection, multiplier = 4.0)
        val afterThirdCall = thirdCallMap["ingredient-1GRAMM"]?.amount ?: 0.0
        assertEquals(
            400.0,
            afterThirdCall,
            0.01,
            "Calculates the next amount correctly"
        )
    }


    // -- Helpers --

    private fun createParticipant(id: String, birthdate: Instant) =
        Participant().apply {
            this.uid = id
            this.birthdate = birthdate
        }

    private fun recipeSelectionWithIngredient(
        ingredientRef: String,
        amount: Double,
        unit: IngredientUnit,
        eaterIds: List<String>
    ): RecipeSelection {
        val ingredient = ShoppingIngredient().apply {
            this.ingredientRef = ingredientRef
            this.amount = amount
            this.unit = unit
        }

        val recipe = Recipe().apply {
            uid = "recipe-${ingredientRef}"
            shoppingIngredients = listOf(ingredient)
        }

        return RecipeSelection().apply {
            this.recipe = recipe
            this.eaterIds = eaterIds.toMutableSet()
        }
    }

    private fun mealWithRecipe(selection: RecipeSelection): Meal {
        return Meal(day = Clock.System.now()).apply {
            recipeSelections = listOf(selection)
        }
    }
}
