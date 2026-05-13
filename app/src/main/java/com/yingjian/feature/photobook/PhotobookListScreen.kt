package com.yingjian.feature.photobook

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yingjian.core.data.database.PhotobookEntity
import com.yingjian.core.ui.theme.PaperTexture
import com.yingjian.core.ui.theme.Primary
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhotobookListScreen(
    viewModel: PhotobookViewModel,
    onNavigateToPhotoPicker: (String) -> Unit = {},
    onNavigateToEditor: (Long) -> Unit = {}
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (viewModel.uiState.isSelectionMode) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消")
                        }
                    },
                    title = {
                        Text("已选择 ${viewModel.uiState.selectedIds.size} 册")
                    },
                    actions = {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            enabled = viewModel.uiState.selectedIds.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = if (viewModel.uiState.selectedIds.isNotEmpty())
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            "画册",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    actions = {
                        IconButton(onClick = { /* MVP: placeholder */ }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Section header
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        "所有画册",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "共 ${viewModel.uiState.photobooks.size} 册",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    )
                }
            }

            // "新建画册" card — first item (hidden in selection mode)
            if (!viewModel.uiState.isSelectionMode) {
                item {
                    CreateNewAlbumCard(onClick = { showCreateDialog = true })
                }
            }

            // Photobook cover cards
            items(viewModel.uiState.photobooks) { book ->
                val isSelected = book.id in viewModel.uiState.selectedIds
                PhotobookCoverCard(
                    photobook = book,
                    isSelected = isSelected,
                    pageCount = viewModel.uiState.pageCounts[book.id] ?: 0,
                    onClick = {
                        if (viewModel.uiState.isSelectionMode) {
                            viewModel.toggleSelection(book.id)
                        } else {
                            onNavigateToEditor(book.id)
                        }
                    },
                    onLongPress = {
                        if (!viewModel.uiState.isSelectionMode) {
                            viewModel.enterSelectionMode(book.id)
                        }
                    }
                )
            }

            // "到底了" end marker
            if (viewModel.uiState.photobooks.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        "到底了",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确定删除 ${viewModel.uiState.selectedIds.size} 本画册？") },
            text = { Text("此操作不可撤销") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePhotobooks(viewModel.uiState.selectedIds.toList())
                    showDeleteConfirm = false
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (showCreateDialog) {
        CreatePhotobookBottomSheet(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                showCreateDialog = false
                onNavigateToPhotoPicker(name.takeIf { it.isNotBlank() } ?: "未命名画册")
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotobookCoverCard(
    photobook: PhotobookEntity,
    isSelected: Boolean = false,
    pageCount: Int = 0,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Column(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongPress
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(285f / 210f)
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(PaperTexture)
        ) {
            // Spine line via Canvas
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    Color.Black.copy(alpha = 0.15f),
                    Offset(8.dp.toPx(), 0f),
                    Offset(8.dp.toPx(), size.height)
                )
                drawLine(
                    Color.White.copy(alpha = 0.05f),
                    Offset(9.dp.toPx(), 0f),
                    Offset(9.dp.toPx(), size.height)
                )
            }

            // Cover image (if set)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
            ) {
                if (photobook.coverImageUri != null) {
                    AsyncImage(
                        model = android.net.Uri.parse(photobook.coverImageUri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .aspectRatio(2f)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }

            // Selection overlay
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Primary.copy(alpha = 0.15f))
                )
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(24.dp)
                )
            }
        }

        // Title row: name left, page count right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = photobook.name,
                style = TextStyle(
                    fontWeight = FontWeight.Light,
                    fontSize = 16.sp,
                    letterSpacing = 0.1.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${pageCount}页",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(photobook.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun CreateNewAlbumCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(285f / 210f)
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(PaperTexture),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            "新建画册",
            style = TextStyle(
                fontWeight = FontWeight.Light,
                fontSize = 16.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 12.dp, start = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePhotobookBottomSheet(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                "创建画册",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("画册名称") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("取消") }
                TextButton(
                    onClick = { onCreate(name.takeIf { it.isNotBlank() } ?: "未命名画册") },
                    enabled = name.isNotBlank()
                ) { Text("下一步") }
            }
        }
    }
}
