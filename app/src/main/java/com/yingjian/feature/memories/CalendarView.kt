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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yingjian.core.data.database.MemoryRecordEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.min

data class CalendarDayInfo(val day: Int, val memories: List<MemoryRecordEntity>)

@Composable
fun CalendarView(
    memories: List<MemoryRecordEntity>,
    isYear: Boolean = false,
    onModeSelected: (Boolean) -> Unit = {},
    onMemoryClick: (MemoryRecordEntity) -> Unit = {},
    onExportImage: (CalendarExportRequest, CalendarImageAction) -> Unit = { _, _ -> }
) {
    val sortedMemories = remember(memories) { memories.sortedByDescending { it.timestamp } }
    val now = Calendar.getInstance()
    val currentMonthOrdinal = now.get(Calendar.YEAR) * 12 + now.get(Calendar.MONTH)
    val memoryMonthOrdinals = remember(sortedMemories) {
        sortedMemories.map { memory ->
            val date = Calendar.getInstance().apply { timeInMillis = memory.timestamp }
            date.get(Calendar.YEAR) * 12 + date.get(Calendar.MONTH)
        }
    }
    val minMonthOrdinal = min(memoryMonthOrdinals.minOrNull() ?: currentMonthOrdinal, currentMonthOrdinal)
    val maxMonthOrdinal = maxOf(memoryMonthOrdinals.maxOrNull() ?: currentMonthOrdinal, currentMonthOrdinal)
    var selectedMonthOrdinal by rememberSaveable {
        mutableIntStateOf(memoryMonthOrdinals.maxOrNull() ?: currentMonthOrdinal)
    }
    val showYear = isYear

    val selectedYearForMonth = Math.floorDiv(selectedMonthOrdinal, 12)
    val selectedMonth = Math.floorMod(selectedMonthOrdinal, 12) + 1
    val minYear = Math.floorDiv(minMonthOrdinal, 12)
    val maxYear = Math.floorDiv(maxMonthOrdinal, 12)
    val monthMemories = remember(sortedMemories, selectedMonthOrdinal) {
        sortedMemories.filter { memory -> monthOrdinal(memory.timestamp) == selectedMonthOrdinal }
    }
    val yearMemories = remember(sortedMemories, selectedYearForMonth) {
        sortedMemories.filter { yearOf(it.timestamp) == selectedYearForMonth }
    }
    val timelineDays = remember(monthMemories) {
        monthMemories.groupBy { dayOf(it.timestamp) }
            .map { (day, dayMemories) -> CalendarDayInfo(day, dayMemories.sortedByDescending { it.timestamp }) }
            .sortedByDescending { it.day }
    }
    val dayToItemIndex = remember(timelineDays) {
        timelineDays.mapIndexed { index, dayInfo -> dayInfo.day to (4 + index) }.toMap()
    }
    val listState = remember { LazyListState() }
    val scope = rememberCoroutineScope()
    val dateFormatter = remember { SimpleDateFormat("M月d日", Locale.getDefault()) }

    if (showYear) {
        YearCalendarView(
            year = selectedYearForMonth,
            yearMemories = yearMemories,
            canGoBack = selectedYearForMonth > minYear,
            canGoForward = selectedYearForMonth < maxYear,
            onPreviousYear = {
                selectedMonthOrdinal -= 12
            },
            onNextYear = {
                selectedMonthOrdinal += 12
            },
            onSelectMonth = { month ->
                selectedMonthOrdinal = selectedYearForMonth * 12 + month - 1
                onModeSelected(false)
            },
            onExport = { action -> onExportImage(CalendarExportRequest(selectedYearForMonth), action) }
        )
        return
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item {
            CalendarControls(
                title = "${selectedYearForMonth}年 ${selectedMonth}月",
                canGoBack = selectedMonthOrdinal > minMonthOrdinal,
                canGoForward = selectedMonthOrdinal < maxMonthOrdinal,
                onPrevious = { selectedMonthOrdinal-- },
                onNext = { selectedMonthOrdinal++ },
                onExport = { action ->
                    onExportImage(CalendarExportRequest(selectedYearForMonth, selectedMonth), action)
                }
            )
        }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            val calendarDays = generateCalendarDays(selectedMonth, selectedYearForMonth, monthMemories)
            Column(modifier = Modifier.fillMaxWidth()) {
                calendarDays.chunked(7).forEach { weekDays ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        weekDays.forEach { dayInfo ->
                            CalendarDayCell(
                                dayInfo = dayInfo,
                                modifier = Modifier.weight(1f),
                                onDayClick = { day ->
                                    dayToItemIndex[day]?.let { itemIndex ->
                                        scope.launch { listState.animateScrollToItem(itemIndex) }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.padding(8.dp)) }
        if (timelineDays.isEmpty()) {
            item {
                Text(
                    text = "這個月還沒有影記",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
        items(items = timelineDays, key = { "${selectedMonthOrdinal}-${it.day}" }) { dayInfo ->
            CalendarTimelineDaySection(
                day = dayInfo.day,
                month = selectedMonth,
                memories = dayInfo.memories,
                dateFormatter = dateFormatter,
                onMemoryClick = onMemoryClick
            )
        }
        item { Spacer(modifier = Modifier.padding(16.dp)) }
    }
}

@Composable
private fun CalendarControls(
    title: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onExport: (CalendarImageAction) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious, enabled = canGoBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一页")
            }
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onNext, enabled = canGoForward) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一页")
            }
            TextButton(onClick = { onExport(CalendarImageAction.OPTIONS) }) { Text("分享") }
        }
    }
}

@Composable
private fun YearCalendarView(
    year: Int,
    yearMemories: List<MemoryRecordEntity>,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPreviousYear: () -> Unit,
    onNextYear: () -> Unit,
    onSelectMonth: (Int) -> Unit,
    onExport: (CalendarImageAction) -> Unit
) {
    val activeDays = remember(yearMemories) { yearMemories.map { dayKey(it.timestamp) }.distinct().size }
    val activeMonths = remember(yearMemories) { yearMemories.map { monthOf(it.timestamp) }.distinct().size }
    val memoriesByMonth = remember(yearMemories) { yearMemories.groupBy { monthOf(it.timestamp) } }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            CalendarControls(
                title = "${year}年",
                canGoBack = canGoBack,
                canGoForward = canGoForward,
                onPrevious = onPreviousYear,
                onNext = onNextYear,
                onExport = onExport
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    YearStat(value = yearMemories.size.toString(), label = "条影记")
                    YearStat(value = activeDays.toString(), label = "个记录日")
                    YearStat(value = activeMonths.toString(), label = "个有记录的月份")
                }
            }
        }
        items((1..12).toList().chunked(3)) { monthRow ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                monthRow.forEach { month ->
                    MiniMonthCard(
                        modifier = Modifier.weight(1f),
                        year = year,
                        month = month,
                        memories = memoriesByMonth[month].orEmpty(),
                        onClick = { onSelectMonth(month) }
                    )
                }
                repeat(3 - monthRow.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
        item { Spacer(modifier = Modifier.padding(12.dp)) }
    }
}

@Composable
private fun YearStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSecondaryContainer)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f))
    }
}

@Composable
private fun MiniMonthCard(
    modifier: Modifier = Modifier,
    year: Int,
    month: Int,
    memories: List<MemoryRecordEntity>,
    onClick: () -> Unit
) {
    val recordedDays = remember(memories) { memories.map { dayOf(it.timestamp) }.toSet() }
    val cells = remember(year, month) { generateCalendarDays(month, year, emptyList()) }
    Card(
        modifier = modifier.padding(vertical = 4.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp)) {
            Text(
                text = "${month}月",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 3.dp)) {
                listOf("日", "一", "二", "三", "四", "五", "六").forEach { weekday ->
                    Text(
                        weekday,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { dayInfo ->
                        Box(
                            modifier = Modifier.weight(1f).height(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayInfo != null) {
                                val marked = dayInfo.day in recordedDays
                                if (marked) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                    )
                                }
                                Text(
                                    dayInfo.day.toString(),
                                    fontSize = 9.sp,
                                    lineHeight = 10.sp,
                                    color = if (marked) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                                    modifier = Modifier.offset(y = (-0.5).dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarTimelineDaySection(
    day: Int,
    month: Int,
    memories: List<MemoryRecordEntity>,
    dateFormatter: SimpleDateFormat,
    onMemoryClick: (MemoryRecordEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = "${month}月${day}日", style = MaterialTheme.typography.titleLarge)
            Divider(
                modifier = Modifier.weight(1f).padding(start = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        }
        memories.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { memory ->
                    Card(
                        modifier = Modifier.weight(1f).clickable { onMemoryClick(memory) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
                    ) {
                        Column {
                            AsyncImage(
                                model = android.net.Uri.parse(memory.imageUri),
                                contentDescription = memory.moodText,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                            )
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(dateFormatter.format(memory.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (!memory.moodText.isNullOrBlank()) {
                                    Text(memory.moodText, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp), maxLines = 1)
                                }
                            }
                        }
                    }
                }
                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
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
    Box(modifier = modifier.aspectRatio(1f).padding(2.dp)) {
        if (dayInfo == null) return
        val shape = RoundedCornerShape(8.dp)
        if (dayInfo.memories.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().clip(shape).shadow(2.dp, shape)
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), shape)
                    .clickable { onDayClick(dayInfo.day) }
            ) {
                AsyncImage(
                    model = android.net.Uri.parse(dayInfo.memories.first().imageUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)), startY = 0.5f)
                    )
                )
                Text(
                    text = dayInfo.day.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 4.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().clip(shape).background(MaterialTheme.colorScheme.surface)
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f), shape),
                contentAlignment = Alignment.Center
            ) {
                Text(dayInfo.day.toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

fun generateCalendarDays(month: Int, year: Int, memories: List<MemoryRecordEntity>): List<CalendarDayInfo?> {
    val calendar = Calendar.getInstance().apply { set(year, month - 1, 1) }
    val offset = calendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val dayMemoryMap = memories.groupBy { dayOf(it.timestamp) }
    val result = mutableListOf<CalendarDayInfo?>()
    repeat(offset) { result.add(null) }
    for (day in 1..daysInMonth) result.add(CalendarDayInfo(day, dayMemoryMap[day].orEmpty()))
    repeat((7 - result.size % 7) % 7) { result.add(null) }
    return result
}

private fun monthOrdinal(timestamp: Long): Int {
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }
    return date.get(Calendar.YEAR) * 12 + date.get(Calendar.MONTH)
}

private fun yearOf(timestamp: Long) = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.YEAR)
private fun monthOf(timestamp: Long) = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.MONTH) + 1
private fun dayOf(timestamp: Long) = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.DAY_OF_MONTH)
private fun dayKey(timestamp: Long): Int {
    val date = Calendar.getInstance().apply { timeInMillis = timestamp }
    return date.get(Calendar.YEAR) * 10_000 + monthOf(timestamp) * 100 + date.get(Calendar.DAY_OF_MONTH)
}
