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
import androidx.compose.runtime.produceState
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
import com.yingjian.feature.memories.MemoryDetailScreen
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
import com.yingjian.core.data.database.MemoryRecordEntity
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
    var refreshTrigger by remember { mutableStateOf(0) }
    var memoryRefreshTrigger by remember { mutableStateOf(0) }
    var pendingPublish by remember { mutableStateOf<MemoryRecordEntity?>(null) }
    val navHostScope = rememberCoroutineScope()

    // Handle publish via LaunchedEffect (not runBlocking)
    LaunchedEffect(pendingPublish) {
        pendingPublish?.let { entity ->
            withContext(Dispatchers.IO) {
                deps.memoryRepository.insertMemory(entity)
            }
            refreshTrigger++
            pendingPublish = null
            navController.popBackStack()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(NavDestinations.Memories.route) {
            val factory = MemoriesViewModel.factory(deps.memoryRepository)
            val viewModel: MemoriesViewModel = viewModel(factory = factory)

            LaunchedEffect(refreshTrigger) {
                viewModel.dispatch(com.yingjian.feature.memories.MemoriesAction.Load)
            }

            MemoriesScreen(
                viewModel = viewModel,
                onNavigateToNewPost = { uris: List<Uri>, dates: List<Long> ->
                    val urisJson = Json.encodeToString(
                        ListSerializer(String.serializer()),
                        uris.map { it.toString() }
                    )
                    val datesJson = Json.encodeToString(
                        ListSerializer(Long.serializer()),
                        dates
                    )
                    navController.currentBackStackEntry?.savedStateHandle?.apply {
                        set("newPostUris", urisJson)
                        set("newPostDates", datesJson)
                    }
                    navController.navigate(NavDestinations.NewPost.route)
                },
                onMemoryClick = { memory ->
                    navController.navigate(NavDestinations.MemoryDetail.createRoute(memory.id))
                }
            )
        }
        composable(NavDestinations.Photobook.route) { backStackEntry ->
            val factory = PhotobookViewModel.factory(
                deps.photobookRepository,
                deps.memoryRepository
            )
            val viewModel: PhotobookViewModel = viewModel(factory = factory)

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
            val (uris, imageUrisJson, dates) = remember {
                val urisJsonStr = navController.previousBackStackEntry?.savedStateHandle?.get<String>("newPostUris")
                val datesJsonStr = navController.previousBackStackEntry?.savedStateHandle?.get<String>("newPostDates")

                val uriStrings = urisJsonStr?.let {
                    runCatching { Json.decodeFromString<List<String>>(it) }.getOrNull()
                } ?: emptyList()
                val datesList = datesJsonStr?.let {
                    runCatching { Json.decodeFromString<List<Long>>(it) }.getOrNull()
                } ?: emptyList()
                val parsedUris = uriStrings.map { Uri.parse(it) }

                navController.previousBackStackEntry?.savedStateHandle?.remove<String>("newPostUris")
                navController.previousBackStackEntry?.savedStateHandle?.remove<String>("newPostDates")

                Triple(
                    parsedUris,
                    urisJsonStr ?: "[]",
                    datesList
                )
            }

            if (uris.isNotEmpty() && dates.isNotEmpty()) {
                val firstMetadata = getImageMetadata(LocalContext.current, uris.first())

                NewPostScreen(
                    imageUris = uris,
                    datesTaken = dates,
                    onPublish = { mood, tags ->
                        pendingPublish = MemoryRecordEntity(
                            imageUri = uris.first().toString(),
                            imageWidth = firstMetadata.first,
                            imageHeight = firstMetadata.second,
                            timestamp = dates.first(),
                            latitude = null,
                            longitude = null,
                            moodText = mood.takeIf { it.isNotBlank() },
                            tags = Json.encodeToString(ListSerializer(String.serializer()), tags),
                            createdAt = System.currentTimeMillis(),
                            imageUrisJson = imageUrisJson
                        )
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable(NavDestinations.MemoryDetail.route) { backStackEntry ->
            val memoryId = backStackEntry.arguments?.getString("memoryId")?.toLongOrNull()
            if (memoryId != null) {
                val memory by produceState<MemoryRecordEntity?>(
                    initialValue = null,
                    memoryId, memoryRefreshTrigger
                ) {
                    value = withContext(Dispatchers.IO) {
                        deps.memoryRepository.getMemoryById(memoryId)
                    }
                }

                memory?.let { memoryEntity ->
                    MemoryDetailScreen(
                        memory = memoryEntity,
                        onBack = { navController.popBackStack() },
                        onUpdate = { updatedMemory ->
                            navHostScope.launch {
                                withContext(Dispatchers.IO) {
                                    deps.memoryRepository.updateMemory(updatedMemory)
                                }
                                memoryRefreshTrigger++
                            }
                            refreshTrigger++
                        },
                        onDelete = {
                            navHostScope.launch {
                                withContext(Dispatchers.IO) {
                                    deps.memoryRepository.deleteMemory(memoryEntity)
                                }
                            }
                            navController.popBackStack()
                            refreshTrigger++
                        }
                    )
                } ?: run {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
        }
        composable(NavDestinations.PhotoPicker.route) { backStackEntry ->
            val allMemories = rememberLoadedMemories(deps.memoryRepository)

            PhotoPickerScreen(
                memories = allMemories,
                onBack = { navController.popBackStack() },
                onComplete = { selectedIds ->
                    val paperSize = backStackEntry.savedStateHandle.get<String>("paperSize")

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
