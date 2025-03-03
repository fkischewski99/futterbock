package view.event.actions

import androidx.navigation.NavHostController
import view.navigation.Routes

interface NavigationActions : BaseAction {
    data object GoBack : NavigationActions
    data class GoToRoute(val route: Routes) : NavigationActions
}

fun handleNavigation(navController: NavHostController, navigationActions: NavigationActions) {
    when (navigationActions) {
        is NavigationActions.GoBack -> navController.navigateUp()
        is NavigationActions.GoToRoute -> navController.navigate(navigationActions.route)
    }
}