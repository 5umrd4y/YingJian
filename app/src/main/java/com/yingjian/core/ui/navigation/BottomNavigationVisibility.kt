package com.yingjian.core.ui.navigation

fun shouldShowBottomNavigationBar(
    currentRoute: String?,
    isLandscape: Boolean
): Boolean {
    val isPhotobookPreview = currentRoute == NavDestinations.Preview.route ||
        currentRoute?.startsWith("photobook_preview/") == true
    return !(isLandscape && isPhotobookPreview)
}
