package modules

import CategorizedShoppingListViewModel
import MaterialListViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import org.koin.dsl.module
import view.admin.csv_import.CsvImportViewModel
import view.admin.new_participant.ViewModelNewParticipant
import view.event.new_meal_screen.RecipeViewModel
import view.event.SharedEventViewModel
import view.event.categorized_shopping_list.IngredientViewModel
import view.event.homescreen.ViewModelEventOverview
import view.event.new_meal_screen.AllParticipantsViewModel
import view.event.recepie_overview_screen.RecipeOverviewViewModel
import view.event.cooking_groups.ingredients.CookingGroupIngredientsViewModel

val viewModelModules = module {
    single { ViewModelEventOverview(get(), get()) }
    single { CategorizedShoppingListViewModel(get(), get()) }
    single { ViewModelNewParticipant(get()) }
    single { SharedEventViewModel(get(), get(), get()) }
    single { RecipeViewModel(get()) }
    single { IngredientViewModel(get()) }
    single { AllParticipantsViewModel(get()) }
    single { RecipeOverviewViewModel(get(), get()) }
    single { MaterialListViewModel(get(), get()) }
    single { CsvImportViewModel(get(), get()) }
    single { CookingGroupIngredientsViewModel(get()) }
}