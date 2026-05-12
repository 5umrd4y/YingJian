package com.yingjian.feature.memories

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Locale
import coil3.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPostScreen(
    imageUri: Uri,
    dateTaken: Long,
    onPublish: (String, List<String>) -> Unit,
    onBack: () -> Unit
) {
    var moodText by remember { mutableStateOf("") }
    val tags = remember { mutableStateListOf("#Life") }
    val defaultChips = listOf("#Life", "#Mood", "#Daily", "#Inspiration")
    val dateFormatter = remember { SimpleDateFormat("yyyy年M月d日", Locale.getDefault()) }
    val dateDisplay = remember(dateTaken) { dateFormatter.format(dateTaken) }

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
            // Image preview
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(28.dp))
            )

            // Date display
            Text(
                dateDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
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
                    .padding(12.dp),
                decorationBox = { innerTextField ->
                    if (moodText.isEmpty()) {
                        Text(
                            "记录此刻的心情...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            )

            // Tag chips
            Text(
                "标签",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            LazyRow {
                items(defaultChips) { chip ->
                    FilterChip(
                        selected = tags.contains(chip),
                        onClick = {
                            if (tags.contains(chip)) tags.remove(chip) else tags.add(chip)
                        },
                        label = { Text(chip) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }
    }
}
