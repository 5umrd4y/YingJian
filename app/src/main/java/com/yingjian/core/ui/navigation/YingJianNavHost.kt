package com.yingjian.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.yingjian.AppDependencies

@Composable
fun YingJianNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = NavDestinations.Memories.route,
    deps: AppDependencies
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(NavDestinations.Memories.route) {
            // Placeholder - real MemoriesScreen in Task 6
            androidx.compose.material3.Text("影记")
        }
        composable(NavDestinations.Photobook.route) {
            // Placeholder - real PhotobookScreen in Task 7
            androidx.compose.material3.Text("画册")
        }
        composable(NavDestinations.Settings.route) {
            // Placeholder - real SettingsScreen in Task 11
            androidx.compose.material3.Text("设置")
        }
    }
}
