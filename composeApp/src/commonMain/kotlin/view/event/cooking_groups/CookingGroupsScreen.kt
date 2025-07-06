package view.event.cooking_groups

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import model.ParticipantTime
import org.koin.compose.koinInject
import view.event.SharedEventViewModel
import view.event.actions.NavigationActions
import view.event.actions.handleNavigation
import view.login.ErrorField
import view.shared.EditTextDialog
import view.shared.MGCircularProgressIndicator
import view.shared.NavigationIconButton
import view.shared.ResultState

data class CookingGroup(
    val name: String,
    val participants: List<ParticipantTime>
)

@Composable
fun CookingGroupsScreen(
    navController: NavHostController
) {
    val sharedEventViewModel: SharedEventViewModel = koinInject()
    val state = sharedEventViewModel.eventState.collectAsStateWithLifecycle()

    CookingGroupsContent(
        state = state.value,
        onAction = { action ->
            when (action) {
                is NavigationActions -> handleNavigation(navController, action)
                is CookingGroupActions -> sharedEventViewModel.onAction(action)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CookingGroupsContent(
    state: ResultState<view.event.EventState>,
    onAction: (Any) -> Unit
) {
    var expandedGroups by remember { mutableStateOf(setOf<String>()) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var selectedGroupForRename by remember { mutableStateOf("") }
    var dropTargetGroup by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var additionalGroups by remember { mutableStateOf(listOf<CookingGroup>()) }
    
    // Optimistic updates for immediate UI feedback
    var optimisticParticipantMoves by remember { mutableStateOf(mapOf<String, String>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kochgruppen") },
                navigationIcon = {
                    NavigationIconButton(onLeave = { onAction(NavigationActions.GoBack) })
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Neue Kochgruppe erstellen"
                )
            }
        }
    ) { paddingValues ->
        when (state) {
            is ResultState.Loading -> MGCircularProgressIndicator()
            is ResultState.Error -> ErrorField(errorMessage = state.message)
            is ResultState.Success -> {
                // Apply optimistic updates to participant list
                val updatedParticipantList = state.data.participantList.map { participant ->
                    optimisticParticipantMoves[participant.uid]?.let { newGroup ->
                        participant.apply { cookingGroup = newGroup }
                    } ?: participant
                }
                
                val participantGroups = groupParticipantsByCookingGroup(updatedParticipantList)
                val allGroups = (participantGroups + additionalGroups).distinctBy { it.name }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (allGroups.isEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Group,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Keine Kochgruppen vorhanden",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(allGroups) { group ->
                            CookingGroupCard(
                                group = group,
                                isExpanded = expandedGroups.contains(group.name),
                                isDraggedOver = dropTargetGroup == group.name,
                                onToggleExpansion = { groupName ->
                                    expandedGroups = if (expandedGroups.contains(groupName)) {
                                        expandedGroups - groupName
                                    } else {
                                        expandedGroups + groupName
                                    }
                                },
                                onRenameGroup = { groupName ->
                                    selectedGroupForRename = groupName
                                    showRenameDialog = true
                                },
                                onMoveParticipant = { participant, targetGroup ->
                                    // Optimistic update - immediately update UI
                                    optimisticParticipantMoves = optimisticParticipantMoves + (participant.uid to targetGroup)
                                    
                                    // Ensure target group exists in additional groups if not in participant groups
                                    if (additionalGroups.none { it.name == targetGroup } && participantGroups.none { it.name == targetGroup }) {
                                        additionalGroups = additionalGroups + CookingGroup(name = targetGroup, participants = emptyList())
                                    }
                                    
                                    // Dispatch backend action
                                    onAction(
                                        CookingGroupActions.MoveBetweenGroups(
                                            participant,
                                            targetGroup
                                        )
                                    )
                                },
                                onMergeGroups = { targetGroupName, groupsToMerge ->
                                    onAction(
                                        CookingGroupActions.MergeGroups(
                                            groupsToMerge,
                                            targetGroupName
                                        )
                                    )
                                    // Update UI state after merge
                                    expandedGroups =
                                        expandedGroups.filterNot { groupsToMerge.contains(it) }
                                            .toSet()
                                    // Update additional groups - remove merged groups and add new target group if it doesn't exist in participants
                                    additionalGroups =
                                        additionalGroups.filterNot { groupsToMerge.contains(it.name) }
                                            .let { filtered ->
                                                if (filtered.none { it.name == targetGroupName }) {
                                                    filtered + CookingGroup(
                                                        name = targetGroupName,
                                                        participants = emptyList()
                                                    )
                                                } else {
                                                    filtered
                                                }
                                            }
                                },
                                availableGroups = allGroups
                            )
                        }
                    }
                }
                if (showCreateDialog) {
                    EditTextDialog(
                        title = "Neue Kochgruppe erstellen",
                        label = "Gruppenname",
                        initialValue = "",
                        confirmButtonText = "Erstellen",
                        onConfirm = { groupName ->
                            if (groupName.isNotBlank() && allGroups.find { it.name == groupName } == null) {
                                val newGroup =
                                    CookingGroup(name = groupName, participants = emptyList())
                                additionalGroups = additionalGroups + newGroup
                            }
                            showCreateDialog = false
                        },
                        onDismiss = {
                            showCreateDialog = false
                        }
                    )
                }

                if (showRenameDialog) {
                    EditTextDialog(
                        title = "Kochgruppe umbenennen",
                        label = "Neuer Gruppenname",
                        initialValue = selectedGroupForRename.takeIf { it != "Keine Gruppe" } ?: "",
                        onConfirm = { newName ->
                            if (newName.isNotBlank()) {
                                onAction(
                                    CookingGroupActions.RenameGroup(
                                        selectedGroupForRename,
                                        newName
                                    )
                                )
                            }
                            showRenameDialog = false
                            selectedGroupForRename = ""
                        },
                        onDismiss = {
                            showRenameDialog = false
                            selectedGroupForRename = ""
                        }
                    )
                }
            }
        }
    }


}

@Composable
private fun CookingGroupCard(
    group: CookingGroup,
    isExpanded: Boolean,
    isDraggedOver: Boolean = false,
    onToggleExpansion: (String) -> Unit,
    onRenameGroup: (String) -> Unit,
    onMoveParticipant: (ParticipantTime, String) -> Unit,
    onMergeGroups: (String, List<String>) -> Unit,
    availableGroups: List<CookingGroup>
) {
    var showMergeDialog by remember { mutableStateOf(false) }

    val cardModifier = Modifier
        .fillMaxWidth()
        .then(
            if (isDraggedOver) {
                Modifier
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
            } else {
                Modifier
            }
        )

    Card(
        modifier = cardModifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpansion(group.name) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.DragIndicator,
                        contentDescription = "Drag handle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${group.participants.size} Teilnehmende",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    IconButton(onClick = { showMergeDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Merge,
                            contentDescription = "Gruppe zusammenführen"
                        )
                    }
                    IconButton(onClick = { onRenameGroup(group.name) }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Gruppe umbenennen"
                        )
                    }
                    IconButton(onClick = { onToggleExpansion(group.name) }) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Zuklappen" else "Aufklappen"
                        )
                    }
                }
            }

            if (isExpanded) {
                HorizontalDivider()
                Column(modifier = Modifier.padding(16.dp)) {
                    group.participants.forEach { participant ->
                        ParticipantItem(
                            participant = participant,
                            onMoveToGroup = { targetGroup ->
                                onMoveParticipant(participant, targetGroup)
                            },
                            availableGroups = availableGroups.filter { it.name != group.name }
                                .map { it.name }
                        )
                        if (participant != group.participants.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
    if (showMergeDialog) {
        MergeGroupsDialog(
            allGroups = availableGroups,
            onConfirm = { targetGroupName, groupsToMerge ->
                onMergeGroups(targetGroupName, groupsToMerge)
                showMergeDialog = false
            },
            onDismiss = {
                showMergeDialog = false
            },
            name = group.name
        )
    }
}

@Composable
private fun ParticipantItem(
    participant: ParticipantTime,
    onMoveToGroup: (String) -> Unit,
    availableGroups: List<String>
) {
    var showMoveMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = participant.getListItemTitle(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Box {
                TextButton(onClick = { showMoveMenu = true }) {
                    Text("Verschieben")
                }

                DropdownMenu(
                    expanded = showMoveMenu,
                    onDismissRequest = { showMoveMenu = false }
                ) {
                    availableGroups.forEach { groupName ->
                        DropdownMenuItem(
                            text = { Text(groupName) },
                            onClick = {
                                onMoveToGroup(groupName)
                                showMoveMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MergeGroupsDialog(
    allGroups: List<CookingGroup>,
    onConfirm: (String, List<String>) -> Unit,
    onDismiss: () -> Unit,
    name: String
) {
    var targetGroupName by remember { mutableStateOf("") }
    var groupsToMerge by remember { mutableStateOf(setOf(name)) }

    val participantsToMerge = allGroups
        .filter { groupsToMerge.contains(it.name) }
        .flatMap { it.participants }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kochgruppen zusammenführen") },
        text = {
            Column {
                Text(
                    text = "Wähle die Gruppen aus, die zusammengeführt werden sollen:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Group selection
                LazyColumn(
                    modifier = Modifier
                        .height(180.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        ),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(allGroups) { group ->
                        val isSelected = groupsToMerge.contains(group.name)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    groupsToMerge = if (isSelected) {
                                        groupsToMerge - group.name
                                    } else {
                                        groupsToMerge + group.name
                                    }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    groupsToMerge = if (checked) {
                                        groupsToMerge + group.name
                                    } else {
                                        groupsToMerge - group.name
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${group.name} (${group.participants.size} Teilnehmende)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = targetGroupName,
                    onValueChange = { targetGroupName = it },
                    label = { Text("Name der neuen Gruppe") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (participantsToMerge.isNotEmpty()) {
                    Text(
                        text = "Teilnehmende die zusammengeführt werden (${participantsToMerge.size}):",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        modifier = Modifier
                            .height(120.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(8.dp)
                    ) {
                        items(participantsToMerge) { participant ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = participant.getListItemTitle(),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } else if (groupsToMerge.isNotEmpty()) {
                    Text(
                        text = "Keine Teilnehmenden in den ausgewählten Gruppen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (targetGroupName.isNotBlank() && groupsToMerge.size >= 2) {
                        onConfirm(targetGroupName, groupsToMerge.toList())
                    }
                },
                enabled = targetGroupName.isNotBlank() && groupsToMerge.size >= 2
            ) {
                Text("Zusammenführen")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

private fun groupParticipantsByCookingGroup(participants: List<ParticipantTime>): List<CookingGroup> {
    return participants
        .groupBy { it.cookingGroup.ifEmpty { "Keine Gruppe" } }
        .map { (groupName, participants) ->
            CookingGroup(
                name = groupName,
                participants = participants.sortedBy { it.participant?.firstName }
            )
        }
        .sortedBy { it.name }
}