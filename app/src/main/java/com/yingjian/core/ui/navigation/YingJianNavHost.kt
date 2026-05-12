package com.yingjian.core.ui.navigation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.yingjian.feature.memories.getImageMetadata
import com.yingjian.feature.photobook.PhotoPickerScreen
import com.yingjian.feature.photobook.PhotobookEditorScreen
import com.yingjian.feature.photobook.PhotobookScreen
import com.yingjian.feature.photobook.PhotobookViewModel
import com.yingjian.feature.photobook.export.PdfExportUtil
import com.yingjian.feature.photobook.model.BookState
import com.yingjian.feature.photobook.model.LayoutMode
import com.yingjian.feature.photobook.model.PageState
import com.yingjian.feature.photobook.model.PaperSize
import com.yingjian.feature.settings.SettingsScreen
import com.yingjian.core.data.database.PageLayoutEntity
import com.yingjian.core.util.ElementSerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

@Composable
fun YingJianNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = NavDestinations.Memories.route,
    deps: AppDependencies
) {
    // Shared trigger to refresh Memories list after a new post is published
    var refreshTrigger by remember { mutableStateOf(0) }
    val navHostScope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(NavDestinations.Memories.route) {
            val factory = MemoriesViewModel.factory(deps.memoryRepository)
            val viewModel: MemoriesViewModel = viewModel(factory = factory)

            // Refresh memories when triggered by new post publish
            LaunchedEffect(refreshTrigger) {
                viewModel.dispatch(com.yingjian.feature.memories.MemoriesAction.Load)
            }

            MemoriesScreen(
                viewModel = viewModel,
                onNavigateToNewPost = { uri ->
                    navController.navigate(NavDestinations.NewPost.createRoute(uri))
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
                },
                onNavigateToEditor = { photobookId ->
                    navController.navigate(NavDestinations.PhotobookEditor.createRoute(photobookId))
                }
            )
        }
        composable(NavDestinations.Settings.route) {
            SettingsScreen(
                localStorageProvider = deps.localStorageProvider
            )
        }
        composable(NavDestinations.NewPost.route) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("encodedUri")
            val uri = encodedUri?.let { Uri.decode(it) }?.let { Uri.parse(it) }
            if (uri != null) {
                val context = LocalContext.current
                NewPostScreen(
                    imageUri = uri,
                    onPublish = { mood, tags ->
                        val metadata = getImageMetadata(context, uri)
                        // Insert directly via repository
                        kotlinx.coroutines.runBlocking {
                            withContext(Dispatchers.IO) {
                                deps.memoryRepository.insertMemory(
                                    com.yingjian.core.data.database.MemoryRecordEntity(
                                        imageUri = uri.toString(),
                                        imageWidth = metadata.first,
                                        imageHeight = metadata.second,
                                        timestamp = metadata.third,
                                        latitude = null,
                                        longitude = null,
                                        moodText = mood.takeIf { it.isNotBlank() },
                                        tags = Json.encodeToString(ListSerializer(String.serializer()), tags),
                                        createdAt = System.currentTimeMillis()
                                    )
                                )
                            }
                        }
                        refreshTrigger++
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
        composable("photobook_editor/{photobookId}") { backStackEntry ->
            val factory = PhotobookViewModel.factory(
                deps.photobookRepository,
                deps.memoryRepository
            )
            val viewModel: PhotobookViewModel = viewModel(factory = factory)
            val photobookId = backStackEntry.arguments?.getString("photobookId")?.toLongOrNull()

            val context = LocalContext.current

            // Load BookState from repository
            var loadedBookState by remember { mutableStateOf<BookState?>(null) }

            LaunchedEffect(photobookId) {
                if (photobookId != null) {
                    val photobook = withContext(Dispatchers.IO) {
                        deps.photobookRepository.getPhotobookById(photobookId)
                    }
                    if (photobook != null) {
                        val pageLayouts = withContext(Dispatchers.IO) {
                            deps.photobookRepository.getPageLayouts(photobookId)
                        }
                        val pages = pageLayouts.map { layout ->
                            val elements = try {
                                ElementSerializer.deserialize(layout.elementsJson)
                            } catch (e: Exception) {
                                emptyList()
                            }
                            PageState(
                                pageNumber = layout.pageNumber,
                                elements = elements,
                                trimWidthMm = PaperSize.valueOf(photobook.paperSize).widthMm,
                                trimHeightMm = PaperSize.valueOf(photobook.paperSize).heightMm
                            )
                        }
                        loadedBookState = BookState(
                            photobook = photobook,
                            pages = pages,
                            currentPage = 0,
                            mode = LayoutMode.MANUAL
                        )
                    }
                }
            }

            // PDF export launcher
            val pdfBytesState = remember { mutableStateOf<ByteArray?>(null) }
            val createPdf = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/pdf")
            ) { uri ->
                uri?.let {
                    val bytes = pdfBytesState.value ?: return@let
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(bytes)
                    }
                }
            }
            val coroutineScope = rememberCoroutineScope()

            loadedBookState?.let { bookState ->
                PhotobookEditorScreen(
                    bookState = bookState,
                    onUpdateState = { loadedBookState = it },
                    onBack = { navController.popBackStack() },
                    onSave = {
                        val currentState = loadedBookState
                        if (currentState != null) {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    currentState.pages.forEach { page ->
                                        deps.photobookRepository.savePageLayout(
                                            PageLayoutEntity(
                                                photobookId = currentState.photobook.id,
                                                pageNumber = page.pageNumber,
                                                elementsJson = ElementSerializer.serialize(page.elements),
                                                mode = currentState.mode.name
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    },
                    onExportPdf = {
                        pdfBytesState.value = PdfExportUtil.exportPdf(context, bookState)
                        createPdf.launch("photobook.pdf")
                    },
                    onUndo = { /* MVP: no-op */ }
                )
            } ?: run {
                androidx.compose.material3.Text("Loading...")
            }
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
