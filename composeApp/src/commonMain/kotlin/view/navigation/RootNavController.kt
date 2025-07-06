package view.navigation

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
import view.admin.csv_import.CsvImportScreen
import view.admin.new_participant.NewParticipantScreen
import view.admin.participants.ParticipantAdminScreen
import view.event.categorized_shopping_list.MaterialListScreen
import view.event.categorized_shopping_list.ShoppingListScreen
import view.event.homescreen.EventOverviewScreen
import view.event.new_event.NewEventScreen
import view.event.new_meal_screen.EditMealScreen
import view.event.participants.ParticipantScreen
import view.event.participants.ParticipantSearchBarScreen
import view.event.cooking_groups.CookingGroupsScreen
import view.event.recepie_overview_screen.RecipeOverviewScreen
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
        composable<Routes.ParticipantAdministration> {
            ParticipantAdminScreen(navController = navController)
        }
        composable<Routes.CsvImport> {
            CsvImportScreen(navController = navController)
        }
        composable<Routes.EventCsvImport> { backStackEntry ->
            val route = backStackEntry.toRoute<Routes.EventCsvImport>()
            CsvImportScreen(
                navController = navController,
                eventId = route.eventId
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
            composable<Routes.CreateOrEditParticipant> {
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
            composable<Routes.CookingGroups> {
                CookingGroupsScreen(navController)
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