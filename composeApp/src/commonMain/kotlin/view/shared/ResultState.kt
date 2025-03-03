package view.shared

sealed class ResultState<out T> {
    data object Loading : ResultState<Nothing>()
    data class Error(val message: String) : ResultState<Nothing>()
    data class Success<out T>(val data: T) : ResultState<T>()

    fun getSuccessData(): T? {
        return when (this) {
            is Success -> data
            else -> null
        }
    }
}