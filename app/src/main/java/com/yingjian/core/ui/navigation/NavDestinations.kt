package com.yingjian.core.ui.navigation

sealed class NavDestinations(val route: String) {
    data object Memories : NavDestinations("memories")
    data object Photobook : NavDestinations("photobook")
    data object Settings : NavDestinations("settings")
    data object NewPost : NavDestinations("new_post")
    data object PhotoPicker : NavDestinations("photo_picker")
    data object PhotobookEditor : NavDestinations("photobook_editor/{photobookId}") {
        fun createRoute(photobookId: Long) = "photobook_editor/$photobookId"
    }
}
