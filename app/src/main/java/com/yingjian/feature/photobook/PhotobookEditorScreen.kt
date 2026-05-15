package com.yingjian.feature.photobook

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.yingjian.feature.photobook.model.BookState
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
    onDeleteImage: () -> Unit,
    onResetImage: () -> Unit,
    pageMoodText: String? = null,
    pageMemoryDate: Long? = null
) {
    val leafCount = bookState.pages.size + 2
    val maxLeafIndex = (leafCount - 1).coerceAtLeast(0)
    var currentLeafIndex by remember(bookState.photobook.id) { mutableIntStateOf(0) }
    var isImageSelected by remember { mutableStateOf(false) }
    var showCoverSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val isCoverLeaf = currentLeafIndex == 0
    val isBackCoverLeaf = currentLeafIndex == leafCount - 1

    LaunchedEffect(maxLeafIndex) {
        if (currentLeafIndex > maxLeafIndex) {
            currentLeafIndex = maxLeafIndex
        }
    }

    fun selectLeaf(targetIndex: Int) {
        val nextLeafIndex = targetIndex.coerceIn(0, maxLeafIndex)
        currentLeafIndex = nextLeafIndex
        isImageSelected = false

        val contentPageIndex = nextLeafIndex - 1
        if (contentPageIndex in bookState.pages.indices) {
            onContentPageSelected(contentPageIndex)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    // Set cover: only enabled on content pages with an image
                    if (!isCoverLeaf && !isBackCoverLeaf) {
                        IconButton(onClick = { showCoverSheet = true }) {
                            Icon(Icons.Default.Style, contentDescription = "设为封面")
                        }
                    }
                    IconButton(onClick = onNavigateToPreview) {
                        Icon(Icons.Default.Visibility, contentDescription = "预览")
                    }
                    IconButton(onClick = onExportPdf) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "导出PDF")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Animated leaf content
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
                },
                modifier = Modifier.weight(1f)
            ) { leafIndex ->
                when {
                    leafIndex == 0 -> {
                        // Cover page
                        CoverEditorPage(
                            photobook = bookState.photobook,
                            onUpdatePhotobook = onUpdatePhotobook,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        )
                    }
                    leafIndex == leafCount - 1 -> {
                        // Back cover page
                        BackCoverEditorPage(
                            photobook = bookState.photobook,
                            onUpdatePhotobook = onUpdatePhotobook,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        )
                    }
                    else -> {
                        // Content page
                        val cp = leafIndex - 1
                        if (cp in bookState.pages.indices) {
                            val currentPageState = bookState.pages[cp]
                            PhotobookCanvasPage(
                                pageState = currentPageState,
                                containerWidthDp = 300.dp,
                                moodText = pageMoodText,
                                memoryDate = pageMemoryDate,
                                selectedSlotId = bookState.selectedSlotId,
                                onSlotSelected = { slotId ->
                                    val updatedPages = bookState.pages.toMutableList()
                                    updatedPages[cp] = currentPageState
                                    onUpdateState(bookState.copy(pages = updatedPages, selectedSlotId = slotId, currentPage = cp))
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
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            )
                        }
                    }
                }
            }

            // Leaf navigation
            Row(
                modifier = Modifier
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

                // Leaf indicator dots
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

            // Bottom toolbar — content pages only
            if (!isCoverLeaf && !isBackCoverLeaf && bookState.pages.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isImageSelected) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            FilledTonalButton(onClick = {
                                onDeleteImage()
                                isImageSelected = false
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("删除")
                            }
                            OutlinedButton(onClick = {
                                onResetImage()
                                isImageSelected = false
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("重置")
                            }
                        }
                    } else {
                        FilledTonalButton(onClick = onAddPhotos) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("添加照片")
                        }
                    }
                }
            }
        }
    }

    // Cover photo bottom sheet
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
}

private enum class LeafType { Cover, Content, BackCover }
