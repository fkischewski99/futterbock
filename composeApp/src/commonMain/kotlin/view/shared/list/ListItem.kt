package view.shared.list

interface ListItem<T> {
    fun getTitle(): String;
    fun getSubtitle(): String;
    fun getItem(): T
    fun getId(): String
}