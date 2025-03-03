package services.event

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant


fun eventIsEditable(eventEnd: Instant): Boolean {
    return eventEnd > Clock.System.now()
}