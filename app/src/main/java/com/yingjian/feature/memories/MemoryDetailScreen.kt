package com.yingjian.feature.memories

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yingjian.core.data.database.MemoryRecordEntity
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryDetailScreen(
    memory: MemoryRecordEntity,
    onBack: () -> Unit,
    onUpdate: (MemoryRecordEntity) -> Unit,
    onDelete: () -> Unit
) {
    val allImageUris = remember(memory.imageUrisJson) { memory.getAllImageUris() }
    val urisAsUri = allImageUris.map { Uri.parse(it) }

    // Empty state protection
    if (urisAsUri.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("无照片", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { urisAsUri.size })
    var showChrome by rememberSaveable { mutableStateOf(true) }
    var showEditSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val dateFormatter = remember { SimpleDateFormat("yyyy年M月d日", Locale.getDefault()) }
    val dateDisplay = dateFormatter.format(memory.timestamp)
    val tags = remember {
        runCatching {
            Json.decodeFromString<List<String>>(memory.tags)
        }.getOrNull() ?: emptyList()
    }

    Scaffold(
        topBar = {
            if (showChrome) {
                TopAppBar(
                    title = {
                        Text(
                            "${pagerState.currentPage + 1}/${urisAsUri.size}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showEditSheet = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
            }
        },
        bottomBar = {
            if (showChrome) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        .padding(16.dp)
                ) {
                    // Date
                    Text(
                        dateDisplay,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Mood
                    if (!memory.moodText.isNullOrBlank()) {
                        Text(
                            memory.moodText,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    // Tags
                    if (tags.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tags.forEach { tag ->
                                Text(
                                    tag,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    // Thumbnail strip (only if multiple photos)
                    if (urisAsUri.size > 1) {
                        LazyRow(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(urisAsUri) { index, uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            scope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        }
                                        .then(
                                            if (index == pagerState.currentPage)
                                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                            else Modifier
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model = urisAsUri[page],
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showChrome = !showChrome }
                )
            }
        }
    }

    // Edit bottom sheet
    if (showEditSheet) {
        MemoryDetailEditSheet(
            totalPhotos = urisAsUri.size,
            onDeleteCurrentPhoto = {
                showEditSheet = false
                val remaining = allImageUris.toMutableList().apply {
                    removeAt(pagerState.currentPage)
                }
                if (remaining.isEmpty()) {
                    onDelete()
                } else {
                    onUpdate(
                        memory.copy(
                            imageUrisJson = Json.encodeToString(
                                ListSerializer(String.serializer()),
                                remaining
                            ),
                            imageUri = remaining.first()
                        )
                    )
                }
            },
            onDeleteEntireMemory = {
                showEditSheet = false
                showDeleteConfirm = true
            },
            onDismiss = { showEditSheet = false }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("删除整条影记将移除所有照片和信息，此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoryDetailEditSheet(
    totalPhotos: Int,
    onDeleteCurrentPhoto: () -> Unit,
    onDeleteEntireMemory: () -> Unit,
    onDismiss: () -> Unit
) {
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
                "编辑影记",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Note: "添加照片" is handled by returning to select again
            Text(
                "要添加照片，请返回后重新选择",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Delete current photo (only if more than 1 photo)
            if (totalPhotos > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDeleteCurrentPhoto() }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "删除此照片",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Delete entire memory
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDeleteEntireMemory() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    "删除整条影记",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
