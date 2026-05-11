package com.yingjian.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun YingJianNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = NavDestinations.Memories.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(NavDestinations.Memories.route) {
            androidx.compose.material3.Text("影记")
        }
        composable(NavDestinations.Photobook.route) {
            androidx.compose.material3.Text("画册")
        }
        composable(NavDestinations.Settings.route) {
            androidx.compose.material3.Text("设置")
        }
    }
}
