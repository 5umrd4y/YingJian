package com.yingjian.feature.memories

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import com.yingjian.core.data.database.MemoryRecordEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

data class CalendarExportRequest(
    val year: Int,
    val month: Int? = null
) {
    val fileName: String
        get() = month?.let { "影笺_${year}年${it}月_月历.png" } ?: "影笺_${year}年_年历.png"
}

enum class CalendarImageAction { OPTIONS, SAVE, SHARE }

object CalendarImageExporter {
    private const val WIDTH = 1080
    private val paper = Color.rgb(248, 245, 239)
    private val ink = Color.rgb(48, 43, 37)
    private val mutedInk = Color.rgb(124, 115, 102)
    private val accent = Color.rgb(102, 90, 74)
    private val cellPaper = Color.rgb(255, 253, 249)

    fun render(
        context: Context,
        request: CalendarExportRequest,
        memories: List<MemoryRecordEntity>
    ): Bitmap = request.month?.let { renderMonth(context, request.year, it, memories) }
        ?: renderYear(request.year, memories)

    private fun renderMonth(
        context: Context,
        year: Int,
        month: Int,
        memories: List<MemoryRecordEntity>
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, 1440, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(paper)

        val monthMemories = memories.filter { memory ->
            val date = Calendar.getInstance().apply { timeInMillis = memory.timestamp }
            date.get(Calendar.YEAR) == year && date.get(Calendar.MONTH) + 1 == month
        }.sortedByDescending { it.timestamp }
        val byDay = monthMemories.groupBy { dayOf(it.timestamp) }
        val dateLabel = SimpleDateFormat("yyyy  /  MM", Locale.getDefault()).format(
            Calendar.getInstance().apply { set(year, month - 1, 1) }.time
        )

        drawText(canvas, "影 笺  ·  MONTHLY MEMORIES", 72f, 88f, 25f, mutedInk, bold = true)
        drawText(canvas, "$month", 72f, 206f, 104f, ink, bold = false)
        drawText(canvas, "月", 210f, 206f, 44f, mutedInk)
        drawText(canvas, "$year 年", 72f, 260f, 34f, mutedInk)
        drawText(canvas, dateLabel, WIDTH - 72f, 260f, 27f, mutedInk, align = Paint.Align.RIGHT)

        val recordedDays = byDay.size
        drawText(canvas, "${monthMemories.size} 条影记    ·    $recordedDays 个记录日", 72f, 324f, 27f, ink)
        drawRule(canvas, 72f, 354f, WIDTH - 72f)

        val margin = 62f
        val gap = 8f
        val cellWidth = (WIDTH - margin * 2 - gap * 6) / 7f
        val weekTop = 390f
        listOf("日", "一", "二", "三", "四", "五", "六").forEachIndexed { index, label ->
            drawText(
                canvas,
                label,
                margin + index * (cellWidth + gap) + cellWidth / 2f,
                weekTop,
                21f,
                mutedInk,
                align = Paint.Align.CENTER,
                bold = true
            )
        }

        val gridTop = 416f
        val cellHeight = 134f
        val cellGap = 10f
        val monthCalendar = Calendar.getInstance().apply { set(year, month - 1, 1) }
        val offset = monthCalendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val dayCount = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val thumbnails = mutableListOf<Bitmap>()
        for (day in 1..dayCount) {
            val index = offset + day - 1
            val row = index / 7
            val col = index % 7
            val left = margin + col * (cellWidth + gap)
            val top = gridTop + row * (cellHeight + cellGap)
            val rect = RectF(left, top, left + cellWidth, top + cellHeight)
            val dayRecords = byDay[day].orEmpty()
            if (dayRecords.isNotEmpty()) {
                val thumbnail = decodeThumbnail(context, dayRecords.first().imageUri)
                if (thumbnail != null) thumbnails += thumbnail
                drawDayPhoto(canvas, rect, day, dayRecords.size, thumbnail)
            } else {
                drawRoundRect(canvas, rect, 18f, cellPaper)
                drawText(canvas, day.toString(), rect.centerX(), rect.centerY() + 11f, 30f, ink, align = Paint.Align.CENTER)
            }
        }
        thumbnails.forEach { it.recycle() }

        drawRule(canvas, 72f, 1300f, WIDTH - 72f)
        drawText(canvas, "把平凡的一天，收进自己的时光里。", 72f, 1350f, 25f, mutedInk)
        drawText(canvas, "YINGJIAN  ·  ${monthMemories.size} MEMORIES", WIDTH - 72f, 1350f, 19f, mutedInk, align = Paint.Align.RIGHT)
        return bitmap
    }

    private fun renderYear(year: Int, memories: List<MemoryRecordEntity>): Bitmap {
        val bitmap = Bitmap.createBitmap(WIDTH, 1920, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(paper)

        val yearMemories = memories.filter { memory -> yearOf(memory.timestamp) == year }
        val recordedDays = yearMemories.map { dayKey(it.timestamp) }.distinct().size
        val activeMonths = yearMemories.map { monthOf(it.timestamp) }.distinct().size
        drawText(canvas, "影 笺  ·  YEAR IN REVIEW", 64f, 84f, 24f, mutedInk, bold = true)
        drawText(canvas, year.toString(), 64f, 198f, 96f, ink)
        drawText(canvas, "这一年，留下了 ${yearMemories.size} 条影记 · $recordedDays 个记录日 · $activeMonths 个有故事的月份", 64f, 260f, 26f, mutedInk)
        drawRule(canvas, 64f, 300f, WIDTH - 64f)

        val margin = 56f
        val gapX = 20f
        val gapY = 20f
        val cardWidth = (WIDTH - margin * 2 - gapX * 2) / 3f
        val cardHeight = 340f
        val startY = 330f
        for (month in 1..12) {
            val index = month - 1
            val row = index / 3
            val col = index % 3
            val left = margin + col * (cardWidth + gapX)
            val top = startY + row * (cardHeight + gapY)
            drawMiniMonth(canvas, RectF(left, top, left + cardWidth, top + cardHeight), year, month, yearMemories)
        }

        drawRule(canvas, 64f, 1810f, WIDTH - 64f)
        drawText(canvas, "每一格日子，都有自己的光。", 64f, 1860f, 25f, mutedInk)
        drawText(canvas, "YINGJIAN  ·  ${yearMemories.size} MEMORIES", WIDTH - 64f, 1860f, 19f, mutedInk, align = Paint.Align.RIGHT)
        return bitmap
    }

    private fun drawMiniMonth(canvas: Canvas, bounds: RectF, year: Int, month: Int, yearMemories: List<MemoryRecordEntity>) {
        drawRoundRect(canvas, bounds, 20f, cellPaper)
        val monthMemories = yearMemories.filter { monthOf(it.timestamp) == month }
        val recordedDays = monthMemories.map { dayOf(it.timestamp) }.toSet()
        val pad = 14f
        drawText(canvas, "${month.toString().padStart(2, '0')} 月", bounds.left + pad, bounds.top + 34f, 23f, ink, bold = true)
        drawText(canvas, "${monthMemories.size}", bounds.right - pad, bounds.top + 34f, 18f, mutedInk, align = Paint.Align.RIGHT)

        val dayLabels = listOf("日", "一", "二", "三", "四", "五", "六")
        val cellWidth = (bounds.width() - pad * 2) / 7f
        val weekdayY = bounds.top + 65f
        dayLabels.forEachIndexed { index, label ->
            drawText(canvas, label, bounds.left + pad + cellWidth * (index + 0.5f), weekdayY, 14f, mutedInk, align = Paint.Align.CENTER)
        }

        val monthCalendar = Calendar.getInstance().apply { set(year, month - 1, 1) }
        val offset = monthCalendar.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
        val dayCount = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val rowHeight = 35f
        val firstRowCenter = bounds.top + 101f
        for (day in 1..dayCount) {
            val index = offset + day - 1
            val row = index / 7
            val col = index % 7
            val centerX = bounds.left + pad + cellWidth * (col + 0.5f)
            val centerY = firstRowCenter + row * rowHeight
            if (day in recordedDays) {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
                canvas.drawCircle(centerX, centerY - 5f, 12.5f, paint)
                drawText(canvas, day.toString(), centerX, centerY, 14f, Color.WHITE, align = Paint.Align.CENTER, bold = true)
            } else {
                drawText(canvas, day.toString(), centerX, centerY, 14f, ink, align = Paint.Align.CENTER)
            }
        }
    }

    private fun drawDayPhoto(canvas: Canvas, rect: RectF, day: Int, count: Int, image: Bitmap?) {
        val path = Path().apply { addRoundRect(rect, 18f, 18f, Path.Direction.CW) }
        val saveCount = canvas.save()
        canvas.clipPath(path)
        if (image != null) {
            val scale = max(rect.width() / image.width, rect.height() / image.height)
            val sourceWidth = rect.width() / scale
            val sourceHeight = rect.height() / scale
            val source = Rect(
                ((image.width - sourceWidth) / 2f).toInt(),
                ((image.height - sourceHeight) / 2f).toInt(),
                ((image.width + sourceWidth) / 2f).toInt(),
                ((image.height + sourceHeight) / 2f).toInt()
            )
            canvas.drawBitmap(image, source, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        } else {
            canvas.drawColor(accent)
        }
        val shade = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(75, 0, 0, 0) }
        canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, shade)
        canvas.restoreToCount(saveCount)
        drawText(canvas, day.toString(), rect.right - 12f, rect.bottom - 12f, 27f, Color.WHITE, align = Paint.Align.RIGHT, bold = true)
        if (count > 1) {
            val badge = RectF(rect.left + 8f, rect.top + 8f, rect.left + 48f, rect.top + 38f)
            drawRoundRect(canvas, badge, 14f, Color.argb(205, 255, 255, 255))
            drawText(canvas, count.toString(), badge.centerX(), badge.centerY() + 6f, 17f, ink, align = Paint.Align.CENTER, bold = true)
        }
    }

    private fun decodeThumbnail(context: Context, uriString: String): Bitmap? = runCatching {
        val uri = Uri.parse(uriString)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val sample = max(1, max(bounds.outWidth, bounds.outHeight) / 360)
        val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()

    private fun dayOf(timestamp: Long) = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.DAY_OF_MONTH)
    private fun monthOf(timestamp: Long) = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.MONTH) + 1
    private fun yearOf(timestamp: Long) = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.YEAR)
    private fun dayKey(timestamp: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return calendar.get(Calendar.YEAR) * 10_000 + (calendar.get(Calendar.MONTH) + 1) * 100 + calendar.get(Calendar.DAY_OF_MONTH)
    }

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        align: Paint.Align = Paint.Align.LEFT,
        bold: Boolean = false
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            textAlign = align
            typeface = Typeface.create(if (bold) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)
        }
        canvas.drawText(text, x, y, paint)
    }

    private fun drawRoundRect(canvas: Canvas, rect: RectF, radius: Float, color: Int) {
        canvas.drawRoundRect(rect, radius, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    private fun drawRule(canvas: Canvas, left: Float, y: Float, right: Float) {
        canvas.drawLine(left, y, right, y, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(218, 210, 199)
            strokeWidth = 2f
        })
    }
}
