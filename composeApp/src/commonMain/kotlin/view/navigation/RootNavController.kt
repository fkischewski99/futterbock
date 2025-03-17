package view.navigation

import CategorizedShoppingListViewModel
import LoadingScreen
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import data.EventRepository
import org.koin.compose.koinInject
import services.login.LoginAndRegister
import view.admin.new_participant.NewParticipantScreen
import view.event.categorized_shopping_list.MaterialListScreen
import view.event.categorized_shopping_list.ShoppingList
import view.event.categorized_shopping_list.ShoppingListScreen
import view.event.homescreen.EventOverviewScreen
import view.event.new_event.NewEventScreen
import view.event.new_meal_screen.EditMealScreen
import view.event.participants.ParticipantScreen
import view.event.participants.ParticipantSearchBarScreen
import view.event.recepie_overview_screen.RecipeOverviewScreen
import view.event.recepie_overview_screen.RecipeOverviewViewModel
import view.login.LoginScreen
import view.login.Register

@Composable
fun RootNavController(

) {
    val navController = rememberNavController()
    val loginService = koinInject<LoginAndRegister>()
    val eventRepository = koinInject<EventRepository>()

    NavHost(
        navController = navController,
        startDestination = getStartDestination(loginService)
    ) {
        composable<Routes.LoadingScreen> {
            LoadingScreen()
        }
        composable<Routes.Login> {
            LoginScreen(
                navigateToRegister = { navController.navigate(Routes.Register) },
                navigateToHome = { navController.navigate(Routes.Home) }
            )
        }
        composable<Routes.Register> {
            Register(
                onRegisterNavigation = { navController.navigate(Routes.Home) },
                onBackNavigation = { navController.navigateUp() }
            )
        }
        composable<Routes.Home> {
            EventOverviewScreen(
                navController = navController
            )
        }
        navigation<Routes.EditEventSubGraph>(
            startDestination = Routes.EditEvent,
        ) {
            composable<Routes.EditEvent> {
                NewEventScreen(
                    navController = navController
                )
            }
            composable<Routes.EditMeal> {
                EditMealScreen(navController)
            }
            composable<Routes.ParticipantsOfEvent> {
                ParticipantScreen(navController)
            }
            composable<Routes.AddOrRemoveParticipantsOfEvent> {
                ParticipantSearchBarScreen(navController)
            }
            composable<Routes.CreateNewParticipant> {
                NewParticipantScreen(navController)
            }
            composable<Routes.RecipeOverview> {
                RecipeOverviewScreen(navController)
            }
            composable<Routes.ShoppingList> {
                ShoppingListScreen(navController)
            }
            composable<Routes.MaterialList> {
                MaterialListScreen(navController)
            }
        }
    }
}

fun getStartDestination(loginService: LoginAndRegister): Any {
    if (loginService.isAuthenticated()) {
        return Routes.Home
    }
    return Routes.Login
}