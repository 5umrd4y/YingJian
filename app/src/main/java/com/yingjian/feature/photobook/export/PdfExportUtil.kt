package com.yingjian.feature.photobook.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.StaticLayout
import android.text.TextPaint
import com.yingjian.feature.photobook.layout.LayoutInput
import com.yingjian.feature.photobook.layout.SlotRectMm
import com.yingjian.feature.photobook.layout.TemplateLayoutEngine
import com.yingjian.feature.photobook.model.BookState
import com.yingjian.feature.photobook.model.CoverLayout
import com.yingjian.feature.photobook.model.ImageSlot
import com.yingjian.feature.photobook.model.PageState
import com.yingjian.feature.photobook.model.PaperSize
import com.yingjian.feature.photobook.model.PhotobookLayoutDefaults
import com.yingjian.feature.photobook.model.TextElement
import com.yingjian.feature.photobook.model.toPaintAlign
import com.yingjian.feature.photobook.model.toStaticLayoutAlignment
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * Exports a BookState to a 300DPI PDF with crop marks.
 *
 * Page order:
 * 1. Cover page (with title/subtitle/optional image)
 * 2. Content pages (images + text + page numbers)
 * 3. Back cover page (with title/subtitle/date text)
 */
object PdfExportUtil {

    private const val DPI = 300
    private const val CROP_MARK_LENGTH_MM = 5f
    private const val CROP_MARK_STROKE_PT = 0.25f
    private const val PAGE_NUMBER_MARGIN_MM = 12f
    private const val MAX_DECODE_DIMENSION_PX = 4096

    // PDF text color synced with design system onSurfaceVariant (#4c463e)
    private const val PDF_TEXT_COLOR = 0xFF4c463e.toInt()
    private const val PDF_GRAY_TEXT_COLOR = 0xFF9E9E9E.toInt()

    private fun backgroundColorInt(hex: String): Int = runCatching {
        android.graphics.Color.parseColor(hex)
    }.getOrDefault(0xFFFAF9F6.toInt())

    fun exportPdf(context: Context, bookState: BookState): ByteArray {
        val outputStream = ByteArrayOutputStream()
        writePdf(context, bookState, outputStream)
        return outputStream.toByteArray()
    }

    fun writePdf(context: Context, bookState: BookState, outputStream: OutputStream) {
        val document = PdfDocument()

        try {
            // Try to load Noto Sans SC from assets, fall back to system default
            val typeface = try {
                Typeface.createFromAsset(context.assets, "fonts/NotoSansSC-Regular.ttf")
            } catch (e: Exception) {
                Typeface.DEFAULT
            }

            val paperSize = runCatching {
                PaperSize.valueOf(bookState.photobook.paperSize)
            }.getOrDefault(PaperSize.TWELVE_INCH_LANDSCAPE)

            val pageWidthPx = mmToPx(paperSize.widthMm)
            val pageHeightPx = mmToPx(paperSize.heightMm)

            // 1. Cover page
            val coverPageInfo = PdfDocument.PageInfo.Builder(pageWidthPx, pageHeightPx, 1).create()
            val coverPage = document.startPage(coverPageInfo)
            coverPage.canvas.drawColor(backgroundColorInt(bookState.coverLayout.backgroundColor))
            renderCoverLayout(coverPage.canvas, bookState.coverLayout, typeface)
            drawCropMarks(coverPage.canvas, pageWidthPx, pageHeightPx)
            document.finishPage(coverPage)

            // 2. Content pages
            bookState.pages.forEachIndexed { index, pageState ->
                val pageNum = index + 2 // cover is page 1
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPx, pageHeightPx, pageNum).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                canvas.drawColor(0xFFFAF9F6.toInt())

                val slotRects = TemplateLayoutEngine.calculateSlots(
                    LayoutInput(
                        trimWidthMm = pageState.trimWidthMm,
                        trimHeightMm = pageState.trimHeightMm,
                        bleedMm = pageState.bleedMm,
                        safeMarginMm = PhotobookLayoutDefaults.SAFE_MARGIN_MM,
                        bottomTextReserveMm = PhotobookLayoutDefaults.BOTTOM_TEXT_RESERVE_MM,
                        gutterMm = PhotobookLayoutDefaults.GUTTER_MM,
                        template = pageState.template
                    )
                )
                slotRects.forEach { rect ->
                    val slot = pageState.slots.firstOrNull { it.slotId == rect.slotId }
                    if (slot?.imageRef != null) {
                        renderImageSlot(context, canvas, rect, slot)
                    }
                }
                pageState.textElements.sortedBy { it.zIndex }.forEach { element ->
                    renderText(canvas, element, typeface)
                }

                drawCropMarks(canvas, pageWidthPx, pageHeightPx)
                drawPageNumber(canvas, pageState.pageNumber, pageWidthPx, pageHeightPx)

                document.finishPage(page)
            }

            // 3. Back cover page
            val backPageNum = bookState.pages.size + 2
            val backPageInfo = PdfDocument.PageInfo.Builder(pageWidthPx, pageHeightPx, backPageNum).create()
            val backPage = document.startPage(backPageInfo)
            backPage.canvas.drawColor(backgroundColorInt(bookState.backCoverLayout.backgroundColor))
            renderCoverLayout(backPage.canvas, bookState.backCoverLayout, typeface)
            drawCropMarks(backPage.canvas, pageWidthPx, pageHeightPx)
            document.finishPage(backPage)

            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    private fun mmToPx(mm: Float): Int = (mm * DPI / 25.4f).toInt()
    private fun mmToPxFloat(mm: Float): Float = mm * DPI / 25.4f

    private fun renderCoverLayout(canvas: Canvas, layout: CoverLayout, typeface: Typeface) {
        layout.textElements.forEach { element ->
            if (element.role == com.yingjian.feature.photobook.model.CoverTextRole.Divider) {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = PDF_TEXT_COLOR
                    strokeWidth = mmToPxFloat(0.5f)
                }
                val x = mmToPxFloat(element.xMm)
                val y = mmToPxFloat(element.yMm + element.heightMm / 2f)
                val w = mmToPxFloat(element.widthMm)
                canvas.drawLine(x, y, x + w, y, paint)
            } else {
                val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.typeface = typeface
                    textSize = mmToPxFloat(element.fontSizeMm)
                    color = PDF_TEXT_COLOR
                    textAlign = element.textAlign.toPaintAlign()
                }
                val textWidthPx = mmToPxFloat(element.widthMm).toInt()
                val textLeft = mmToPxFloat(element.xMm)
                val textTop = mmToPxFloat(element.yMm)

                val staticLayout = StaticLayout.Builder
                    .obtain(element.text, 0, element.text.length, paint, textWidthPx)
                    .setAlignment(element.textAlign.toStaticLayoutAlignment())
                    .setLineSpacing(0f, 1.2f)
                    .setIncludePad(false)
                    .build()

                canvas.save()
                canvas.translate(textLeft, textTop)
                staticLayout.draw(canvas)
                canvas.restore()
            }
        }
    }

    private fun renderImageSlot(context: Context, canvas: Canvas, rect: SlotRectMm, slot: ImageSlot) {
        runCatching {
            val imageRef = slot.imageRef ?: return@runCatching
            val uri = Uri.parse(imageRef.imageUri)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@runCatching
            val targetWidthPx = mmToPx(rect.widthMm * slot.cropScale).coerceAtLeast(1)
            val targetHeightPx = mmToPx(rect.heightMm * slot.cropScale).coerceAtLeast(1)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            val inSampleSize = PdfImageRenderMath.calculateInSampleSize(
                options.outWidth, options.outHeight,
                targetWidthPx, targetHeightPx,
                MAX_DECODE_DIMENSION_PX
            )

            val bitmapStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(
                bitmapStream,
                null,
                BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
            )
            bitmapStream?.close()
            if (bitmap == null) return@runCatching

            val x = mmToPxFloat(rect.xMm)
            val y = mmToPxFloat(rect.yMm)
            val w = mmToPxFloat(rect.widthMm)
            val h = mmToPxFloat(rect.heightMm)

            canvas.save()
            canvas.clipRect(RectF(x, y, x + w, y + h))
            val dx = mmToPxFloat(slot.cropOffsetX)
            val dy = mmToPxFloat(slot.cropOffsetY)
            val dest = PdfImageRenderMath.destinationRect(
                slotLeft = x,
                slotTop = y,
                slotWidth = w,
                slotHeight = h,
                imageWidth = bitmap.width,
                imageHeight = bitmap.height,
                fitMode = slot.fitMode,
                cropScale = slot.cropScale,
                offsetX = dx,
                offsetY = dy
            )
            canvas.drawBitmap(bitmap, null, RectF(dest.left, dest.top, dest.right, dest.bottom), null)
            canvas.restore()
            bitmap.recycle()
        }
    }

    private fun renderText(canvas: Canvas, element: TextElement, typeface: Typeface) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = element.fontSizeMm * DPI / 25.4f
            color = PDF_TEXT_COLOR
            textAlign = when (element.textAlign) {
                androidx.compose.ui.text.style.TextAlign.Center -> Paint.Align.CENTER
                androidx.compose.ui.text.style.TextAlign.Left -> Paint.Align.LEFT
                androidx.compose.ui.text.style.TextAlign.Right -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
        }

        val x = mmToPxFloat(element.xMm + element.widthMm / 2)
        var y = mmToPxFloat(element.yMm) + paint.textSize

        val textWidthPx = mmToPxFloat(element.widthMm)
        val text = element.text

        val staticLayout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, textWidthPx.toInt())
            .setAlignment(
                when (element.textAlign) {
                    androidx.compose.ui.text.style.TextAlign.Center ->
                        android.text.Layout.Alignment.ALIGN_CENTER
                    androidx.compose.ui.text.style.TextAlign.Left ->
                        android.text.Layout.Alignment.ALIGN_NORMAL
                    androidx.compose.ui.text.style.TextAlign.Right ->
                        android.text.Layout.Alignment.ALIGN_OPPOSITE
                    else -> android.text.Layout.Alignment.ALIGN_CENTER
                }
            )
            .setLineSpacing(0f, 1.2f)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(x, y)
        staticLayout.draw(canvas)
        canvas.restore()
    }

    private fun drawCropMarks(
        canvas: Canvas,
        pageWidthPx: Int,
        pageHeightPx: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            strokeWidth = CROP_MARK_STROKE_PT
            style = Paint.Style.STROKE
        }
        val cropPx = mmToPxFloat(CROP_MARK_LENGTH_MM)

        canvas.drawLine(0f, 0f, cropPx, 0f, paint)
        canvas.drawLine(0f, 0f, 0f, cropPx, paint)
        canvas.drawLine(
            pageWidthPx.toFloat(), 0f,
            pageWidthPx - cropPx, 0f, paint
        )
        canvas.drawLine(
            pageWidthPx.toFloat(), 0f,
            pageWidthPx.toFloat(), cropPx, paint
        )
        canvas.drawLine(
            0f, pageHeightPx.toFloat(),
            cropPx, pageHeightPx.toFloat(), paint
        )
        canvas.drawLine(
            0f, pageHeightPx.toFloat(),
            0f, pageHeightPx - cropPx, paint
        )
        canvas.drawLine(
            pageWidthPx.toFloat(), pageHeightPx.toFloat(),
            pageWidthPx - cropPx, pageHeightPx.toFloat(), paint
        )
        canvas.drawLine(
            pageWidthPx.toFloat(), pageHeightPx.toFloat(),
            pageWidthPx.toFloat(), pageHeightPx - cropPx, paint
        )
    }

    private fun drawPageNumber(
        canvas: Canvas,
        pageNumber: Int,
        pageWidthPx: Int,
        pageHeightPx: Int
    ) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f * DPI / 25.4f
            color = PDF_GRAY_TEXT_COLOR
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.DEFAULT
        }

        val text = "$pageNumber"
        val x = pageWidthPx.toFloat() - mmToPxFloat(PAGE_NUMBER_MARGIN_MM)
        val y = pageHeightPx.toFloat() - mmToPxFloat(PAGE_NUMBER_MARGIN_MM)

        canvas.drawText(text, x, y, paint)
    }
}
