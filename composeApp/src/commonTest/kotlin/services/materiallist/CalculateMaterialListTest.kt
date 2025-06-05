package services.materiallist

import data.EventRepository
import data.FakeEventRepository
import model.Material
import model.Recipe
import model.RecipeSelection
import model.Source
import model.Meal
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.assertEquals

class CalculateMaterialListTest : KoinTest {

    private lateinit var calculateMaterialList: CalculateMaterialList
    private lateinit var repository: FakeEventRepository

    private val testModule = module {
        single<EventRepository> { FakeEventRepository().also { repository = it } }
        single { CalculateMaterialList(get()) }
    }

    @BeforeTest
    fun setUp() {
        stopKoin()
        startKoin {
            modules(testModule)
        }
        calculateMaterialList = get()
    }

    @Test
    fun `calculates material list with merged and max counts`() = runTest {
        val eventId = "event-123"

        // Fake data setup
        val recipe1 = Recipe().apply {
            materials = listOf("Fork", "Knife", "Knife") // Knife appears twice
        }
        val recipe2 = Recipe().apply {
            materials = listOf("Fork", "Spoon")
        }

        val meal = Meal(day = Clock.System.now()).apply {
            recipeSelections = listOf(
                RecipeSelection().apply { recipe = recipe1 },
                RecipeSelection().apply { recipe = recipe2 }
            )
        }

        repository.mealsForEvent.add(meal)
        repository.materialsForEvent.add(Material().apply {
            name = "Cup"; amount = 5; source = Source.ENTERED_BY_USER
        })

        val result = calculateMaterialList.calculate(eventId)

        // Assertions
        assertEquals(4, result.size)
        val fork = result.find { it.name == "Fork" }
        val knife = result.find { it.name == "Knife" }
        val spoon = result.find { it.name == "Spoon" }
        val cup = result.find { it.name == "Cup" }

        assertEquals(1, fork?.amount)
        assertEquals(2, knife?.amount)
        assertEquals(1, spoon?.amount)
        assertEquals(5, cup?.amount)
        assertEquals(Source.ENTERED_BY_USER, cup?.source)
        assertEquals(Source.COMPUTED, fork?.source)
    }

    @Test
    fun `returns only user-defined materials when no meals exist`() = runTest {
        val eventId = "event-empty"

        repository.mealsForEvent = mutableListOf()
        repository.materialsForEvent = mutableListOf(Material().apply {
            name = "Plate"; amount = 2; source = Source.ENTERED_BY_USER
        })

        val result = calculateMaterialList.calculate(eventId)

        assertEquals(1, result.size)
        assertEquals("Plate", result[0].name)
        assertEquals(2, result[0].amount)
        assertEquals(Source.ENTERED_BY_USER, result[0].source)
    }
}
