package view.event.categorized_shopping_list

import MaterialListState
import MaterialListViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import org.koin.compose.koinInject
import view.event.actions.BaseAction
import view.event.actions.NavigationActions
import view.event.actions.handleNavigation
import view.event.materiallist.EditMaterialListActions
import view.shared.MGCircularProgressIndicator
import view.shared.NavigationIconButton
import view.shared.ResultState

@Composable
fun MaterialListScreen(navController: NavHostController) {
    val viewModelMaterialList: MaterialListViewModel = koinInject()
    val state = viewModelMaterialList.state.collectAsStateWithLifecycle()

    MaterialList(
        onAction = { action ->
            when (action) {
                is NavigationActions -> handleNavigation(navController, action)
                is EditMaterialListActions -> viewModelMaterialList.onAction(action)
            }
        },
        state = state.value
    )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialList(
    onAction: (BaseAction) -> Unit,
    state: ResultState<MaterialListState>,
) {
    // Inject view Model

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        TopAppBar(title = {
            Text(text = "Materialliste")
        }, navigationIcon = {
            NavigationIconButton(
                onLeave = {
                    onAction(EditShoppingListActions.SaveToEvent)
                    onAction(NavigationActions.GoBack)
                }

            )
        })
    }) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
                .padding(top = it.calculateTopPadding(), start = 8.dp, bottom = 24.dp, end = 8.dp)
        ) {
            when (state) {

                is ResultState.Success -> {
                    MaterialList(
                        materialList = state.data.materialList,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Information",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Hier sind Erweiterungen geplant",
                            fontSize = 16.sp
                        )
                    }
                }

                is ResultState.Error -> Text("Fehler beim abrufen der Einkaufsliste")
                ResultState.Loading -> MGCircularProgressIndicator()
            }
        }
    }
}

@Composable
fun MaterialList(
    materialList: Map<String, Int>,
) {
    Spacer(modifier = Modifier.height(16.dp))
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        materialList.forEach { ingredient ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)

            ) {
                Text("- ${ingredient.value}x ${ingredient.key}")
            }
        }
    }
}