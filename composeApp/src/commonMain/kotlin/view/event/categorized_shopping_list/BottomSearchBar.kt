package view.event.categorized_shopping_list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times
import kotlinx.coroutines.launch
import view.shared.list.ListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> BottomSheetWithSearchBar(
    content: @Composable () -> Unit,
    items: List<ListItem<T>>,
    onItemAdded: (ListItem<T>) -> Unit,
    listItemFromString: (String) -> ListItem<T>
) {
    val coroutineScope = rememberCoroutineScope()

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded
        )
    )
    Scaffold { paddingValues ->
        BoxWithConstraints(Modifier.fillMaxSize(), propagateMinConstraints = true) {
            val maxHeight = this.maxHeight
            val maxWidth = this.maxWidth

            BottomSheetScaffold(
                modifier = Modifier.fillMaxWidth(),
                sheetPeekHeight = 128.dp,
                sheetShape = RoundedCornerShape(topEnd = 20.dp, topStart = 20.dp),
                sheetShadowElevation = 20.dp,
                sheetMaxWidth = maxWidth,
                snackbarHost = {
                    SnackbarHost(hostState = scaffoldState.snackbarHostState)
                },
                sheetContent = {
                    SearchBarComponent(
                        items = items,
                        maxHeight = maxHeight,
                        onItemAdded = { name ->
                            coroutineScope.launch {
                                scaffoldState.snackbarHostState.showSnackbar(
                                    message = "${name.getListItemTitle()} wurde hinzugefügt",
                                    duration = SnackbarDuration.Short
                                )
                            }
                            onItemAdded(name)
                        },
                        onActiveChanged = {
                            coroutineScope.launch {
                                if (it) {
                                    scaffoldState.bottomSheetState.expand()
                                } else {
                                    scaffoldState.bottomSheetState.partialExpand()
                                }
                            }
                        },
                        listItemFromString = listItemFromString
                    )
                },
                scaffoldState = scaffoldState,
                content = {
                    Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) { content() }

                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun <T> SearchBarComponent(
    items: List<ListItem<T>>,
    onItemAdded: (ListItem<T>) -> Unit,
    listItemFromString: (String) -> ListItem<T>,
    onActiveChanged: (Boolean) -> Unit,
    maxHeight: Dp
) {
    var searchText by remember { mutableStateOf("") }
    val matches = remember(searchText) {
        items.filter { it.getListItemTitle().contains(searchText, ignoreCase = true) }
    }
    var bottomSheetExpanded by remember { mutableStateOf(false) }
    var currentListItem by remember { mutableStateOf<ListItem<T>?>(null) }


    Column(
        modifier = Modifier
            .heightIn(min = 200.dp, max = 0.7 * maxHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        DockedSearchBar(
            modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp)
                .height(0.7 * maxHeight).fillMaxWidth(),
            inputField = {
                SearchBarDefaults.InputField(
                    modifier = Modifier.fillMaxWidth(),
                    query = searchText,
                    onSearch = { bottomSheetExpanded = false },
                    expanded = bottomSheetExpanded,
                    onExpandedChange = {
                        bottomSheetExpanded = it;
                        onActiveChanged(it)
                    },
                    placeholder = { Text("Artikel hinzufügen") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Suchen"
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            modifier = Modifier.align(alignment = Alignment.End),
                            onClick = {
                                if (searchText.isNotEmpty()) {
                                    searchText = ""
                                } else {
                                    bottomSheetExpanded = false
                                    onActiveChanged(false)
                                }
                            }) {
                            if (searchText.isNotEmpty()) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Hinzufügen",
                                    modifier = Modifier.clickable {
                                        if (currentListItem != null && currentListItem!!.getListItemTitle() == searchText) {
                                            onItemAdded(currentListItem!!)
                                            currentListItem = null
                                            searchText = ""
                                            return@clickable
                                        }
                                        onItemAdded(listItemFromString(searchText))
                                        searchText = ""
                                    }
                                )
                            } else {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Schliessen"
                                )
                            }
                        }
                    },
                    onQueryChange = { searchText = it },
                )
            },
            expanded = bottomSheetExpanded,
            onExpandedChange = { bottomSheetExpanded = it },
        ) {
            if (matches.isNotEmpty() && searchText.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    FlowRow(modifier = Modifier.padding(horizontal = 8.dp)) {
                        matches.take(20).forEach { item ->
                            AssistChip(
                                onClick = {
                                    searchText = item.getListItemTitle()
                                    currentListItem = item
                                },
                                label = { Text(item.getListItemTitle()) },
                                modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
            } else {
                Text("Keine Vorschläge", modifier = Modifier.padding(16.dp))
            }
        }
    }
}