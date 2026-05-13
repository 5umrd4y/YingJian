package com.yingjian.core.ui.navigation

import android.net.Uri
import android.content.Intent
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
import androidx.compose.runtime.key
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
import com.yingjian.feature.memories.getAllImageUris
import com.yingjian.feature.photobook.PhotoPickerScreen
import com.yingjian.feature.photobook.PhotobookEditorScreen
import com.yingjian.feature.photobook.PhotobookPreviewScreen
import com.yingjian.feature.photobook.PhotobookScreen
import com.yingjian.feature.photobook.PhotobookViewModel
import com.yingjian.feature.photobook.export.PdfExportUtil
import com.yingjian.feature.photobook.layout.AutoLayoutAlgorithm
import com.yingjian.feature.photobook.model.BookState
import com.yingjian.feature.photobook.model.ImageElement
import com.yingjian.feature.photobook.model.LayoutMode
import com.yingjian.feature.photobook.model.PageState
import com.yingjian.feature.photobook.model.PaperSize
import com.yingjian.feature.settings.SettingsScreen
import com.yingjian.feature.settings.AboutScreen
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

            if (selectedIdsStr != null) {
                val ids = selectedIdsStr.split(",").mapNotNull { it.toLongOrNull() }
                val photobookName = backStackEntry.savedStateHandle.get<String>("newPhotobookName") ?: "未命名画册"

                LaunchedEffect(selectedIdsStr) {
                    viewModel.createPhotobook(
                        name = photobookName,
                        selectedMemoryIds = ids
                    )
                    backStackEntry.savedStateHandle.remove<String>("selectedMemoryIds")
                    backStackEntry.savedStateHandle.remove<String>("newPhotobookName")
                    backStackEntry.savedStateHandle.set("createdBook", "true")
                }
            }

            // Auto-navigate to editor after creation completes
            val createdBook = viewModel.uiState.currentBookState
            val shouldNavigate = backStackEntry.savedStateHandle.get<String>("createdBook")
            LaunchedEffect(createdBook, shouldNavigate) {
                if (createdBook != null && shouldNavigate == "true") {
                    backStackEntry.savedStateHandle.remove<String>("createdBook")
                    navController.navigate(
                        NavDestinations.PhotobookEditor.createRoute(createdBook.photobook.id)
                    )
                }
            }

            PhotobookScreen(
                viewModel = viewModel,
                onNavigateToPhotoPicker = { name ->
                    backStackEntry.savedStateHandle.set("newPhotobookName", name)
                    navController.navigate(NavDestinations.PhotoPicker.route)
                },
                onNavigateToEditor = { photobookId ->
                    navController.navigate(NavDestinations.PhotobookEditor.createRoute(photobookId))
                }
            )
        }
        composable(NavDestinations.Settings.route) {
            SettingsScreen(
                localStorageProvider = deps.localStorageProvider,
                onNavigateToAbout = { navController.navigate(NavDestinations.About.route) }
            )
        }
        composable(NavDestinations.About.route) {
            AboutScreen(
                onBack = { navController.popBackStack() }
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

                val context = LocalContext.current

                // Photo picker for adding photos from detail screen
                val pickMultipleLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(9)
                ) { pickedUris: List<Uri> ->
                    if (pickedUris.isNotEmpty()) {
                        pickedUris.forEach { uri ->
                            runCatching {
                                context.contentResolver.takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                                )
                            }
                        }
                        val existing = memory?.let { m -> m.getAllImageUris().map { Uri.parse(it) } } ?: emptyList()
                        val allUris = existing + pickedUris
                        val imageUrisJson = Json.encodeToString(
                            ListSerializer(String.serializer()),
                            allUris.map { it.toString() }
                        )
                        navHostScope.launch {
                            withContext(Dispatchers.IO) {
                                memory?.let { m ->
                                    deps.memoryRepository.updateMemory(
                                        m.copy(
                                            imageUrisJson = imageUrisJson,
                                            imageUri = allUris.first().toString()
                                        )
                                    )
                                }
                            }
                            memoryRefreshTrigger++
                            refreshTrigger++
                        }
                    }
                }

                val pickSingleLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
                ) { uri: Uri? ->
                    uri?.let { pickedUri ->
                        runCatching {
                            context.contentResolver.takePersistableUriPermission(
                                pickedUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }
                        val existing = memory?.let { m -> m.getAllImageUris().map { Uri.parse(it) } } ?: emptyList()
                        val allUris = existing + pickedUri
                        val imageUrisJson = Json.encodeToString(
                            ListSerializer(String.serializer()),
                            allUris.map { it.toString() }
                        )
                        navHostScope.launch {
                            withContext(Dispatchers.IO) {
                                memory?.let { m ->
                                    deps.memoryRepository.updateMemory(
                                        m.copy(
                                            imageUrisJson = imageUrisJson,
                                            imageUri = allUris.first().toString()
                                        )
                                    )
                                }
                            }
                            memoryRefreshTrigger++
                            refreshTrigger++
                        }
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
                        },
                        onAddPhotos = { _ ->
                            val currentCount = memoryEntity.getAllImageUris().size
                            val maxPick = 9 - currentCount
                            if (maxPick > 0) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    pickMultipleLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                } else {
                                    pickSingleLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                            }
                        }
                    )
                } ?: run {
                    androidx.compose.material3.CircularProgressIndicator()
                }
            }
        }
        composable(NavDestinations.PhotoPicker.route) { backStackEntry ->
            val allMemories = rememberLoadedMemories(deps.memoryRepository)
            val isAppendMode = backStackEntry.savedStateHandle.get<String>("appendMode") == "true"

            PhotoPickerScreen(
                memories = allMemories,
                onBack = { navController.popBackStack() },
                onComplete = { selectedIds ->
                    val idsJson = selectedIds.map { it.toString() }.joinToString(",")
                    navController.previousBackStackEntry?.savedStateHandle?.apply {
                        if (isAppendMode) {
                            set("appendMemoryIds", idsJson)
                        } else {
                            set("selectedMemoryIds", idsJson)
                        }
                    }
                    backStackEntry.savedStateHandle.remove<String>("appendMode")
                    backStackEntry.savedStateHandle.remove<String>("editorPhotobookId")
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

            // Extract currentPage outside let so key() can wrap the mood lookup properly
            val theBookState = loadedBookState
            val currentPage = theBookState?.currentPage ?: 0

            if (theBookState != null) {
                // Load mood/date from memory for current page — keyed to page index
                // to force recreation when switching pages (AnimatedContent preserves compositions)
                key(currentPage) {
                    val currentPageState = theBookState.pages.getOrNull(currentPage)
                    val currentImageElement = currentPageState?.elements?.filterIsInstance<ImageElement>()?.firstOrNull()
                    val currentMemory by produceState<MemoryRecordEntity?>(
                        initialValue = null,
                        currentImageElement?.memoryId
                    ) {
                        value = currentImageElement?.memoryId?.let { memoryId ->
                            withContext(Dispatchers.IO) { deps.memoryRepository.getMemoryById(memoryId) }
                        }
                    }

                    // Handle append mode: when returning from PhotoPicker with new photos
                    val appendIdsStr = backStackEntry.savedStateHandle.get<String>("appendMemoryIds")
                    LaunchedEffect(appendIdsStr) {
                        if (appendIdsStr != null) {
                            val ids = appendIdsStr.split(",").mapNotNull { it.toLongOrNull() }
                            if (ids.isNotEmpty()) {
                                viewModel.appendPhotosToBook(ids)
                                val vmState = viewModel.uiState.currentBookState
                                if (vmState != null && vmState.photobook.id == photobookId) {
                                    loadedBookState = vmState
                                }
                            }
                            backStackEntry.savedStateHandle.remove<String>("appendMemoryIds")
                        }
                    }

                    // Sync loadedBookState when ViewModel's state changes
                    val vmCurrentState = viewModel.uiState.currentBookState
                    LaunchedEffect(vmCurrentState?.pages?.size, vmCurrentState?.photobook?.id) {
                        if (vmCurrentState != null && vmCurrentState.photobook.id == photobookId
                            && vmCurrentState.pages.size != loadedBookState?.pages?.size) {
                            loadedBookState = vmCurrentState
                        }
                    }

                    PhotobookEditorScreen(
                        bookState = theBookState,
                    onUpdateState = { loadedBookState = it },
                    onBack = { navController.popBackStack() },
                    onSave = {
                        val currentState = loadedBookState
                        if (currentState != null) {
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    deps.photobookRepository.deletePageLayouts(currentState.photobook.id)
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
                                    deps.photobookRepository.updatePhotobook(currentState.photobook)
                                }
                            }
                        }
                    },
                    onExportPdf = {
                        pdfBytesState.value = PdfExportUtil.exportPdf(context, theBookState)
                        createPdf.launch("photobook.pdf")
                    },
                    onNavigateToPreview = {
                        navController.navigate(NavDestinations.Preview.createRoute(theBookState.photobook.id))
                    },
                    onAddPhotos = {
                        navController.currentBackStackEntry?.savedStateHandle?.set("appendMode", "true")
                        navController.currentBackStackEntry?.savedStateHandle?.set("editorPhotobookId", theBookState.photobook.id.toString())
                        navController.navigate(NavDestinations.PhotoPicker.route)
                    },
                    onSetCover = {
                        val currentPage = loadedBookState?.currentPage ?: 0
                        val imageElement = loadedBookState?.pages?.getOrNull(currentPage)
                            ?.elements?.filterIsInstance<ImageElement>()?.firstOrNull()
                        imageElement?.let {
                            val updatedPhotobook = loadedBookState!!.photobook.copy(
                                coverImageUri = it.imageUri,
                                updatedAt = System.currentTimeMillis()
                            )
                            loadedBookState = loadedBookState?.copy(photobook = updatedPhotobook)
                            // Persist cover to DB immediately
                            coroutineScope.launch {
                                withContext(Dispatchers.IO) {
                                    deps.photobookRepository.updatePhotobook(updatedPhotobook)
                                }
                            }
                        }
                    },
                    onDeleteImage = {
                        loadedBookState?.let { state ->
                            val cp = state.currentPage
                            val page = state.pages.getOrNull(cp) ?: return@let
                            val updatedElements = page.elements.filterNot { it is ImageElement }
                            val updatedPages = state.pages.toMutableList()
                            if (updatedElements.isEmpty()) {
                                updatedPages.removeAt(cp)
                                val renumbered = updatedPages.mapIndexed { idx, p ->
                                    p.copy(pageNumber = idx + 1)
                                }
                                loadedBookState = state.copy(
                                    pages = renumbered,
                                    currentPage = cp.coerceAtMost((renumbered.size - 1).coerceAtLeast(0))
                                )
                            } else {
                                updatedPages[cp] = page.copy(elements = updatedElements)
                                loadedBookState = state.copy(pages = updatedPages)
                            }
                        }
                    },
                    onResetImage = {
                        loadedBookState?.let { state ->
                            val cp = state.currentPage
                            val page = state.pages.getOrNull(cp) ?: return@let
                            val imageEl = page.elements.filterIsInstance<ImageElement>().firstOrNull() ?: return@let
                            coroutineScope.launch {
                                val memory = withContext(Dispatchers.IO) {
                                    deps.memoryRepository.getMemoryById(imageEl.memoryId)
                                }
                                if (memory != null) {
                                    val paperSize = runCatching {
                                        PaperSize.valueOf(state.photobook.paperSize)
                                    }.getOrDefault(PaperSize.TWELVE_INCH_LANDSCAPE)
                                    val resetPage = AutoLayoutAlgorithm.createSinglePhotoPage(
                                        memory = memory,
                                        paperSize = paperSize,
                                        pageNumber = page.pageNumber,
                                        imageUri = imageEl.imageUri
                                    )
                                    val updatedPages = state.pages.toMutableList()
                                    updatedPages[cp] = resetPage
                                    loadedBookState = state.copy(pages = updatedPages)
                                }
                            }
                        }
                    },
                    pageMoodText = currentMemory?.moodText,
                    pageMemoryDate = currentMemory?.timestamp
                )
            } // end key(currentPage)
        } else {
            androidx.compose.material3.Text("Loading...")
        }
        }
        composable(NavDestinations.Preview.route) { backStackEntry ->
            val photobookId = backStackEntry.arguments?.getString("photobookId")?.toLongOrNull()

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

            val context = LocalContext.current

            loadedBookState?.let { state ->
                PhotobookPreviewScreen(
                    bookState = state,
                    onBack = { navController.popBackStack() },
                    onShare = {
                        val bytes = PdfExportUtil.exportPdf(context, state)
                        val cacheDir = context.cacheDir
                        val shareFile = java.io.File(cacheDir, "photobook_${state.photobook.id}.pdf")
                        shareFile.writeBytes(bytes)
                        val uri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            shareFile
                        )
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "分享画册"))
                    }
                )
            } ?: run {
                androidx.compose.material3.CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun rememberLoadedMemories(
    repository: com.yingjian.core.data.repository.MemoryRepository
): List<com.yingjian.core.data.database.MemoryRecordEntity> {
    val memories by produceState<List<com.yingjian.core.data.database.MemoryRecordEntity>>(
        initialValue = emptyList(),
        key1 = repository
    ) {
        value = withContext(Dispatchers.IO) {
            repository.getAllMemories()
        }
    }

    return memories
}
