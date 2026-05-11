package com.yingjian.core.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.yingjian.AppDependencies
import com.yingjian.feature.memories.MemoriesScreen
import com.yingjian.feature.memories.MemoriesViewModel
import com.yingjian.feature.memories.NewPostScreen
import com.yingjian.feature.memories.getImageDimensions

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
            val factory = MemoriesViewModel.factory(deps.memoryRepository)
            val viewModel: MemoriesViewModel = viewModel(factory = factory)
            MemoriesScreen(
                viewModel = viewModel,
                onNavigateToNewPost = { uri ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("imageUri", uri.toString())
                    navController.navigate(NavDestinations.NewPost.route)
                }
            )
        }
        composable(NavDestinations.Photobook.route) {
            // Placeholder - will be implemented in Task 7
            androidx.compose.material3.Text("画册")
        }
        composable(NavDestinations.Settings.route) {
            // Placeholder - will be implemented in Task 11
            androidx.compose.material3.Text("设置")
        }
        composable(NavDestinations.NewPost.route) { backStackEntry ->
            val uri = backStackEntry.savedStateHandle.get<String>("imageUri")?.let { Uri.parse(it) }
            if (uri != null) {
                val factory = MemoriesViewModel.factory(deps.memoryRepository)
                val viewModel: MemoriesViewModel = viewModel(factory = factory)
                val context = LocalContext.current
                NewPostScreen(
                    imageUri = uri,
                    onPublish = { mood, tags ->
                        val dims = getImageDimensions(context, uri)
                        viewModel.dispatch(
                            com.yingjian.feature.memories.MemoriesAction.Add(
                                uri, mood, tags, dims.first, dims.second
                            )
                        )
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable(NavDestinations.PhotoPicker.route) {
            // Placeholder - will be properly integrated when PhotobookViewModel is wired
            androidx.compose.material3.Text("照片选择 (开发中)")
        }
    }
}
