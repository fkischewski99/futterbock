package data.local.dao

import androidx.room.Dao
import androidx.room.Query
import model.ParticipantTime

@Dao
interface ParticipantTimeDao : BaseDao<ParticipantTime> {
    @Query("SELECT * FROM participant_times WHERE eventId = :eventId")
    suspend fun getByEventId(eventId: String): List<ParticipantTime>

    @Query("SELECT COUNT(*) FROM participant_times WHERE eventId = :eventId")
    suspend fun getCountByEventId(eventId: String): Int

    @Query("DELETE FROM participant_times WHERE eventId = :eventId AND participantRef = :participantId")
    suspend fun deleteByEventAndParticipant(eventId: String, participantId: String)

    @Query("DELETE FROM participant_times WHERE participantRef = :participantId")
    suspend fun deleteByParticipantId(participantId: String)
}
