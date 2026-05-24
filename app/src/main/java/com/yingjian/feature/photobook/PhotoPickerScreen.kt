package com.yingjian.feature.photobook

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yingjian.core.data.database.MemoryRecordEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPickerScreen(
    memories: List<MemoryRecordEntity>,
    mode: MemoryPhotoPickerMode,
    onBack: () -> Unit,
    onComplete: (List<SelectedMemoryPhoto>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val selectedPhotos = remember { mutableStateListOf<SelectedMemoryPhoto>() }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    fun completeSelection(photos: List<SelectedMemoryPhoto>) {
        coroutineScope.launch {
            val resolvedPhotos = withContext(Dispatchers.IO) {
                photos.map { it.withResolvedImageDimensions(context) }
            }
            onComplete(resolvedPhotos)
        }
    }

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
                                .clickable { completeSelection(selectedPhotos.toList()) }
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

            if (mode == MemoryPhotoPickerMode.SingleSlot) {
                // SingleSlot mode: show every individual photo from all memories
                val allPhotos = remember(memories) {
                    memories.flatMap { it.toSelectablePhotos() }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(allPhotos) { photo ->
                        val isSelected = selectedPhotos.any { it.memoryId == photo.memoryId && it.imageUri == photo.imageUri }
                        PhotoPickerGridItem(
                            uri = photo.imageUri,
                            isSelected = isSelected,
                            onToggle = {
                                selectedPhotos.clear()
                                selectedPhotos.add(photo)
                                completeSelection(selectedPhotos.toList())
                            }
                        )
                    }
                }
            } else {
                // BatchImport mode: one item per memory
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(memories) { memory ->
                        val photos = memory.toSelectablePhotos()
                        val allSelected = photos.all { photo ->
                            selectedPhotos.any { it.memoryId == photo.memoryId && it.imageUri == photo.imageUri }
                        }

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
                    }
                }
            }
        }
    }

}

private fun SelectedMemoryPhoto.withResolvedImageDimensions(context: Context): SelectedMemoryPhoto {
    val dimensions = resolveImageDimensions(context, imageUri) ?: return this
    return withImageDimensions(dimensions.first, dimensions.second)
}

private fun resolveImageDimensions(context: Context, uriString: String): Pair<Int, Int>? {
    val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }

    runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
    }.getOrNull()

    var width = bounds.outWidth
    var height = bounds.outHeight
    if (width <= 0 || height <= 0) return null

    val orientation = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
        } ?: ExifInterface.ORIENTATION_UNDEFINED
    }.getOrDefault(ExifInterface.ORIENTATION_UNDEFINED)

    if (orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
        orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
        orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
        orientation == ExifInterface.ORIENTATION_TRANSVERSE
    ) {
        val originalWidth = width
        width = height
        height = originalWidth
    }

    return width to height
}

@Composable
fun PhotoPickerGridItem(
    uri: String,
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
            model = Uri.parse(uri),
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
