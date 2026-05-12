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
    data object PhotoPicker : NavDestinations("photo_picker")
    data object PhotobookEditor : NavDestinations("photobook_editor/{photobookId}") {
        fun createRoute(photobookId: Long) = "photobook_editor/$photobookId"
    }

    object PhotoPickerNav {
        fun navigate(navController: androidx.navigation.NavHostController, paperSize: String) {
            navController.currentBackStackEntry?.savedStateHandle?.set("paperSize", paperSize)
            navController.navigate(Photobook.route)
        }

        fun getPaperSize(backStackEntry: androidx.navigation.NavBackStackEntry): String? {
            return backStackEntry.savedStateHandle.get<String>("paperSize")
        }
    }
}
