package com.yingjian.feature.photobook

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yingjian.core.data.database.MemoryRecordEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPickerScreen(
    memories: List<MemoryRecordEntity>,
    mode: MemoryPhotoPickerMode,
    onBack: () -> Unit,
    onComplete: (List<SelectedMemoryPhoto>) -> Unit
) {
    val selectedPhotos = remember { mutableStateListOf<SelectedMemoryPhoto>() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择照片") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (selectedPhotos.isNotEmpty()) {
                        Text(
                            "完成 (${selectedPhotos.size})",
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { onComplete(selectedPhotos.toList()) }
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("全部照片") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("按月份浏览") }
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize()
            ) {
                items(memories) { memory ->
                    val photos = memory.toSelectablePhotos()
                    val allSelected = photos.all { photo ->
                        selectedPhotos.any { it.memoryId == photo.memoryId && it.imageUri == photo.imageUri }
                    }

                    if (mode == MemoryPhotoPickerMode.BatchImport) {
                        PhotoGridItem(
                            memory = memory,
                            isSelected = allSelected,
                            onToggle = {
                                if (allSelected) {
                                    selectedPhotos.removeAll { selected ->
                                        photos.any { it.memoryId == selected.memoryId && it.imageUri == selected.imageUri }
                                    }
                                } else {
                                    photos.forEach { photo ->
                                        if (selectedPhotos.none { it.memoryId == photo.memoryId && it.imageUri == photo.imageUri }) {
                                            selectedPhotos.add(photo)
                                        }
                                    }
                                }
                            }
                        )
                    } else {
                        // Single-slot mode
                        val firstPhoto = photos.firstOrNull()
                        PhotoGridItem(
                            memory = memory,
                            isSelected = firstPhoto != null && selectedPhotos.any {
                                it.memoryId == firstPhoto.memoryId && it.imageUri == firstPhoto.imageUri
                            },
                            onToggle = {
                                if (mode == MemoryPhotoPickerMode.SingleSlot && firstPhoto != null) {
                                    selectedPhotos.clear()
                                    selectedPhotos.add(firstPhoto)
                                    onComplete(selectedPhotos.toList())
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Floating preview button (batch mode only)
    if (selectedPhotos.isNotEmpty() && mode == MemoryPhotoPickerMode.BatchImport) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            FloatingActionButton(
                onClick = { /* Preview selected */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null)
                Text(
                    "预览选中 (${selectedPhotos.size})",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun PhotoGridItem(
    memory: MemoryRecordEntity,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(1.dp)
            .aspectRatio(1f)
            .clickable(onClick = onToggle)
    ) {
        AsyncImage(
            model = Uri.parse(memory.imageUri),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.small),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
}
