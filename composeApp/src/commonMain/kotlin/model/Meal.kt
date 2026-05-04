package model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import view.shared.list.ListItem

@Serializable
@Entity(
    tableName = "meals",
    foreignKeys = [ForeignKey(
        entity = Event::class,
        parentColumns = ["uid"],
        childColumns = ["eventId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Meal(
    @PrimaryKey val uid: String = "",
    var day: Instant,
    var mealType: MealType = MealType.MITTAG,
    var recipeSelections: List<RecipeSelection> = emptyList(),
) : ListItem<Meal> {

    @kotlinx.serialization.Transient
    var eventId: String = ""

    override fun getListItemTitle(): String {
        return mealType.name
    }

    override fun getSubtitle(): String {
        return recipeSelections.joinToString(", ") { it.selectedRecipeName }
    }

    override fun getItem(): Meal {
        return this
    }
}