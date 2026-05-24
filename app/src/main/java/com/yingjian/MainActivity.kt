package com.yingjian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yingjian.core.ui.common.YingJianBottomNavigationBar
import com.yingjian.core.ui.navigation.YingJianNavHost
import com.yingjian.core.ui.navigation.shouldShowBottomNavigationBar
import com.yingjian.core.ui.theme.YingJianTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deps = (application as YingJianApplication).deps

        setContent {
            YingJianTheme {
                val navController = rememberNavController()
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                val configuration = LocalConfiguration.current
                val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
                val showBottomNavigation = shouldShowBottomNavigationBar(
                    currentRoute = currentRoute,
                    isLandscape = isLandscape
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomNavigation) {
                            YingJianBottomNavigationBar(
                                currentRoute = currentRoute,
                                onNavigate = { dest ->
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    YingJianNavHost(
                        navController = navController,
                        modifier = if (showBottomNavigation) {
                            Modifier.padding(innerPadding)
                        } else {
                            Modifier
                        },
                        deps = deps
                    )
                }
            }
        }
    }
}
