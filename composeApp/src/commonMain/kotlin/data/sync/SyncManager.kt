package data.sync

import co.touchlab.kermit.Logger
import data.FireBaseRepository
import data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import model.*

class SyncManager(
    private val db: AppDatabase,
    private val firebaseRepository: FireBaseRepository,
    private val networkMonitor: NetworkMonitor
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun startObserving() {
        networkMonitor.isOnline
            .collect { online ->
                if (online) {
                    processPendingOperations()
                }
            }
    }

    suspend fun processPendingOperations() {
        val pending = db.pendingOperationDao().getAllOrdered()
        if (pending.isEmpty()) return

        Logger.i("SyncManager: Processing ${pending.size} pending operations")

        for (op in pending) {
            try {
                executeOperation(op)
                db.pendingOperationDao().deleteById(op.id)
                Logger.d("SyncManager: Completed operation ${op.operationType} for ${op.entityId}")
            } catch (e: Exception) {
                Logger.e("SyncManager: Failed operation ${op.operationType} for ${op.entityId}: ${e.message}")
                db.pendingOperationDao().deleteById(op.id)
            }
        }
    }

    private suspend fun executeOperation(op: PendingOperation) {
        when (op.operationType) {
            "CREATE_EVENT", "UPDATE_EVENT" -> {
                val event = json.decodeFromString<Event>(op.payloadJson)
                firebaseRepository.saveExistingEvent(event)
            }
            "DELETE_EVENT" -> {
                firebaseRepository.deleteEvent(op.entityId)
            }
            "CREATE_PARTICIPANT", "UPDATE_PARTICIPANT" -> {
                val participant = json.decodeFromString<Participant>(op.payloadJson)
                firebaseRepository.updateParticipant(participant)
            }
            "DELETE_PARTICIPANT" -> {
                firebaseRepository.deleteParticipant(op.entityId)
            }
            "CREATE_MEAL", "UPDATE_MEAL" -> {
                val meal = json.decodeFromString<Meal>(op.payloadJson)
                val eventId = op.parentId ?: return
                firebaseRepository.updateMeal(eventId, meal)
            }
            "DELETE_MEAL" -> {
                val eventId = op.parentId ?: return
                firebaseRepository.deleteMeal(eventId, op.entityId)
            }
            "CREATE_RECIPE", "UPDATE_RECIPE" -> {
                val recipe = json.decodeFromString<Recipe>(op.payloadJson)
                firebaseRepository.updateRecipe(recipe)
            }
            "DELETE_RECIPE" -> {
                firebaseRepository.deleteRecipe(op.entityId)
            }
            "ADD_PARTICIPANT_TO_EVENT" -> {
                val eventId = op.parentId ?: return
                firebaseRepository.updateParticipantTime(eventId,
                    json.decodeFromString<ParticipantTime>(op.payloadJson))
            }
            "DELETE_PARTICIPANT_OF_EVENT" -> {
                val eventId = op.parentId ?: return
                firebaseRepository.deleteParticipantOfEvent(eventId, op.entityId)
            }
            "UPDATE_PARTICIPANT_TIME" -> {
                val eventId = op.parentId ?: return
                firebaseRepository.updateParticipantTime(eventId,
                    json.decodeFromString<ParticipantTime>(op.payloadJson))
            }
            "SAVE_MULTI_DAY_SHOPPING_LIST" -> {
                val eventId = op.parentId ?: return
                val list = json.decodeFromString<MultiDayShoppingList>(op.payloadJson)
                firebaseRepository.saveMultiDayShoppingList(eventId, list)
            }
            "SAVE_MATERIAL_LIST" -> {
                val eventId = op.parentId ?: return
                val materials = json.decodeFromString<List<Material>>(op.payloadJson)
                firebaseRepository.saveMaterialList(eventId, materials)
            }
            "DELETE_MATERIAL" -> {
                val eventId = op.parentId ?: return
                firebaseRepository.deleteMaterialById(eventId, op.entityId)
            }
            else -> Logger.w("SyncManager: Unknown operation type: ${op.operationType}")
        }
    }

}
