package model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import view.shared.HelperFunctions
import view.shared.list.ListItem

@Serializable
class Participant : ListItem<Participant> {
    var uid: String = "";
    var allergies: List<String> = emptyList();
    var intolerances: List<FoodIntolerance> = emptyList();
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
        var subtitle = HelperFunctions.formatDate(birthdate!!)
        if (intolerances.isNotEmpty()) {
            subtitle += " - ${intolerances.joinToString { it.displayName }}"
        }
        return subtitle
    }

    override fun getItem(): Participant {
        return this;
    }
}





