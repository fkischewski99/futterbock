package model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import view.shared.HelperFunctions.Companion.generateRandomStringId


@Serializable
@Entity(tableName = "recipe_selections")
class RecipeSelection() {
    @PrimaryKey var uid: String = generateRandomStringId()
    var eaterIds: MutableSet<String> = mutableSetOf()
    var recipeRef: String = ""
    var selectedRecipeName: String = ""
    var guestCount: Int = 0

    @Transient
    @Ignore
    var recipe: Recipe? = null
}