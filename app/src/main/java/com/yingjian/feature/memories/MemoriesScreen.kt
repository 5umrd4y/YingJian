package com.yingjian.feature.memories

import android.content.Context
import android.media.ExifInterface
import android.provider.MediaStore
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun MemoriesScreen(
    viewModel: MemoriesViewModel,
    onNavigateToNewPost: (Uri) -> Unit
) {
    val context = LocalContext.current
    val selectedTab = rememberSaveable { mutableIntStateOf(0) }

    // Photo picker: after picking, navigate to NewPostScreen
    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { onNavigateToNewPost(it) }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
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
            TabRow(selectedTabIndex = selectedTab.intValue) {
                Tab(
                    selected = selectedTab.intValue == 0,
                    onClick = { selectedTab.intValue = 0 }
                ) {
                    Text("时光轴", modifier = Modifier.padding(16.dp))
                }
                Tab(
                    selected = selectedTab.intValue == 1,
                    onClick = { selectedTab.intValue = 1 }
                ) {
                    Text("日历", modifier = Modifier.padding(16.dp))
                }
            }
            when (selectedTab.intValue) {
                0 -> TimelineView(memories = viewModel.uiState.memories)
                1 -> CalendarView(memories = viewModel.uiState.memories)
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
            // Fallback to MediaStore DATE_MODIFIED
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
