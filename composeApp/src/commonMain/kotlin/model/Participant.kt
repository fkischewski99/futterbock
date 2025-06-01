package model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import view.shared.HelperFunctions
import view.shared.list.ListItem

@Serializable
class Participant : ListItem<Participant> {
    var uid: String = "";
    var allergies: MutableList<String> = mutableListOf();
    var group: String = ""
    var birthdate: Instant? = null;
    var eatingHabit: EatingHabit = EatingHabit.OMNIVORE
    var firstName: String = "";
    var lastName: String = "";

    override fun getListItemTitle(): String {
        return "$firstName $lastName"
    }

    override fun getSubtitle(): String {
        if (birthdate == null) {
            return ""
        }
        return HelperFunctions.formatDate(birthdate!!)
    }

    override fun getItem(): Participant {
        return this;
    }
}





