package com.yingjian.feature.memories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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

data class CalendarDayInfo(val day: Int, val memories: List<MemoryRecordEntity>)

@Composable
fun CalendarView(memories: List<MemoryRecordEntity>) {
    if (memories.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "暂无照片",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }

    // Group by month (year-month string to memories list)
    val grouped = memories.groupBy { memory ->
        val cal = Calendar.getInstance().apply { timeInMillis = memory.timestamp }
        "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月" to (cal.get(Calendar.MONTH) + 1)
    }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        grouped.forEach { (label, monthMemories) ->
            val (yearMonthStr, monthNum) = label
            val year = Calendar.getInstance().get(Calendar.YEAR)
            item {
                Text(
                    yearMonthStr,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp, 16.dp, 8.dp, 4.dp)
                )
                // Weekday headers
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                        Text(
                            day,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            // Generate calendar grid days for this month
            val calendarDays = generateCalendarDays(monthNum, year, monthMemories)
            val weekChunks = calendarDays.chunked(7)
            items(items = weekChunks) { weekDays ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekDays.forEach { dayInfo ->
                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f).padding(1.dp)
                        ) {
                            if (dayInfo != null) {
                                // Background: thumbnail
                                dayInfo.memories.firstOrNull()?.let { memory ->
                                    AsyncImage(
                                        model = memory.imageUri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } ?: Box(
                                    modifier = Modifier.fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                )

                                // Foreground: date overlay
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        dayInfo.day.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                // Empty day cell
                                Box(
                                    modifier = Modifier.fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.padding(8.dp)) }
        }
    }
}

/**
 * Generate a list of CalendarDayInfo for the given month (1-based), including blank days for alignment.
 * Maps actual memories to their date cells.
 */
fun generateCalendarDays(month: Int, year: Int, memories: List<MemoryRecordEntity>): List<CalendarDayInfo?> {
    val cal = Calendar.getInstance()
    cal.set(year, month - 1, 1)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sunday
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Build a map of day -> memories for that day
    val dayMemoryMap = memories.groupBy { memory ->
        val mCal = Calendar.getInstance().apply { timeInMillis = memory.timestamp }
        mCal.get(Calendar.DAY_OF_MONTH)
    }

    val result = mutableListOf<CalendarDayInfo?>()
    // Fill blank days before the 1st
    repeat(firstDayOfWeek - 1) { result.add(null) }
    // Fill actual days
    for (day in 1..daysInMonth) {
        result.add(CalendarDayInfo(day, dayMemoryMap[day] ?: emptyList()))
    }
    return result
}
