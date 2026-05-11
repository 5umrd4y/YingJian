package com.yingjian

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yingjian.core.ui.common.YingJianBottomNavigationBar
import com.yingjian.core.ui.navigation.YingJianNavHost
import com.yingjian.core.ui.theme.YingJianTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val deps = (application as YingJianApplication).deps

        setContent {
            YingJianTheme {
                val navController = rememberNavController()
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
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
                ) { innerPadding ->
                    YingJianNavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        deps = deps
                    )
                }
            }
        }
    }
}
