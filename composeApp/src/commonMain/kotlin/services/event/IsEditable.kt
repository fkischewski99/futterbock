package services.event

import kotlin.time.Clock
import kotlinx.datetime.Instant


fun eventIsEditable(eventEnd: Instant): Boolean {
    return eventEnd > Clock.System.now()
}