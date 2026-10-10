package com.yingjian.feature.memories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yingjian.core.data.database.MemoryRecordEntity
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun TimelineView(
    memories: List<MemoryRecordEntity>,
    onMemoryClick: (MemoryRecordEntity) -> Unit = {},
    onAddPhoto: () -> Unit = {}
) {
    val sortedMemories = remember(memories) { memories.sortedByDescending { it.timestamp } }
    val grouped = remember(sortedMemories) {
        sortedMemories.groupBy { memory ->
            val cal = Calendar.getInstance().apply { timeInMillis = memory.timestamp }
            cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
        }
    }
    val monthFormatter = remember { SimpleDateFormat("yyyy年M月", Locale.getDefault()) }
    val dayFormatter = remember { SimpleDateFormat("M月d日 EEEE", Locale.getDefault()) }
    val today = Calendar.getInstance().run {
        get(Calendar.YEAR) * 10_000 + get(Calendar.MONTH) * 100 + get(Calendar.DAY_OF_MONTH)
    }
    val todayMemories = remember(sortedMemories, today) {
        sortedMemories.filter { memory ->
            val cal = Calendar.getInstance().apply { timeInMillis = memory.timestamp }
            cal.get(Calendar.YEAR) * 10_000 + cal.get(Calendar.MONTH) * 100 + cal.get(Calendar.DAY_OF_MONTH) == today
        }
    }
    val todayFormatter = remember { SimpleDateFormat("yyyy年M月d日 EEEE", Locale.getDefault()) }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            TodayOverview(
                date = todayFormatter.format(Calendar.getInstance().time),
                recordCount = todayMemories.size,
                onAddPhoto = onAddPhoto
            )
        }

        if (memories.isEmpty()) {
            item {
                Text(
                    text = "照片会按拍摄日期留在这里",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp)
                )
            }
        }

        grouped.forEach { (monthKey, monthMemories) ->
            item {
                val monthStart = Calendar.getInstance().apply {
                    set(Calendar.YEAR, monthKey.first)
                    set(Calendar.MONTH, monthKey.second)
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                Text(
                    text = monthFormatter.format(monthStart.time),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
                )
            }

            val dailyGroups = monthMemories.groupBy { memory ->
                val cal = Calendar.getInstance().apply { timeInMillis = memory.timestamp }
                cal.get(Calendar.YEAR) * 10_000 + cal.get(Calendar.MONTH) * 100 + cal.get(Calendar.DAY_OF_MONTH)
            }
            dailyGroups.forEach { (dayKey, dayMemories) ->
                item {
                    val dayMemory = dayMemories.first()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                        text = dayFormatter.format(dayMemory.timestamp),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (dayKey == today) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${dayMemories.size} 条影记",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                items(items = dayMemories.chunked(2)) { rowItems ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { memory ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onMemoryClick(memory) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                                )
                            ) {
                                Column {
                                    AsyncImage(
                                        model = Uri.parse(memory.imageUri),
                                        contentDescription = memory.moodText,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                    )
                                    if (!memory.moodText.isNullOrBlank()) {
                                        Text(
                                            text = memory.moodText,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(8.dp),
                                            maxLines = 1
                                        )
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

@Composable
private fun TodayOverview(
    date: String,
    recordCount: Int,
    onAddPhoto: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 18.dp, end = 12.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TODAY  /  ${Calendar.getInstance().get(Calendar.YEAR)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
                )
                Text(
                    text = "今天",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(top = 3.dp)
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.78f),
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = if (recordCount == 0) "今天还没有影记" else "今天留下了 $recordCount 条影记",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            TextButton(onClick = onAddPhoto) {
                Text(if (recordCount == 0) "记录一刻" else "继续记录")
            }
        }
    }
}
