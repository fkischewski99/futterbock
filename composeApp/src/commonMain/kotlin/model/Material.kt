package model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import view.shared.HelperFunctions
import view.shared.list.ListItem

@Serializable
@Entity(tableName = "materials")
class Material : ListItem<Material> {
    @PrimaryKey var uid: String = ""
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