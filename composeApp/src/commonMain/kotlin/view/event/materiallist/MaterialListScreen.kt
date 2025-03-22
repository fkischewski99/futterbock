package view.event.categorized_shopping_list

import MaterialListState
import MaterialListViewModel
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Divider
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import model.Material
import model.Source
import org.koin.compose.koinInject
import view.event.actions.BaseAction
import view.event.actions.NavigationActions
import view.event.actions.handleNavigation
import view.event.materiallist.EditMaterialListActions
import view.login.ErrorField
import view.shared.MGCircularProgressIndicator
import view.shared.NavigationIconButton
import view.shared.ResultState
import view.shared.page.ColumnWithPadding

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

    when (state) {

        is ResultState.Success -> {
            BottomSheetWithSearchBar(
                items = state.data.allMaterialList,
                content = {
                    MaterialList(
                        materialList = state.data.materialList,
                    )
                },
                onItemAdded = { text -> onAction(EditMaterialListActions.Add(text)) },
                topBar = {
                    TopAppBar(title = {
                        Text(text = "Materialliste")
                    }, navigationIcon = {
                        NavigationIconButton(
                            onLeave = {
                                onAction(EditMaterialListActions.SaveMaterialList)
                                onAction(NavigationActions.GoBack)
                            }

                        )
                    })
                })

        }

        is ResultState.Error -> ColumnWithPadding { ErrorField(state.message) }
        ResultState.Loading -> ColumnWithPadding { MGCircularProgressIndicator() }
    }
}

@Composable
fun MaterialList(
    materialList: List<Material>,
) {
    var scrollState = rememberScrollState()

    Spacer(modifier = Modifier.height(16.dp))
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp).verticalScroll(scrollState),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        materialList.forEach { material ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)

            ) {
                if (material.source == Source.ENTERED_BY_USER) {
                    Text("- ${material.name}", modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { EditMaterialListActions.Delete(material.uid) }) {

                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }

                } else {
                    Text("- ${material.amount}x ${material.name}")
                }
            }
            HorizontalDivider()
        }
    }
}