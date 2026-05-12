package com.yingjian.feature.memories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yingjian.core.data.database.MemoryRecordEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class CalendarDayInfo(val day: Int, val memories: List<MemoryRecordEntity>)

@Composable
fun CalendarView(
    memories: List<MemoryRecordEntity>,
    onMemoryClick: (MemoryRecordEntity) -> Unit = {}
) {
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

    // Sort memories by timestamp descending
    val sortedMemories = memories.sortedByDescending { it.timestamp }

    // Determine available months from memories (year-month), sorted descending (most recent first)
    val availableMonths = remember(sortedMemories) {
        sortedMemories.map { memory ->
            val cal = Calendar.getInstance().apply { timeInMillis = memory.timestamp }
            cal.get(Calendar.YEAR) to (cal.get(Calendar.MONTH) + 1)
        }.distinct().sortedWith(compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second })
    }

    // Current month index (default to most recent month with photos)
    var currentMonthIndex by remember { mutableIntStateOf(0) }
    val currentMonth = availableMonths.getOrNull(currentMonthIndex)

    if (currentMonth == null) return

    val (year, month) = currentMonth
    val monthMemories = sortedMemories.filter { memory ->
        val cal = Calendar.getInstance().apply { timeInMillis = memory.timestamp }
        cal.get(Calendar.YEAR) == year && (cal.get(Calendar.MONTH) + 1) == month
    }

    // Timeline days for current month — grouped by day, used for both calendar grid and timeline feed
    val timelineDays = remember(monthMemories) {
        monthMemories.groupBy { memory ->
            val cal = Calendar.getInstance().apply { timeInMillis = memory.timestamp }
            cal.get(Calendar.DAY_OF_MONTH)
        }.filterValues { it.isNotEmpty() }
            .mapKeys { (day, _) -> day }
            .entries
            .sortedByDescending { (day, _) -> day }
            .map { (day, dayMemories) ->
                CalendarDayInfo(day, dayMemories.sortedByDescending { it.timestamp })
            }
    }

    // LazyListState for scrolling to selected day
    val listState = remember { LazyListState() }
    val scope = rememberCoroutineScope()

    // Mapping: day number -> item index in LazyColumn (calendar items + spacer + one per timeline day)
    // Items: 0 = month selector, 1 = weekday header, 2 = calendar grid, 3 = spacer, 4+ = timeline days
    val dayToItemIndex = remember(timelineDays) {
        timelineDays.mapIndexed { index, dayInfo ->
            dayInfo.day to (4 + index)
        }.toMap()
    }

    val dateFormatter = remember { SimpleDateFormat("M月d日", Locale.getDefault()) }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Month selector header with navigation arrows
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${year}年 ${month}月",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(
                        onClick = {
                            if (currentMonthIndex < availableMonths.size - 1) {
                                currentMonthIndex++
                            }
                        },
                        enabled = currentMonthIndex < availableMonths.size - 1
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "上个月"
                        )
                    }
                    IconButton(
                        onClick = { if (currentMonthIndex > 0) currentMonthIndex-- },
                        enabled = currentMonthIndex > 0
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "下个月"
                        )
                    }
                }
            }
        }

        // Weekday headers
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Calendar grid — all weeks rendered in a single Column item
        item {
            val calendarDays = generateCalendarDays(month, year, monthMemories)
            val weekChunks = calendarDays.chunked(7)
            Column(modifier = Modifier.fillMaxWidth()) {
                weekChunks.forEach { weekDays ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        weekDays.forEach { dayInfo ->
                            CalendarDayCell(
                                dayInfo = dayInfo,
                                modifier = Modifier.weight(1f),
                                onDayClick = { day ->
                                    // Scroll to this day's timeline section
                                    dayToItemIndex[day]?.let { itemIndex ->
                                        scope.launch {
                                            listState.animateScrollToItem(itemIndex)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Spacer before timeline feed
        item { Spacer(modifier = Modifier.padding(8.dp)) }

        // Timeline feed — one LazyColumn item per day
        items(
            items = timelineDays,
            key = { it.day }
        ) { dayInfo ->
            CalendarTimelineDaySection(
                day = dayInfo.day,
                year = year,
                month = month,
                memories = dayInfo.memories,
                dateFormatter = dateFormatter,
                onMemoryClick = onMemoryClick
            )
        }

        item { Spacer(modifier = Modifier.padding(16.dp)) }
    }
}

/**
 * Timeline section for a single day — date label + ink divider + memory cards
 */
@Composable
private fun CalendarTimelineDaySection(
    day: Int,
    year: Int,
    month: Int,
    memories: List<MemoryRecordEntity>,
    dateFormatter: SimpleDateFormat,
    onMemoryClick: (MemoryRecordEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        // Day label: "3月10日"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${month}月${day}日",
                style = MaterialTheme.typography.titleLarge
            )
            Divider(
                modifier = Modifier.weight(1f).padding(start = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        }

        // Memory cards in 2-column layout
        memories.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
                                model = android.net.Uri.parse(memory.imageUri),
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
                // Fill empty slot if odd count
                if (rowItems.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun CalendarDayCell(
    dayInfo: CalendarDayInfo?,
    modifier: Modifier = Modifier,
    onDayClick: (Int) -> Unit = {}
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
    ) {
        if (dayInfo == null) {
            // Empty day cell — blank spacer
            return
        }

        val hasPhotos = dayInfo.memories.isNotEmpty()
        val shape = RoundedCornerShape(8.dp)

        if (hasPhotos) {
            // Photo cell: thumbnail background with bottom gradient + date in bottom-right
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .shadow(2.dp, shape)
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = shape
                    )
                    .clickable {
                        onDayClick(dayInfo.day)
                    }
            ) {
                // Background: thumbnail fills cell
                AsyncImage(
                    model = android.net.Uri.parse(dayInfo.memories.first().imageUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Bottom gradient overlay for date readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.4f)
                                ),
                                startY = 0.5f
                            )
                        )
                )

                // Date in bottom-right corner, white text
                Text(
                    text = dayInfo.day.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 6.dp, bottom = 4.dp)
                )
            }
        } else {
            // Empty day: surface background, centered date, subtle border
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        shape = shape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayInfo.day.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
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
    // Fill blank days after the last day to complete the final week
    val remaining = (7 - (result.size % 7)) % 7
    repeat(remaining) { result.add(null) }
    return result
}
