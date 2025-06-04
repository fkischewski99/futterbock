package services.event

import data.EventRepository
import data.FakeEventRepository
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import kotlin.test.BeforeTest
import org.koin.test.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import model.EatingHabit
import model.FoodIntolerance
import model.Participant
import model.ParticipantTime
import model.Recipe
import model.RecipeSelection
import model.ShoppingIngredient
import org.koin.core.context.stopKoin
import kotlin.test.DefaultAsserter.assertEquals
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParticipantCanEatRecipeTest : KoinTest {

    private lateinit var canParticipantCanEatRecipe: ParticipantCanEatRecipe

    // Koin module
    private val testModule = module {
        single<EventRepository> { FakeEventRepository() }
        single { ParticipantCanEatRecipe(get()) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        stopKoin()
        // Start Koin
        startKoin {
            modules(testModule)
        }
        canParticipantCanEatRecipe = get()

        // Set the main dispatcher for testing
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @Test
    fun `returns true when participant has no restrictions`() = runTest {
        val participant = Participant().apply {
            eatingHabit = EatingHabit.OMNIVORE
            allergies = emptyList()
            intolerances = emptyList()
        }

        val recipe = Recipe().apply {
            dietaryHabit = EatingHabit.OMNIVORE
            shoppingIngredients = emptyList()
            foodIntolerances = emptyList()
        }

        val member = ParticipantTime(
            from = Clock.System.now(),
            to = Clock.System.now(),
            participantRef = "abc"
        ).apply { this.participant = participant }
        val recipeSelection = RecipeSelection().apply { this.recipe = recipe }

        assertTrue(canParticipantCanEatRecipe.canParticipantEatRecipe(member, recipeSelection))
        assertNull(
            canParticipantCanEatRecipe.getErrorMessageForParticipant(
                member,
                recipeSelection
            )
        )
    }

    @Test
    fun `returns false if participant's eating habit does not match recipe`() = runTest {
        val participant = Participant().apply {
            eatingHabit = EatingHabit.VEGAN
        }

        val recipe = Recipe().apply {
            dietaryHabit = EatingHabit.OMNIVORE
        }

        val member = ParticipantTime(
            from = Clock.System.now(),
            to = Clock.System.now(),
            participantRef = "abc"
        ).apply { this.participant = participant }
        val recipeSelection = RecipeSelection().apply { this.recipe = recipe }

        assertFalse(canParticipantCanEatRecipe.canParticipantEatRecipe(member, recipeSelection))
        assertEquals(
            expected = "Essgewohnheit stimmt nicht überein",
            actual = canParticipantCanEatRecipe.getErrorMessageForParticipant(
                member,
                recipeSelection
            ),
            message = "Fehlermeldung zur essgewohnheit wird nicht ausgegeben"
        )
    }

    @Test
    fun `returns false if participant has an allergy in the recipe`() = runTest {
        val allergyId = "ingredient-1"

        val participant = Participant().apply {
            eatingHabit = EatingHabit.OMNIVORE
            allergies = listOf(allergyId)
        }

        val recipe = Recipe().apply {
            dietaryHabit = EatingHabit.OMNIVORE
            shoppingIngredients = listOf(
                ShoppingIngredient().apply { ingredientRef = allergyId }
            )
        }

        val member = ParticipantTime(
            from = Clock.System.now(),
            to = Clock.System.now(),
            participantRef = "abc"
        ).apply { this.participant = participant }
        val recipeSelection = RecipeSelection().apply { this.recipe = recipe }

        assertFalse(canParticipantCanEatRecipe.canParticipantEatRecipe(member, recipeSelection))
        assertEquals(
            expected = "Teilnehmer ist allergisch gegen eine der Zutaten: TestIngredient",
            actual = canParticipantCanEatRecipe.getErrorMessageForParticipant(
                member,
                recipeSelection
            ),
            message = "Fehlermeldung zur allergie wird nicht ausgegeben"
        )
    }

    @Test
    fun `returns false if participant has intolerance not handled by recipe`() = runTest {
        val participant = Participant().apply {
            eatingHabit = EatingHabit.OMNIVORE
            intolerances = listOf(FoodIntolerance.GLUTEN_INTOLERANCE)
        }

        val recipe = Recipe().apply {
            dietaryHabit = EatingHabit.OMNIVORE
            foodIntolerances = emptyList() // does not cover GLUTEN
        }

        val member = ParticipantTime(
            from = Clock.System.now(),
            to = Clock.System.now(),
            participantRef = "abc"
        ).apply { this.participant = participant }
        val recipeSelection = RecipeSelection().apply { this.recipe = recipe }

        assertFalse(canParticipantCanEatRecipe.canParticipantEatRecipe(member, recipeSelection))
        assertEquals(
            expected = "Teilnehmer hat eine Unverträglichkeit: " + FoodIntolerance.GLUTEN_INTOLERANCE.displayName,
            actual = canParticipantCanEatRecipe.getErrorMessageForParticipant(
                member,
                recipeSelection
            ),
            message = "Fehlermeldung zur Unverträglichkeit wird nicht ausgegeben"
        )
    }
}