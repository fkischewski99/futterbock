package model

import kotlinx.serialization.Serializable
import view.shared.HelperFunctions
import view.shared.list.ListItem

@Serializable
class Material : ListItem<Material> {
    var uid = ""
    var name: String = "";
    var source: Source = Source.ENTERED_BY_USER
    var amount: Int = 0

    override fun getListItemTitle(): String {
        return name
    }

    override fun getSubtitle(): String {
        return source.toString()
    }

    override fun getItem(): Material {
        return this
    }
}