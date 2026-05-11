package com.yingjian.feature.memories

import android.content.Context
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
 * Get image dimensions from a content URI.
 */
fun getImageDimensions(context: Context, uri: Uri): Pair<Int, Int> {
    context.contentResolver.openInputStream(uri)?.use { stream ->
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(stream, null, options)
        return options.outWidth to options.outHeight
    }
    return 0 to 0
}
