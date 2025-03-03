package model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
class Participant {
    var uid: String = "";
    var allergies: MutableList<String> = mutableListOf();
    var group: String = ""
    var birthdate: Instant? = null;
    var eatingHabit: EatingHabit = EatingHabit.OMNIVORE
    var firstName: String = "";
    var lastName: String = "";
}





