package com.yingjian.feature.memories

import android.net.Uri
import android.os.Build
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPostScreen(
    imageUris: List<Uri>,
    datesTaken: List<Long>,
    onPublish: (String, List<String>) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var previewIndex by rememberSaveable { mutableIntStateOf(0) }
    val uris = remember { mutableStateListOf(*imageUris.toTypedArray()) }
    val dates = remember { mutableStateListOf(*datesTaken.toTypedArray()) }
    var moodText by remember { mutableStateOf("") }
    val tags = remember { mutableStateListOf("#Life") }
    val defaultChips = listOf("#Life", "#Mood", "#Daily", "#Inspiration")
    val dateFormatter = remember { SimpleDateFormat("yyyy年M月d日", Locale.getDefault()) }
    val dateDisplay = remember(dates.getOrNull(previewIndex)) {
        dates.getOrNull(previewIndex)?.let { dateFormatter.format(it) } ?: ""
    }

    // Two independent launchers: multi (API 33+) and single (fallback)
    val pickMultipleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(9)
    ) { pickedUris: List<Uri> ->
        for (pickedUri in pickedUris) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    pickedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val (_, _, dateMs) = getImageMetadata(context, pickedUri)
            uris.add(pickedUri)
            dates.add(dateMs)
        }
    }

    val pickSingleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    pickedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            val (_, _, dateMs) = getImageMetadata(context, pickedUri)
            uris.add(pickedUri)
            dates.add(dateMs)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加影记") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { onPublish(moodText, tags.toList()) }) {
                        Icon(Icons.Default.Check, contentDescription = "发布")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Main image preview (3:2 aspect ratio, rounded corners)
            val previewUri = uris.getOrNull(previewIndex)
            if (previewUri != null) {
                AsyncImage(
                    model = previewUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 2f)
                        .clip(RoundedCornerShape(28.dp))
                )
            }

            // Thumbnail row
            LazyRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uris) { index, uri ->
                    ThumbnailItem(
                        uri = uri,
                        isSelected = index == previewIndex,
                        onSelected = { previewIndex = index },
                        onRemove = {
                            if (uris.size <= 1) return@ThumbnailItem
                            uris.removeAt(index)
                            dates.removeAt(index)
                            if (previewIndex >= uris.size) {
                                previewIndex = uris.size - 1
                            }
                        }
                    )
                }
                // "Add photos" button (last item, disabled at max 9)
                item {
                    AddPhotoButton(
                        enabled = uris.size < 9,
                        onClick = {
                            val maxPick = 9 - uris.size
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                pickMultipleLauncher.launch(
                                    PickVisualMediaRequest.Builder()
                                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        .setMaxItems(maxPick)
                                        .build()
                                )
                            } else {
                                pickSingleLauncher.launch(
                                    PickVisualMediaRequest.Builder()
                                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        .build()
                                )
                            }
                        }
                    )
                }
            }

            // Date display
            Text(
                dateDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Mood text input
            Text(
                "此刻心情",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            BasicTextField(
                value = moodText,
                onValueChange = { moodText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                decorationBox = { innerTextField ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        if (moodText.isEmpty()) {
                            Text(
                                "记录此刻的心情...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // Tag chips with add button
            Text(
                "标签",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            TagChips(
                tags = tags,
                defaultChips = defaultChips,
                onToggleTag = { chip ->
                    if (tags.contains(chip)) tags.remove(chip) else tags.add(chip)
                }
            )
        }
    }
}

@Composable
private fun ThumbnailItem(
    uri: Uri,
    isSelected: Boolean,
    onSelected: () -> Unit,
    onRemove: () -> Unit
) {
    Box(modifier = Modifier.size(100.dp)) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onSelected)
                .then(
                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    else Modifier
                )
        )
        // Remove badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.85f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "移除",
                modifier = Modifier.size(14.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun AddPhotoButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (enabled) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            .clickable(enabled = enabled, onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "添加照片",
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(24.dp)
        )
        Text(
            "添加",
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun TagChips(
    tags: List<String>,
    defaultChips: List<String>,
    onToggleTag: (String) -> Unit
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(defaultChips) { chip ->
            FilterChip(
                selected = tags.contains(chip),
                onClick = { onToggleTag(chip) },
                label = { Text(chip) }
            )
        }
        // Custom tags that are not in default chips
        items(tags.filter { it !in defaultChips }) { chip ->
            FilterChip(
                selected = true,
                onClick = { onToggleTag(chip) },
                label = { Text(chip) }
            )
        }
        // Add tag button
        item {
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { showAddDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "添加",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Add tag dialog
    if (showAddDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加标签") },
            text = {
                BasicTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    decorationBox = { innerTextField ->
                        if (newTagInput.isEmpty()) {
                            Text(
                                "输入标签...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val tag = newTagInput.trim().takeIf { it.isNotBlank() }
                    if (tag != null && tag !in tags) {
                        onToggleTag(tag)
                    }
                    newTagInput = ""
                    showAddDialog = false
                }) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    newTagInput = ""
                    showAddDialog = false
                }) {
                    Text("取消")
                }
            }
        )
    }
}
