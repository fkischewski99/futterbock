package view.event.new_meal_screen

import co.touchlab.kermit.Logger
import data.EventRepository
import kotlinx.datetime.LocalDate
import model.Meal
import model.MealType
import model.ParticipantTime
import model.Recipe
import model.RecipeSelection
import view.event.EventState
import view.event.new_event.groupMealsByDate
import view.shared.HelperFunctions
import view.shared.ResultState

class HandleEditMealActions(
    private val eventRepository: EventRepository,
) {
    suspend fun handleAction(
        currentState: EventState,
        editEventActions: EditMealActions
    ): ResultState<EventState> {
        return when (editEventActions) {
            is EditMealActions.ChangeMealType -> changeMealTypeOfEvent(
                currentState = currentState,
                mealType = editEventActions.mealType
            )

            is EditMealActions.SelectRecipe -> selectRecipe(
                currentState = currentState,
                recipeToAdd = editEventActions.recipie
            )

            is EditMealActions.ChangeDateOfMeal -> changeDateOfMeal(
                currentState = currentState,
                newDay = editEventActions.newDayOfMeal
            )

            is EditMealActions.SaveMeal -> saveMeal(currentState = currentState)
            is EditMealActions.RemoveEaterFromRecipe -> removeEaterFromRecipe(
                currentState = currentState,
                recipeWhereEaterIsToRemoveFrom = editEventActions.recipeSelection,
                participantToRemove = editEventActions.participantTime
            )

            is EditMealActions.AddEaterToRecipe -> addEaterToRecipe(
                currentState = currentState,
                recipeSelection = editEventActions.recipeSelection,
                participantTime = editEventActions.participantTime
            )

            is EditMealActions.DeleteRecipe -> deleteRecipe(
                currentState = currentState,
                recipeSelection = editEventActions.recipeSelection
            )

            else -> {
                Logger.i(editEventActions.toString())
                return ResultState.Error("wrong action")
            }
        }
    }

    private fun deleteRecipe(
        currentState: EventState,
        recipeSelection: RecipeSelection
    ): ResultState<EventState> {
        recipeSelection.eaterIds = mutableSetOf()
        val newRecipeSelections =
            currentState.selectedMeal.recipeSelections.filter { it != recipeSelection }
        newRecipeSelections.forEach { recipeSelectionIt ->
            recipeSelectionIt.eaterIds = recipeSelectionIt.eaterIds.toMutableSet()
        }
        currentState.selectedMeal.recipeSelections = newRecipeSelections;
        return ResultState.Success(
            currentState.copy(
                selectedMeal = currentState.selectedMeal
            )
        )
    }

    private fun addEaterToRecipe(
        currentState: EventState,
        recipeSelection: RecipeSelection,
        participantTime: ParticipantTime
    ): ResultState<EventState> {
        recipeSelection.eaterIds.add(participantTime.participantRef)
        return ResultState.Success(currentState)
    }

    private fun removeEaterFromRecipe(
        currentState: EventState,
        recipeWhereEaterIsToRemoveFrom: RecipeSelection,
        participantToRemove: ParticipantTime
    ): ResultState<EventState> {
        recipeWhereEaterIsToRemoveFrom.eaterIds.remove(participantToRemove.participantRef)
        return ResultState.Success(currentState)
    }

    private suspend fun saveMeal(currentState: EventState): ResultState<EventState> {
        eventRepository.updateMeal(eventId = currentState.event.uid, currentState.selectedMeal)
        return ResultState.Success(currentState)
    }

    private fun changeDateOfMeal(
        currentState: EventState,
        newDay: LocalDate
    ): ResultState<EventState> {
        val newMeal = currentState.selectedMeal.copy(day = HelperFunctions.getInstant(newDay))
        return updateMeal(currentState = currentState, newMeal = newMeal)
    }

    private fun selectRecipe(
        currentState: EventState,
        recipeToAdd: Recipe
    ): ResultState<EventState> {
        Logger.i("select recipe")
        val recepieSelection = RecipeSelection().apply {
            recipe = recipeToAdd
            recipeRef = recipeToAdd.uid
            selectedRecipeName = recipeToAdd.name
        }

        val idList = mutableSetOf<String>() // Change String to match the type of your IDs
        for (obj in currentState.currentParticipantsOfMeal) {
            idList.add(obj.participantRef)
        }
        recepieSelection.apply {
            eaterIds = idList
        }

        val mutableList = mutableListOf(recepieSelection)
        mutableList.addAll(currentState.selectedMeal.recipeSelections.toMutableList())
        currentState.selectedMeal.recipeSelections = mutableList
        Logger.i("Added recipe")
        return ResultState.Success(
            currentState.copy(selectedMeal = currentState.selectedMeal)
        )
    }

    private fun changeMealTypeOfEvent(
        currentState: EventState,
        mealType: MealType
    ): ResultState<EventState> {
        val newMeal = currentState.selectedMeal.copy(mealType = mealType)
        return updateMeal(currentState = currentState, newMeal = newMeal)

    }

    private fun updateMeal(
        currentState: EventState,
        newMeal: Meal
    ): ResultState.Success<EventState> {
        val newMealList =
            currentState.mealList.filter { meal: Meal -> meal.uid != newMeal.uid }.toMutableList()
        newMealList.add(newMeal)
        return ResultState.Success(
            currentState.copy(
                selectedMeal = newMeal,
                mealList = newMealList,
                mealsGroupedByDate = groupMealsByDate(
                    currentState.event.from,
                    currentState.event.to,
                    currentState.mealList
                )
            )
        )
    }

}