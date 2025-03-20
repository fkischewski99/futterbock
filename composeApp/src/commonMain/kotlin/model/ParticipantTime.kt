package model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import view.shared.HelperFunctions
import view.shared.list.ListItem

@Serializable
class ParticipantTime(
    var uid: String = "",
    @Transient
    var participant: Participant? = null,
    var from: Instant,
    var to: Instant,
    val participantRef: String
) :
    ListItem<ParticipantTime> {

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