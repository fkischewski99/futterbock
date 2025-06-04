package model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import view.shared.HelperFunctions
import view.shared.list.ListItem

@Serializable
class Event(val group: String) : ListItem<Event> {
    var uid: String = ""
    var from: Instant = Clock.System.now();
    var to: Instant = Clock.System.now();

    var eventType: EventType = EventType.SONSTIGES;
    var kitchenSchedule: String? = "";
    var name: String = "";

    fun isFutureEvent(): Boolean {
        return this.to > Clock.System.now();
    }

    override fun getListItemTitle(): String {
        return this.name // Example: Mapping 'name' property to getTitle()
    }

    override fun getSubtitle(): String {
        return HelperFunctions.formatDate(this.from) + " - " + HelperFunctions.formatDate(
            this.to
        )
    }

    override fun getItem(): Event {
        return this
    }


}

enum class EventType(var state: String) {
    SONSTIGES("SONSTIGES"),
    BUNDESLAGER("BUNDESLAGER"),
    LANDESLAGER("LANDESLAGER"),
    BEZIRKSLAGER("BEZIRKSLAGER"),
    STAMMESLAGER("COMPLETE")
}