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

    /**
     * Helper for navigating to PhotoPicker with a paperSize stored in SavedStateHandle.
     * The caller should set the paperSize on the SavedStateHandle before navigating.
     */
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
