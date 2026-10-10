package com.yingjian.feature.memories

import android.net.Uri
import android.os.Build
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.yingjian.core.data.database.MemoryRecordEntity
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesScreen(
    viewModel: MemoriesViewModel,
    onNavigateToNewPost: (List<Uri>, List<Long>) -> Unit,
    onMemoryClick: (MemoryRecordEntity) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedView by rememberSaveable { mutableStateOf("timeline") }
    var pendingShareRequest by remember { mutableStateOf<CalendarExportRequest?>(null) }
    var showShareOptions by remember { mutableStateOf(false) }
    var pendingCalendarImage by remember { mutableStateOf<File?>(null) }
    val saveCalendarImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { destinationUri ->
        val sourceFile = pendingCalendarImage
        pendingCalendarImage = null
        if (destinationUri == null || sourceFile == null) {
            sourceFile?.delete()
        } else {
            coroutineScope.launch {
                val saved = runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                            sourceFile.inputStream().use { input -> input.copyTo(output) }
                        } ?: error("无法创建图片文件")
                    }
                }.isSuccess
                sourceFile.delete()
                Toast.makeText(
                    context,
                    if (saved) "日历图片已导出" else "导出失败，请重试",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // Two independent launchers: multi (API 33+) and single (fallback)
    val pickMultipleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(9)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            // Persist URI permissions so images survive app restart
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            coroutineScope.launch {
                val dates = uris.map { uri -> getImageMetadata(context, uri).third }
                onNavigateToNewPost(uris, dates)
            }
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
            coroutineScope.launch {
                val (_, _, dateMs) = getImageMetadata(context, pickedUri)
                onNavigateToNewPost(listOf(pickedUri), listOf(dateMs))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    androidx.compose.foundation.layout.Row(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("timeline" to "时光轴", "month" to "月历", "year" to "年历").forEach { (mode, label) ->
                            val activeColor = MaterialTheme.colorScheme.primary
                            val isSelected = selectedView == mode
                            Text(
                                label,
                                style = if (isSelected) MaterialTheme.typography.titleLarge
                                else MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { selectedView = mode }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pickMultipleLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    } else {
                        pickSingleLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "添加照片")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selectedView != "timeline") {
                CalendarView(
                    memories = viewModel.uiState.memories,
                    isYear = selectedView == "year",
                    onModeSelected = { selectedView = if (it) "year" else "month" },
                    onMemoryClick = onMemoryClick,
                    onExportImage = { request, action ->
                        if (action == CalendarImageAction.OPTIONS) {
                            pendingShareRequest = request
                            showShareOptions = true
                            return@CalendarView
                        }
                        coroutineScope.launch {
                            runCatching {
                                val imageFile = withContext(Dispatchers.IO) {
                                    val folder = File(context.cacheDir, "calendar_exports").apply { mkdirs() }
                                    val outputFile = File(folder, request.fileName)
                                    val bitmap = CalendarImageExporter.render(
                                        context,
                                        request,
                                        viewModel.uiState.memories
                                    )
                                    try {
                                        outputFile.outputStream().use { stream ->
                                            check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)) {
                                                "无法编码日历图片"
                                            }
                                        }
                                    } finally {
                                        bitmap.recycle()
                                    }
                                    outputFile
                                }
                                if (action == CalendarImageAction.SAVE) {
                                    pendingCalendarImage?.delete()
                                    pendingCalendarImage = imageFile
                                    saveCalendarImageLauncher.launch(request.fileName)
                                } else {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        imageFile
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        clipData = android.content.ClipData.newUri(
                                            context.contentResolver,
                                            "日历图片",
                                            uri
                                        )
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    val title = if (request.month == null) "分享年历图片" else "分享月历图片"
                                    context.startActivity(Intent.createChooser(shareIntent, title))
                                }
                            }.onFailure {
                                Toast.makeText(context, "日历图片生成失败，请重试", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            } else {
                TimelineView(
                    memories = viewModel.uiState.memories,
                    onMemoryClick = onMemoryClick,
                    onAddPhoto = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pickMultipleLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        } else {
                            pickSingleLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    }
                )
            }
        }
    }

    if (showShareOptions) {
        AlertDialog(
            onDismissRequest = { showShareOptions = false },
            title = { Text("分享日历") },
            text = { Text("选择保存到本地，或分享给其他应用。") },
            confirmButton = {
                TextButton(onClick = {
                    showShareOptions = false
                    pendingShareRequest?.let { request ->
                        coroutineScope.launch {
                            createCalendarImage(context, request, viewModel.uiState.memories)?.let { imageFile ->
                                pendingCalendarImage?.delete()
                                pendingCalendarImage = imageFile
                                saveCalendarImageLauncher.launch(request.fileName)
                            }
                        }
                    }
                }) { Text("保存到本地") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showShareOptions = false
                    pendingShareRequest?.let { request ->
                        coroutineScope.launch {
                            val imageFile = createCalendarImage(context, request, viewModel.uiState.memories)
                            if (imageFile != null) {
                                runCatching {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        imageFile
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        clipData = android.content.ClipData.newUri(context.contentResolver, "日历图片", uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    val title = if (request.month == null) "分享年历图片" else "分享月历图片"
                                    context.startActivity(Intent.createChooser(shareIntent, title))
                                }.onFailure {
                                    Toast.makeText(context, "日历图片分享失败，请重试", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }) { Text("分享图片") }
            }
        )
    }
}

private suspend fun createCalendarImage(
    context: Context,
    request: CalendarExportRequest,
    memories: List<MemoryRecordEntity>
): File? = runCatching {
    withContext(Dispatchers.IO) {
        val folder = File(context.cacheDir, "calendar_exports").apply { mkdirs() }
        val outputFile = File(folder, request.fileName)
        val bitmap = CalendarImageExporter.render(context, request, memories)
        try {
            outputFile.outputStream().use { stream ->
                check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)) {
                    "无法编码日历图片"
                }
            }
        } finally {
            bitmap.recycle()
        }
        outputFile
    }
}.onFailure {
    Toast.makeText(context, "日历图片生成失败，请重试", Toast.LENGTH_SHORT).show()
}.getOrNull()

/**
 * Get image metadata (width, height, dateTakenMs) from a content URI.
 * Uses ExifInterface for date extraction with fallback chain:
 * TAG_DATETIME_ORIGINAL > TAG_DATETIME > MediaStore DATE_MODIFIED > currentTime
 */
suspend fun getImageMetadata(context: Context, uri: Uri): Triple<Int, Int, Long> = withContext(Dispatchers.IO) {
    readImageMetadata(context, uri)
}

private fun readImageMetadata(context: Context, uri: Uri): Triple<Int, Int, Long> {
    var width = 0
    var height = 0
    var dateTakenMs = System.currentTimeMillis()

    context.contentResolver.openInputStream(uri)?.use { stream ->
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(stream, null, options)
        width = options.outWidth
        height = options.outHeight
    }

    context.contentResolver.openInputStream(uri)?.use { stream ->
        val exif = ExifInterface(stream)
        val dateTimeOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
        val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME)

        val rawDate = dateTimeOriginal ?: dateTime
        if (rawDate != null) {
            kotlin.runCatching {
                val format = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.getDefault())
                format.timeZone = java.util.TimeZone.getDefault()
                dateTakenMs = format.parse(rawDate)?.time ?: System.currentTimeMillis()
            }.onFailure {
                dateTakenMs = System.currentTimeMillis()
            }
        } else {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns.DATE_MODIFIED),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val modifiedSec = cursor.getLong(0)
                    dateTakenMs = modifiedSec * 1000
                }
            }
        }
    }

    return Triple(width, height, dateTakenMs)
}
