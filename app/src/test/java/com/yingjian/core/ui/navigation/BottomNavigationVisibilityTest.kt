package com.yingjian.core.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomNavigationVisibilityTest {
    @Test
    fun `hides bottom navigation for photobook preview in landscape`() {
        assertFalse(
            shouldShowBottomNavigationBar(
                currentRoute = NavDestinations.Preview.route,
                isLandscape = true
            )
        )
    }

    @Test
    fun `keeps bottom navigation for photobook preview in portrait`() {
        assertTrue(
            shouldShowBottomNavigationBar(
                currentRoute = NavDestinations.Preview.route,
                isLandscape = false
            )
        )
    }

    @Test
    fun `keeps bottom navigation for other landscape screens`() {
        assertTrue(
            shouldShowBottomNavigationBar(
                currentRoute = NavDestinations.PhotobookEditor.route,
                isLandscape = true
            )
        )
    }
}
