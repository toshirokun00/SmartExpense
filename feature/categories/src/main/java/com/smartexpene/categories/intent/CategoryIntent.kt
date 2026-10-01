package com.smartexpene.categories.intent

sealed interface CategoryIntent {

    data class NameChanged(
        val name : String
    ) : CategoryIntent

    data object AddCategory : CategoryIntent

    data object CancelEdit : CategoryIntent

    data class LoadCategory(
        val id: Long
    ) : CategoryIntent

    data object UpdateCategory : CategoryIntent

    data class RequestDeleteCategory(
        val id: Long
    ) : CategoryIntent

    data object ConfirmDeleteCategory : CategoryIntent

    data object CancelDeleteCategory : CategoryIntent
}