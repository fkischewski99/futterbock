package data.sync

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_operations")
data class PendingOperation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val operationType: String,
    val entityId: String,
    val parentId: String? = null,
    val payloadJson: String,
    val timestamp: Long
)
