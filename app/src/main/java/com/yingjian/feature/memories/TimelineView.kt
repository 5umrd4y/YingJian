package com.yingjian.feature.memories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yingjian.core.data.database.MemoryRecordEntity
import java.util.Calendar

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

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        grouped.forEach { (monthLabel, monthMemories) ->
            item {
                Text(
                    monthLabel,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
                )
            }
            val rowChunks = monthMemories.chunked(3)
            items(items = rowChunks) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rowItems.forEach { memory ->
                        Card(
                            modifier = Modifier.weight(1f).aspectRatio(4f / 3f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                            )
                        ) {
                            AsyncImage(
                                model = memory.imageUri,
                                contentDescription = memory.moodText,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    // Fill remaining slots if less than 3
                    repeat(3 - rowItems.size) {
                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(4f / 3f)
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.padding(4.dp)) }
        }
    }
}
