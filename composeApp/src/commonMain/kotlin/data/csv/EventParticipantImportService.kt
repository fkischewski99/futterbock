package data.csv

import co.touchlab.kermit.Logger
import data.EventRepository
import model.Participant

data class EventImportResult(
    val participantsAddedToEvent: Int,
    val participantsCreated: Int,
    val participantsFound: Int,
    val errors: List<EventImportError>
)

data class EventImportError(
    val rowIndex: Int,
    val participantName: String,
    val error: String
)

class EventParticipantImportService(
    private val eventRepository: EventRepository
) {

    suspend fun importParticipantsToEvent(
        eventId: String,
        participants: List<ParticipantImportData>,
        addParticipantCallback: (Participant) -> Unit
    ): EventImportResult {

        val existingEventParticipants = try {
            eventRepository.getParticipantsOfEvent(eventId, withParticipant = false)
                .map { it.participantRef }
                .toSet()
        } catch (e: Exception) {
            Logger.w("Could not load existing event participants, proceeding without duplicate check")
            emptySet<String>()
        }
        Logger.i(
            "Loaded set of existing event participants with ids: " + existingEventParticipants.joinToString(
                ", "
            )
        )


        var participantsAddedToEvent = 0
        var participantsCreated = 0
        var participantsFound = 0
        val errors = mutableListOf<EventImportError>()
        val createdParticipants = mutableListOf<Participant>()

        participants.forEach { importData ->
            try {
                val participantName = "${importData.firstName} ${importData.lastName}"

                // Look for existing participant by name
                val foundParticipant = eventRepository.findParticipantByName(
                    importData.firstName,
                    importData.lastName
                )
                var participant = foundParticipant
                if (participant != null) {
                    participantsFound++
                    Logger.i("Found existing participant: $participantName")
                } else {
                    // Create new participant
                    participant = Participant().apply {
                        firstName = importData.firstName
                        lastName = importData.lastName
                        birthdate = importData.birthDate
                        eatingHabit = importData.eatingHabit
                    }
                    createdParticipants.add(participant)
                    participant = eventRepository.createNewParticipant(participant)
                    if (participant == null) {
                        errors.add(
                            EventImportError(
                                importData.rowIndex,
                                participantName,
                                "Teilnehmer konnte nicht erstellt werden"
                            )
                        )
                        return@forEach
                    } else {
                        participantsCreated++
                        Logger.i("Created new participant: $participantName")
                    }
                }

                // Check if participant is already in the event
                Logger.i("Checking if participant ${participant.uid} is in existing participants: $existingEventParticipants")
                if (existingEventParticipants.contains(participant.uid)) {
                    Logger.i("Participant $participantName already in event, skipping")
                    return@forEach
                }

                // Add participant to event
                try {
                    participantsAddedToEvent++
                    addParticipantCallback(participant!!)
                    Logger.i("Added participant $participantName to event")
                } catch (e: Exception) {
                    Logger.e("Error adding participant to event: $participantName", e)
                    errors.add(
                        EventImportError(
                            importData.rowIndex,
                            participantName,
                            "Teilnehmer konnte nicht zum Event hinzugefügt werden: ${e.message}"
                        )
                    )
                }

            } catch (e: Exception) {
                val participantName = "${importData.firstName} ${importData.lastName}"
                Logger.e("Error processing participant: $participantName", e)
                errors.add(
                    EventImportError(
                        importData.rowIndex,
                        participantName,
                        "Unerwarteter Fehler: ${e.message}"
                    )
                )
            }
        }

        return EventImportResult(
            participantsAddedToEvent = participantsAddedToEvent,
            participantsCreated = participantsCreated,
            participantsFound = participantsFound,
            errors = errors
        )
    }
}