import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import view.shared.list.ListItem


@Composable
fun <T> CardWithList(
    title: String,
    listItems: List<ListItem<T>>,
    addItemToList: (() -> Unit)? = null,
    onDeleteClick: ((ListItem<T>) -> Unit)? = null,
    onListItemClick: ((ListItem<T>) -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        listItems.forEach { listItem ->
            ListItemComponent(
                listItem = listItem,
                onDeleteClick = onDeleteClick,
                onListItemClick = onListItemClick
            )
        }
        if (listItems.isEmpty()) {
            Spacer(Modifier.height(16.dp))
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            addItemToList?.let {
                FloatingActionButton(
                    onClick = { addItemToList.invoke() },
                    modifier = Modifier.clip(shape = RoundedCornerShape(50)),
                    containerColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add, contentDescription = "Add Icon"
                    )
                }
            }
        }
    }
}

@Composable
fun <T> ListItemComponent(
    listItem: ListItem<T>,
    onDeleteClick: ((ListItem<T>) -> Unit)? = null,
    onListItemClick: ((ListItem<T>) -> Unit)? = null
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable {
                if (onListItemClick != null) {
                    onListItemClick(listItem)
                }
            }.padding(16.dp)
    ) {
        Row {
            Column(modifier = Modifier.weight(6f)) {
                Text(
                    text = listItem.getTitle(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = listItem.getSubtitle(),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            if (
                onDeleteClick != null
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { showDialog = true },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Item",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    if (showDialog) {
                        ConfirmDialog({ onDeleteClick(listItem) }, { showDialog = false })
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Löschen Bestätigen") },
        text = { Text(text = "Willst du das Item wirklich löschen?") },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                }
            ) {
                Text("Bestätigen")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss
            ) {
                Text("Abbrechen")
            }
        }
    )
}

