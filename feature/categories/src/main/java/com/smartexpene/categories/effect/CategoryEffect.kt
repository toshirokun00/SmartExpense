package com.smartexpene.categories.effect

sealed interface CategoryEffect {

    data object CategoryAdded : CategoryEffect

    data object CategoryUpdated : CategoryEffect

    data object CategoryDeleted : CategoryEffect

    data class ShowError(val message: String) : CategoryEffect

}