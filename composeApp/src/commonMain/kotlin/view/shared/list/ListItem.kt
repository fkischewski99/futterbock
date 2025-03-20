package view.shared.list

interface ListItem<T> {
    fun getListItemTitle(): String;
    fun getSubtitle(): String;
    fun getItem(): T
}