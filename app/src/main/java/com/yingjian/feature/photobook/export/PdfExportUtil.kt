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
import android.text.TextPaint
import android.net.Uri
import com.yingjian.feature.photobook.model.BookState
import com.yingjian.feature.photobook.model.ImageElement
import com.yingjian.feature.photobook.model.PageState
import com.yingjian.feature.photobook.model.PaperSize
import com.yingjian.feature.photobook.model.TextElement
import java.io.ByteArrayOutputStream

/**
 * Exports a BookState to a 300DPI PDF with crop marks.
 *
 * Process:
 * 1. Create PdfDocument.Page at 300DPI for each page
 * 2. Draw white background
 * 3. Draw each PageElement (ImageElement with inSampleSize, TextElement with Noto Sans SC or system fallback)
 * 4. Draw crop marks at 4 corners (5mm black 0.25pt lines)
 */
object PdfExportUtil {

    private const val DPI = 300
    private const val CROP_MARK_LENGTH_MM = 5f
    private const val CROP_MARK_STROKE_PT = 0.25f

    // PDF text color synced with design system onSurfaceVariant (#4c463e)
    private const val PDF_TEXT_COLOR = 0xFF4c463e.toInt()

    fun exportPdf(context: Context, bookState: BookState): ByteArray {
        val document = PdfDocument()

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

        bookState.pages.forEachIndexed { index, pageState ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPx, pageHeightPx, index + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // White background
            canvas.drawColor(Color.WHITE)

            // Render elements sorted by zIndex
            val sortedElements = pageState.elements.sortedBy { it.zIndex }
            sortedElements.forEach { element ->
                when (element) {
                    is ImageElement -> renderImage(context, canvas, element)
                    is TextElement -> renderText(canvas, element, typeface)
                }
            }

            // Crop marks
            drawCropMarks(canvas, pageWidthPx, pageHeightPx)

            document.finishPage(page)
        }

        val outputStream = ByteArrayOutputStream()
        document.writeTo(outputStream)
        document.close()
        return outputStream.toByteArray()
    }

    private fun mmToPx(mm: Float): Int = (mm * DPI / 25.4f).toInt()
    private fun mmToPxFloat(mm: Float): Float = mm * DPI / 25.4f

    private fun renderImage(context: Context, canvas: Canvas, element: ImageElement) {
        val uri = Uri.parse(element.imageUri)
        val inputStream = context.contentResolver.openInputStream(uri) ?: return

        // Calculate inSampleSize to avoid OOM
        val targetWidthPx = mmToPx(element.widthMm)
        val targetHeightPx = mmToPx(element.heightMm)

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        val inSampleSize = calculateInSampleSize(
            options.outWidth, options.outHeight,
            targetWidthPx, targetHeightPx
        )

        val bitmapOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        val bitmapStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(bitmapStream, null, bitmapOptions)
        bitmapStream?.close()

        if (bitmap == null) return

        val x = mmToPxFloat(element.xMm)
        val y = mmToPxFloat(element.yMm)
        val w = mmToPxFloat(element.widthMm)
        val h = mmToPxFloat(element.heightMm)

        if (element.rotationDeg != 0f) {
            canvas.save()
            canvas.rotate(element.rotationDeg, x + w / 2, y + h / 2)
            canvas.drawBitmap(bitmap, null, RectF(x, y, x + w, y + h), null)
            canvas.restore()
        } else {
            canvas.drawBitmap(bitmap, null, RectF(x, y, x + w, y + h), null)
        }

        bitmap.recycle()
    }

    private fun calculateInSampleSize(
        reqWidth: Int,
        reqHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        var inSampleSize = 1
        if (reqHeight > targetHeight || reqWidth > targetWidth) {
            val halfHeight = reqHeight / 2
            val halfWidth = reqWidth / 2
            while ((halfHeight / inSampleSize) >= targetHeight &&
                (halfWidth / inSampleSize) >= targetWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
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

        val staticLayout = android.text.StaticLayout.Builder
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

        // Top-left
        canvas.drawLine(0f, 0f, cropPx, 0f, paint)
        canvas.drawLine(0f, 0f, 0f, cropPx, paint)
        // Top-right
        canvas.drawLine(
            pageWidthPx.toFloat(), 0f,
            pageWidthPx - cropPx, 0f, paint
        )
        canvas.drawLine(
            pageWidthPx.toFloat(), 0f,
            pageWidthPx.toFloat(), cropPx, paint
        )
        // Bottom-left
        canvas.drawLine(
            0f, pageHeightPx.toFloat(),
            cropPx, pageHeightPx.toFloat(), paint
        )
        canvas.drawLine(
            0f, pageHeightPx.toFloat(),
            0f, pageHeightPx - cropPx, paint
        )
        // Bottom-right
        canvas.drawLine(
            pageWidthPx.toFloat(), pageHeightPx.toFloat(),
            pageWidthPx - cropPx, pageHeightPx.toFloat(), paint
        )
        canvas.drawLine(
            pageWidthPx.toFloat(), pageHeightPx.toFloat(),
            pageWidthPx.toFloat(), pageHeightPx - cropPx, paint
        )
    }
}
