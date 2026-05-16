package com.yingjian.feature.photobook

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yingjian.feature.photobook.model.BookState
import com.yingjian.feature.photobook.model.CoverLayout
import com.yingjian.feature.photobook.model.CoverPageType
import com.yingjian.feature.photobook.model.CoverTextRole
import com.yingjian.feature.photobook.model.PageTemplate
import com.yingjian.feature.photobook.model.PhotobookLayoutDefaults
import com.yingjian.feature.photobook.model.TypedEditorSelection
import com.yingjian.feature.photobook.model.moveText
import com.yingjian.feature.photobook.model.updateText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotobookEditorScreen(
    bookState: BookState,
    onUpdateState: (BookState) -> Unit,
    onUpdatePhotobook: (com.yingjian.core.data.database.PhotobookEntity) -> Unit,
    onContentPageSelected: (Int) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onExportPdf: () -> Unit,
    onNavigateToPreview: () -> Unit,
    onAddPhotos: () -> Unit,
    onSetCover: () -> Unit,
    onChangeTemplate: (PageTemplate) -> Unit = {},
    onMoveSelectedToPreviousPage: () -> Unit = {},
    onMoveSelectedToNextPage: () -> Unit = {},
    onMoveSelectedToNewPage: () -> Unit = {},
    onSwapSelectedWithSlot: (String) -> Unit = {},
    onFillSelectedSlot: () -> Unit = {},
    onDeleteSelectedSlotImage: () -> Unit = {},
    onResetSelectedSlotImage: () -> Unit = {},
    pageMoodText: String? = null,
    pageMemoryDate: Long? = null,
    snackbarHostState: androidx.compose.material3.SnackbarHostState? = null,
    isExportingPdf: Boolean = false,
    onTextAction: () -> Unit = {},
    onAddPage: () -> Unit = {},
    onSelectLeaf: (Int) -> Unit = {},
    onUpdateCoverLayout: (CoverLayout) -> Unit = {},
    onUpdateBackCoverLayout: (CoverLayout) -> Unit = {}
) {
    val leafCount = bookState.pages.size + 2
    val maxLeafIndex = (leafCount - 1).coerceAtLeast(0)
    var currentLeafIndex by remember(bookState.photobook.id) { mutableIntStateOf(0) }
    var showCoverSheet by remember { mutableStateOf(false) }
    var showLayoutSheet by remember { mutableStateOf(false) }
    var showMoveSheet by remember { mutableStateOf(false) }
    var editingTextPageType by remember { mutableStateOf<CoverPageType?>(null) }
    var editingTextId by remember { mutableStateOf<String?>(null) }
    var editingTextValue by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val isCoverLeaf = currentLeafIndex == 0
    val isBackCoverLeaf = currentLeafIndex == leafCount - 1

    LaunchedEffect(maxLeafIndex) {
        if (currentLeafIndex > maxLeafIndex) {
            currentLeafIndex = maxLeafIndex
        }
    }

    // Sync to externally-set currentPage (e.g. from onAddPage)
    LaunchedEffect(bookState.currentPage) {
        val expectedLeaf = (bookState.currentPage + 1).coerceIn(0, maxLeafIndex)
        if (currentLeafIndex != expectedLeaf) {
            currentLeafIndex = expectedLeaf
        }
    }

    fun selectLeaf(targetIndex: Int) {
        val nextLeafIndex = targetIndex.coerceIn(0, maxLeafIndex)
        currentLeafIndex = nextLeafIndex

        val contentPageIndex = nextLeafIndex - 1
        if (contentPageIndex in bookState.pages.indices) {
            onContentPageSelected(contentPageIndex)
        }
    }

    fun openCoverTextEditor(pageType: CoverPageType, layout: CoverLayout) {
        val selectedId = (bookState.selection as? TypedEditorSelection.CoverText)
            ?.takeIf { it.pageType == pageType }
            ?.textId
        val element = selectedId
            ?.let { id -> layout.textElements.firstOrNull { it.id == id && it.role != CoverTextRole.Divider } }
            ?: layout.textElements.firstOrNull { it.role == CoverTextRole.Title }
            ?: layout.textElements.firstOrNull { it.role != CoverTextRole.Divider }
            ?: return
        onUpdateState(bookState.copy(selection = TypedEditorSelection.CoverText(pageType, element.id)))
        editingTextPageType = pageType
        editingTextId = element.id
        editingTextValue = element.text
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState ?: SnackbarHostState()) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            isCoverLeaf -> "封面"
                            isBackCoverLeaf -> "封底"
                            else -> bookState.photobook.name
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (!isCoverLeaf && !isBackCoverLeaf) {
                        IconButton(onClick = { showCoverSheet = true }) {
                            Icon(Icons.Default.Style, contentDescription = "设为封面")
                        }
                    }
                    IconButton(onClick = onNavigateToPreview) {
                        Icon(Icons.Default.Visibility, contentDescription = "预览")
                    }
                    if (isExportingPdf) {
                        Box(
                            modifier = Modifier.padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        IconButton(onClick = onExportPdf) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "导出PDF")
                        }
                    }
                    IconButton(onClick = {
                        onSave()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("已保存")
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            // Floating toolbar at top center
            FloatingPhotobookToolbar(
                onLayoutClicked = {
                    if (!isCoverLeaf && !isBackCoverLeaf) showLayoutSheet = true
                },
                onTextClicked = {
                    when {
                        isCoverLeaf -> openCoverTextEditor(CoverPageType.Cover, bookState.coverLayout)
                        isBackCoverLeaf -> openCoverTextEditor(CoverPageType.BackCover, bookState.backCoverLayout)
                        else -> onTextAction()
                    }
                },
                onMoveClicked = {
                    if (!isCoverLeaf && !isBackCoverLeaf && bookState.selectedSlotId != null) showMoveSheet = true
                },
                onDeleteClicked = onDeleteSelectedSlotImage,
                onAddPageClicked = onAddPage,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )

            // Stage centered in workspace
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = currentLeafIndex,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally { it } + fadeIn())
                                .togetherWith(slideOutHorizontally { -it } + fadeOut())
                        } else {
                            (slideInHorizontally { -it } + fadeIn())
                                .togetherWith(slideOutHorizontally { it } + fadeOut())
                        }
                    }
                ) { leafIndex ->
                    when {
                        leafIndex == 0 -> {
                            PhotobookStage(
                                modifier = Modifier,
                                maxWidth = 300.dp,
                                backgroundColor = Color(PhotobookLayoutDefaults.COVER_PAGE_COLOR)
                            ) { scaleFactor ->
                                CoverPageRenderer(
                                    layout = bookState.coverLayout,
                                    scaleFactor = scaleFactor,
                                    selectedTextId = (bookState.selection as? TypedEditorSelection.CoverText)
                                        ?.takeIf { it.pageType == CoverPageType.Cover }
                                        ?.textId,
                                    onTextSelected = { textId ->
                                        onUpdateState(bookState.copy(selection = TypedEditorSelection.CoverText(CoverPageType.Cover, textId)))
                                    },
                                    onTextMoved = { textId, xMm, yMm ->
                                        onUpdateCoverLayout(bookState.coverLayout.moveText(textId, xMm, yMm))
                                    }
                                )
                            }
                        }
                        leafIndex == leafCount - 1 -> {
                            PhotobookStage(
                                modifier = Modifier,
                                maxWidth = 300.dp,
                                backgroundColor = Color(PhotobookLayoutDefaults.COVER_PAGE_COLOR)
                            ) { scaleFactor ->
                                CoverPageRenderer(
                                    layout = bookState.backCoverLayout,
                                    scaleFactor = scaleFactor,
                                    selectedTextId = (bookState.selection as? TypedEditorSelection.CoverText)
                                        ?.takeIf { it.pageType == CoverPageType.BackCover }
                                        ?.textId,
                                    onTextSelected = { textId ->
                                        onUpdateState(bookState.copy(selection = TypedEditorSelection.CoverText(CoverPageType.BackCover, textId)))
                                    },
                                    onTextMoved = { textId, xMm, yMm ->
                                        onUpdateBackCoverLayout(bookState.backCoverLayout.moveText(textId, xMm, yMm))
                                    }
                                )
                            }
                        }
                        else -> {
                            val cp = leafIndex - 1
                            if (cp in bookState.pages.indices) {
                                val currentPageState = bookState.pages[cp]
                                PhotobookStage(
                                    modifier = Modifier,
                                    maxWidth = 300.dp,
                                    backgroundColor = Color(PhotobookLayoutDefaults.CONTENT_PAGE_COLOR)
                                ) { scaleFactor ->
                                    ContentPageRenderer(
                                        pageState = currentPageState,
                                        scaleFactor = scaleFactor,
                                        selectedSlotId = bookState.selectedSlotId,
                                        onSlotSelected = { slotId ->
                                            val updatedPages = bookState.pages.toMutableList()
                                            updatedPages[cp] = currentPageState
                                            onUpdateState(bookState.copy(pages = updatedPages, selectedSlotId = slotId, currentPage = cp))
                                        },
                                        onEmptySlotAddClicked = { slotId ->
                                            val updatedPages = bookState.pages.toMutableList()
                                            updatedPages[cp] = currentPageState
                                            onUpdateState(bookState.copy(pages = updatedPages, selectedSlotId = slotId, currentPage = cp))
                                            onFillSelectedSlot()
                                        },
                                        onSlotImageAdjusted = { slotId, offsetXMm, offsetYMm, scale ->
                                            val updatedPages = bookState.pages.toMutableList()
                                            updatedPages[cp] = currentPageState.copy(
                                                slots = currentPageState.slots.map { slot ->
                                                    if (slot.slotId == slotId) {
                                                        slot.copy(cropOffsetX = offsetXMm, cropOffsetY = offsetYMm, cropScale = scale)
                                                    } else {
                                                        slot
                                                    }
                                                }
                                            )
                                            onUpdateState(bookState.copy(pages = updatedPages, selectedSlotId = slotId))
                                        },
                                        moodText = pageMoodText,
                                        memoryDate = pageMemoryDate
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Leaf navigation
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { selectLeaf(currentLeafIndex - 1) },
                    enabled = currentLeafIndex > 0
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上一页")
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    repeat(leafCount) { index ->
                        val dotType = when (index) {
                            0 -> LeafType.Cover
                            leafCount - 1 -> LeafType.BackCover
                            else -> LeafType.Content
                        }
                        Box(
                            modifier = Modifier
                                .size(
                                    width = if (index == currentLeafIndex) 16.dp else 6.dp,
                                    height = 6.dp
                                )
                                .background(
                                    if (index == currentLeafIndex) MaterialTheme.colorScheme.primary
                                    else when (dotType) {
                                        LeafType.Cover, LeafType.BackCover -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                        LeafType.Content -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    },
                                    CircleShape
                                )
                        )
                    }
                }

                IconButton(
                    onClick = { selectLeaf(currentLeafIndex + 1) },
                    enabled = currentLeafIndex < leafCount - 1
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "下一页")
                }
            }
        }
    }

    if (showCoverSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCoverSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "设为封面",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    "将当前页面的图片设为画册封面？",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showCoverSheet = false }) {
                        Text("取消")
                    }
                    TextButton(onClick = {
                        onSetCover()
                        showCoverSheet = false
                    }) {
                        Text("确认")
                    }
                }
            }
        }
    }

    if (showLayoutSheet) {
        val currentTemplate = bookState.pages.getOrNull(currentLeafIndex - 1)?.template
        ModalBottomSheet(
            onDismissRequest = { showLayoutSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PageTemplate.entries.forEach { template ->
                    PageTemplateIconButton(
                        template = template,
                        selected = template == currentTemplate,
                        onClick = {
                            onChangeTemplate(template)
                            showLayoutSheet = false
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.padding(8.dp))
        }
    }

    if (showMoveSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoveSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "移动图片",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Column(modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = {
                            onMoveSelectedToPreviousPage()
                            showMoveSheet = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("移到上一页") }
                    FilledTonalButton(
                        onClick = {
                            onMoveSelectedToNextPage()
                            showMoveSheet = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("移到下一页") }
                    FilledTonalButton(
                        onClick = {
                            onMoveSelectedToNewPage()
                            showMoveSheet = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("新建页面") }
                }
                Spacer(modifier = Modifier.padding(8.dp))
            }
        }
    }

    val activeEditingTextPageType = editingTextPageType
    val activeEditingTextId = editingTextId
    if (activeEditingTextPageType != null && activeEditingTextId != null) {
        ModalBottomSheet(
            onDismissRequest = {
                editingTextPageType = null
                editingTextId = null
                editingTextValue = ""
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    "编辑文字",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = editingTextValue,
                    onValueChange = { editingTextValue = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        editingTextPageType = null
                        editingTextId = null
                        editingTextValue = ""
                    }) {
                        Text("取消")
                    }
                    FilledTonalButton(onClick = {
                        when (activeEditingTextPageType) {
                            CoverPageType.Cover -> onUpdateCoverLayout(bookState.coverLayout.updateText(activeEditingTextId, editingTextValue))
                            CoverPageType.BackCover -> onUpdateBackCoverLayout(bookState.backCoverLayout.updateText(activeEditingTextId, editingTextValue))
                        }
                        editingTextPageType = null
                        editingTextId = null
                        editingTextValue = ""
                    }) {
                        Text("保存")
                    }
                }
                Spacer(modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
private fun PageTemplateIconButton(
    template: PageTemplate,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val backgroundColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        PageTemplateIcon(template = template, selected = selected)
    }
}

@Composable
private fun PageTemplateIcon(template: PageTemplate, selected: Boolean) {
    val pageColor = MaterialTheme.colorScheme.surfaceContainerLowest
    val pageBorderColor = MaterialTheme.colorScheme.outlineVariant
    val slotColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = Modifier.size(38.dp)) {
        val pageWidth = size.width * 0.9f
        val pageHeight = pageWidth * 210f / 285f
        val pageLeft = (size.width - pageWidth) / 2f
        val pageTop = (size.height - pageHeight) / 2f
        val pageCorner = 2.5.dp.toPx()

        drawRoundRect(
            color = pageColor,
            topLeft = Offset(pageLeft, pageTop),
            size = Size(pageWidth, pageHeight),
            cornerRadius = CornerRadius(pageCorner, pageCorner)
        )
        drawRoundRect(
            color = pageBorderColor,
            topLeft = Offset(pageLeft, pageTop),
            size = Size(pageWidth, pageHeight),
            cornerRadius = CornerRadius(pageCorner, pageCorner),
            style = Stroke(width = 1.dp.toPx())
        )

        fun drawSlot(leftRatio: Float, topRatio: Float, widthRatio: Float, heightRatio: Float) {
            drawRoundRect(
                color = slotColor,
                topLeft = Offset(
                    pageLeft + pageWidth * leftRatio,
                    pageTop + pageHeight * topRatio
                ),
                size = Size(pageWidth * widthRatio, pageHeight * heightRatio),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }

        when (template) {
            PageTemplate.SingleLandscape -> drawSlot(0.16f, 0.2f, 0.68f, 0.48f)
            PageTemplate.SinglePortrait -> drawSlot(0.39f, 0.08f, 0.22f, 0.78f)
            PageTemplate.TwoHorizontal -> {
                drawSlot(0.18f, 0.12f, 0.64f, 0.32f)
                drawSlot(0.18f, 0.56f, 0.64f, 0.32f)
            }
            PageTemplate.TwoVertical -> {
                drawSlot(0.14f, 0.16f, 0.32f, 0.64f)
                drawSlot(0.54f, 0.16f, 0.32f, 0.64f)
            }
            PageTemplate.GridFour -> {
                drawSlot(0.15f, 0.14f, 0.3f, 0.3f)
                drawSlot(0.55f, 0.14f, 0.3f, 0.3f)
                drawSlot(0.15f, 0.56f, 0.3f, 0.3f)
                drawSlot(0.55f, 0.56f, 0.3f, 0.3f)
            }
        }
    }
}

private enum class LeafType { Cover, Content, BackCover }
