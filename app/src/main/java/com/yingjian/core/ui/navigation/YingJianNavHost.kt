package com.yingjian.core.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
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
import com.yingjian.feature.photobook.PhotoPickerScreen
import com.yingjian.feature.photobook.PhotobookScreen
import com.yingjian.feature.photobook.PhotobookViewModel
import com.yingjian.feature.photobook.model.PaperSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        composable(NavDestinations.Photobook.route) { backStackEntry ->
            val factory = PhotobookViewModel.factory(
                deps.photobookRepository,
                deps.memoryRepository
            )
            val viewModel: PhotobookViewModel = viewModel(factory = factory)

            // Check if we received selected memory IDs from PhotoPicker
            val selectedIdsStr = backStackEntry.savedStateHandle.get<String>("selectedMemoryIds")
            val paperSizeStr = backStackEntry.savedStateHandle.get<String>("paperSize")

            if (selectedIdsStr != null && paperSizeStr != null) {
                val paperSize = runCatching { PaperSize.valueOf(paperSizeStr) }
                    .getOrNull() ?: PaperSize.A4
                val ids = selectedIdsStr.split(",").mapNotNull { it.toLongOrNull() }

                LaunchedEffect(Unit) {
                    viewModel.createPhotobook(
                        name = "未命名画册",
                        paperSize = paperSize,
                        selectedMemoryIds = ids
                    )
                    backStackEntry.savedStateHandle.remove<String>("selectedMemoryIds")
                    backStackEntry.savedStateHandle.remove<String>("paperSize")
                }
            }

            PhotobookScreen(
                viewModel = viewModel,
                onNavigateToPhotoPicker = { paperSize ->
                    // Store paperSize in savedStateHandle for PhotoPicker to read
                    backStackEntry.savedStateHandle.set("paperSize", paperSize.name)
                    navController.navigate(NavDestinations.PhotoPicker.route)
                }
            )
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
        composable(NavDestinations.PhotoPicker.route) { backStackEntry ->
            val allMemories = rememberLoadedMemories(deps.memoryRepository)

            PhotoPickerScreen(
                memories = allMemories,
                onBack = { navController.popBackStack() },
                onComplete = { selectedIds ->
                    // Read paperSize from PhotoPicker's savedStateHandle (set by Photobook nav)
                    val paperSize = backStackEntry.savedStateHandle.get<String>("paperSize")

                    // Pass selected IDs + paperSize back to Photobook via savedStateHandle
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        set("selectedMemoryIds", selectedIds.map { it.toString() }.joinToString(","))
                        paperSize?.let { set("paperSize", it) }
                    }
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Helper composable to load all memories from the repository.
 */
@Composable
private fun rememberLoadedMemories(
    repository: com.yingjian.core.data.repository.MemoryRepository
): List<com.yingjian.core.data.database.MemoryRecordEntity> {
    var memories by mutableStateOf(
        emptyList<com.yingjian.core.data.database.MemoryRecordEntity>()
    )

    LaunchedEffect(repository) {
        withContext(Dispatchers.IO) {
            memories = repository.getAllMemories()
        }
    }

    return memories
}
