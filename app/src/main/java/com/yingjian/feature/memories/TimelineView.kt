package com.yingjian.feature.memories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yingjian.core.data.database.MemoryRecordEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun TimelineView(memories: List<MemoryRecordEntity>) {
    if (memories.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "暂无照片",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Group by year-month
    val grouped = memories.groupBy { memory ->
        val cal = Calendar.getInstance().apply { timeInMillis = memory.timestamp }
        "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月"
    }

    val dateFormatter = SimpleDateFormat("yyyy年M月d日", Locale.getDefault())

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        grouped.forEach { (monthLabel, monthMemories) ->
            item {
                Text(
                    monthLabel,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
                )
            }

            // Featured entry (first in month) — full width card
            item {
                val featured = monthMemories.first()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    ) {
                        Column {
                            AsyncImage(
                                model = featured.imageUri,
                                contentDescription = featured.moodText,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(4f / 3f)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            )
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    dateFormatter.format(featured.timestamp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!featured.moodText.isNullOrBlank()) {
                                    Text(
                                        featured.moodText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Remaining entries — 2-column grid
            if (monthMemories.size > 1) {
                val restMemories = monthMemories.drop(1)
                items(items = restMemories.chunked(2)) { rowItems ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { memory ->
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                                )
                            ) {
                                Column {
                                    AsyncImage(
                                        model = memory.imageUri,
                                        contentDescription = memory.moodText,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                    )
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            dateFormatter.format(memory.timestamp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!memory.moodText.isNullOrBlank()) {
                                            Text(
                                                memory.moodText,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(top = 2.dp),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // Fill remaining slot if odd count
                        if (rowItems.size == 1) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}
