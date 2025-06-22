package view.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import services.update.UpdateChecker
import services.update.UpdateInfo

/**
 * Update notification banner that checks for updates and displays notification
 * Only shown on desktop platforms
 */
@Composable
fun UpdateNotificationBanner() {
    val updateChecker: UpdateChecker = koinInject()
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isVisible by remember { mutableStateOf(true) }
    var isChecking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Check for cached update info first
    LaunchedEffect(Unit) {
        val cached = updateChecker.getCachedUpdateInfo()
        if (cached != null) {
            updateInfo = cached
        } else {
            // Check for updates in background
            isChecking = true
            scope.launch {
                try {
                    val update = updateChecker.checkForUpdates()
                    updateInfo = update
                } catch (e: Exception) {
                    // Silently fail - update checking is not critical
                } finally {
                    isChecking = false
                }
            }
        }
    }
    
    // Show notification if update is available and user hasn't dismissed it
    if (isVisible && updateInfo != null) {
        UpdateNotificationCard(
            updateInfo = updateInfo!!,
            onDismiss = { isVisible = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateNotificationCard(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.SystemUpdate,
                contentDescription = "Update verfügbar",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Update gefunden",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Text(
                    text = "Bitte laden Sie die neue App Version herunter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                
                Text(
                    text = "Version ${updateInfo.version}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Download button
            Button(
                onClick = {
                    try {
                        uriHandler.openUri(updateInfo.downloadUrl)
                    } catch (e: Exception) {
                        // Fallback to release page if direct download fails
                        uriHandler.openUri(updateInfo.releaseUrl)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("Download", color = MaterialTheme.colorScheme.onPrimary)
            }
            
            // Dismiss button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Schließen",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Compact update notification for situations where space is limited
 */
@Composable
fun CompactUpdateNotification() {
    val updateChecker: UpdateChecker = koinInject()
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var isVisible by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    
    LaunchedEffect(Unit) {
        val cached = updateChecker.getCachedUpdateInfo()
        if (cached != null) {
            updateInfo = cached
        } else {
            scope.launch {
                try {
                    val update = updateChecker.checkForUpdates()
                    updateInfo = update
                } catch (e: Exception) {
                    // Silently fail
                }
            }
        }
    }
    
    if (isVisible && updateInfo != null) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    try {
                        uriHandler.openUri(updateInfo!!.downloadUrl)
                    } catch (e: Exception) {
                        uriHandler.openUri(updateInfo!!.releaseUrl)
                    }
                }
                .padding(horizontal = 16.dp, vertical = 4.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = "Update verfügbar",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Update verfügbar - Jetzt herunterladen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(
                    onClick = { isVisible = false },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Schließen",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}