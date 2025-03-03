package model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import view.shared.HelperFunctions.Companion.generateRandomStringId


@Serializable
class RecipeSelection() {
    var uid: String = generateRandomStringId()
    var eaterIds: MutableSet<String> = mutableSetOf();
    var recipeRef: String = ""
    var selectedRecipeName: String = ""

    @Transient
    var recipe: Recipe? = null
}