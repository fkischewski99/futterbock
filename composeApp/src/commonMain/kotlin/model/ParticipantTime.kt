package model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import view.shared.HelperFunctions
import view.shared.list.ListItem

@Serializable
@Entity(
    tableName = "participant_times",
    foreignKeys = [
        ForeignKey(
            entity = Event::class,
            parentColumns = ["uid"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Participant::class,
            parentColumns = ["uid"],
            childColumns = ["participantRef"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
class ParticipantTime(
    @PrimaryKey var uid: String = "",
    var from: Instant,
    var to: Instant,
    var participantRef: String = "",
    var cookingGroup: String = ""
) :
    ListItem<ParticipantTime> {

    @Transient
    @Ignore
    var participant: Participant? = null

    @Transient
    var eventId: String = ""

    override fun getListItemTitle(): String {
        return (this.participant?.firstName?.trim()
            ?: "") + " " + (this.participant?.lastName?.trim()
            ?: "") // Example: Mapping 'name' property to getTitle()
    }

    override fun getSubtitle(): String {
        return HelperFunctions.formatDate(from) + " - " +
                HelperFunctions.formatDate(
                    to
                )

    }

    override fun getItem(): ParticipantTime {
        return this
    }


}