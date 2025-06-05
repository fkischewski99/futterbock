package services.shoppingList

import data.EventRepository
import data.FakeEventRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import model.*
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import kotlin.test.*

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
        fakeRepo.shoppingIngredients = mutableListOf(existing)

        val result = calculator.calculate(eventId)
        assertEquals(1, result.size)
        assertEquals("Extra cheese", result[0].note)
        assertEquals(2.0, result[0].amount)
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


    // -- Helpers --

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
