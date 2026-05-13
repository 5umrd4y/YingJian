package com.yingjian.feature.memories

import android.net.Uri
import android.os.Build
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.yingjian.core.data.database.MemoryRecordEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesScreen(
    viewModel: MemoriesViewModel,
    onNavigateToNewPost: (List<Uri>, List<Long>) -> Unit,
    onMemoryClick: (MemoryRecordEntity) -> Unit
) {
    val context = LocalContext.current
    var showCalendar by rememberSaveable { mutableStateOf(false) }

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
            val dates = uris.map { uri -> getImageMetadata(context, uri).third }
            onNavigateToNewPost(uris, dates)
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
            onNavigateToNewPost(listOf(pickedUri), listOf(dateMs))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("影记") },
                actions = {
                    IconButton(onClick = { showCalendar = !showCalendar }) {
                        Icon(
                            imageVector = if (showCalendar) Icons.Default.ViewAgenda else Icons.Default.CalendarMonth,
                            contentDescription = if (showCalendar) "切换时光轴" else "切换日历"
                        )
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
            if (showCalendar) {
                CalendarView(
                    memories = viewModel.uiState.memories,
                    onMemoryClick = onMemoryClick
                )
            } else {
                TimelineView(
                    memories = viewModel.uiState.memories,
                    onMemoryClick = onMemoryClick
                )
            }
        }
    }
}

/**
 * Get image metadata (width, height, dateTakenMs) from a content URI.
 * Uses ExifInterface for date extraction with fallback chain:
 * TAG_DATETIME_ORIGINAL > TAG_DATETIME > MediaStore DATE_MODIFIED > currentTime
 */
fun getImageMetadata(context: Context, uri: Uri): Triple<Int, Int, Long> {
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
