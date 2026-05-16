package com.yingjian.core.ui.navigation

sealed class NavDestinations(val route: String) {
    data object Memories : NavDestinations("memories")
    data object Photobook : NavDestinations("photobook")
    data object Settings : NavDestinations("settings")
    data object About : NavDestinations("about")
    // Route has no parameters — URIs/dates passed via savedStateHandle
    data object NewPost : NavDestinations("new_post")
    data object MemoryDetail : NavDestinations("memory_detail/{memoryId}") {
        fun createRoute(memoryId: Long) = "memory_detail/$memoryId"
    }
    data object PhotoPicker : NavDestinations("photo_picker?mode={mode}") {
        fun createRoute(mode: String = "") =
            if (mode.isEmpty()) "photo_picker" else "photo_picker?mode=$mode"
    }
    data object PhotobookEditor : NavDestinations("photobook_editor/{photobookId}") {
        fun createRoute(photobookId: Long) = "photobook_editor/$photobookId"
    }
    data object Preview : NavDestinations("photobook_preview/{photobookId}") {
        fun createRoute(photobookId: Long) = "photobook_preview/$photobookId"
    }
}
