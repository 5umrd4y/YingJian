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
import com.yingjian.feature.photobook.displayBackDateText
import com.yingjian.feature.photobook.displayBackSubtitle
import com.yingjian.feature.photobook.displayBackTitle
import com.yingjian.feature.photobook.displayCoverSubtitle
import com.yingjian.feature.photobook.displayCoverTitle
import com.yingjian.feature.photobook.model.BookState
import com.yingjian.feature.photobook.model.ImageElement
import com.yingjian.feature.photobook.model.PageState
import com.yingjian.feature.photobook.model.PaperSize
import com.yingjian.feature.photobook.model.TextElement
import java.io.ByteArrayOutputStream

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

    // PDF text color synced with design system onSurfaceVariant (#4c463e)
    private const val PDF_TEXT_COLOR = 0xFF4c463e.toInt()
    private const val PDF_GRAY_TEXT_COLOR = 0xFF9E9E9E.toInt()

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

        // 1. Cover page
        val coverPageInfo = PdfDocument.PageInfo.Builder(pageWidthPx, pageHeightPx, 1).create()
        val coverPage = document.startPage(coverPageInfo)
        coverPage.canvas.drawColor(0xFFFAF9F6.toInt())
        renderCoverPage(context, coverPage.canvas, bookState, paperSize, typeface)
        drawCropMarks(coverPage.canvas, pageWidthPx, pageHeightPx)
        document.finishPage(coverPage)

        // 2. Content pages
        bookState.pages.forEachIndexed { index, pageState ->
            val pageNum = index + 2 // cover is page 1
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPx, pageHeightPx, pageNum).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(0xFFFAF9F6.toInt())

            val sortedElements = pageState.elements.sortedBy { it.zIndex }
            sortedElements.forEach { element ->
                when (element) {
                    is ImageElement -> renderImage(context, canvas, element)
                    is TextElement -> renderText(canvas, element, typeface)
                }
            }

            drawCropMarks(canvas, pageWidthPx, pageHeightPx)
            drawPageNumber(canvas, pageState.pageNumber, pageWidthPx, pageHeightPx)

            document.finishPage(page)
        }

        // 3. Back cover page
        val backPageNum = bookState.pages.size + 2
        val backPageInfo = PdfDocument.PageInfo.Builder(pageWidthPx, pageHeightPx, backPageNum).create()
        val backPage = document.startPage(backPageInfo)
        backPage.canvas.drawColor(0xFFFAF9F6.toInt())
        renderBackPage(backPage.canvas, bookState, paperSize, typeface)
        drawCropMarks(backPage.canvas, pageWidthPx, pageHeightPx)
        document.finishPage(backPage)

        val outputStream = ByteArrayOutputStream()
        document.writeTo(outputStream)
        document.close()
        return outputStream.toByteArray()
    }

    private fun mmToPx(mm: Float): Int = (mm * DPI / 25.4f).toInt()
    private fun mmToPxFloat(mm: Float): Float = mm * DPI / 25.4f

    private fun renderCoverPage(
        context: Context,
        canvas: Canvas,
        bookState: BookState,
        paperSize: PaperSize,
        typeface: Typeface
    ) {
        val photobook = bookState.photobook

        // Render cover image if set
        photobook.coverImageUri?.let { uriString ->
            runCatching {
                val uri = Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream.close()

                    val imageAreaWidthMm = paperSize.widthMm * 0.7f
                    val imageAreaHeightMm = paperSize.heightMm * 0.5f
                    val targetWidthPx = mmToPx(imageAreaWidthMm)
                    val targetHeightPx = mmToPx(imageAreaHeightMm)

                    val inSampleSize = calculateInSampleSize(
                        options.outWidth, options.outHeight,
                        targetWidthPx, targetHeightPx
                    )

                    val bitmapStream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(bitmapStream, null, BitmapFactory.Options().apply { this.inSampleSize = inSampleSize })
                    bitmapStream?.close()

                    if (bitmap != null) {
                        val bitmapAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                        var drawWidthMm = imageAreaWidthMm
                        var drawHeightMm = drawWidthMm / bitmapAspect
                        if (drawHeightMm > imageAreaHeightMm) {
                            drawHeightMm = imageAreaHeightMm
                            drawWidthMm = drawHeightMm * bitmapAspect
                        }

                        val areaXMm = (paperSize.widthMm - imageAreaWidthMm) / 2
                        val areaYMm = paperSize.heightMm * 0.1f
                        val x = mmToPxFloat(areaXMm + (imageAreaWidthMm - drawWidthMm) / 2)
                        val y = mmToPxFloat(areaYMm + (imageAreaHeightMm - drawHeightMm) / 2)
                        val w = mmToPxFloat(drawWidthMm)
                        val h = mmToPxFloat(drawHeightMm)
                        canvas.drawBitmap(bitmap, null, RectF(x, y, x + w, y + h), null)
                        bitmap.recycle()
                    }
                }
            }
        }

        // Cover title
        val titleText = photobook.displayCoverTitle()
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = mmToPxFloat(10f)
            color = PDF_TEXT_COLOR
            textAlign = Paint.Align.CENTER
        }
        val titleY = if (photobook.coverImageUri != null) {
            mmToPxFloat(paperSize.heightMm * 0.65f)
        } else {
            mmToPxFloat(paperSize.heightMm * 0.45f)
        }
        val centerX = mmToPxFloat(paperSize.widthMm / 2)

        val titleWidth = mmToPxFloat(paperSize.widthMm * 0.7f).toInt()
        val titleLeft = (mmToPxFloat(paperSize.widthMm) - titleWidth) / 2f
        val titleLayout = StaticLayout.Builder
            .obtain(titleText, 0, titleText.length, titlePaint, titleWidth)
            .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.3f)
            .setIncludePad(false)
            .build()

        canvas.save()
        canvas.translate(titleLeft, titleY)
        titleLayout.draw(canvas)
        canvas.restore()

        // Cover subtitle
        val subtitleText = photobook.displayCoverSubtitle()
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = mmToPxFloat(7f)
            color = PDF_TEXT_COLOR
            textAlign = Paint.Align.CENTER
        }
        val subtitleY = titleY + titleLayout.height + mmToPxFloat(5f)

        canvas.drawText(subtitleText, centerX, subtitleY, subtitlePaint)
    }

    private fun renderBackPage(
        canvas: Canvas,
        bookState: BookState,
        paperSize: PaperSize,
        typeface: Typeface
    ) {
        val photobook = bookState.photobook
        val centerX = mmToPxFloat(paperSize.widthMm / 2)
        var y = mmToPxFloat(paperSize.heightMm * 0.4f)

        // Back title
        val titleText = photobook.displayBackTitle()
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = mmToPxFloat(9f)
            color = PDF_TEXT_COLOR
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(titleText, centerX, y, titlePaint)
        y += mmToPxFloat(12f)

        // Back subtitle
        val subtitleText = photobook.displayBackSubtitle()
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            textSize = mmToPxFloat(7f)
            color = PDF_TEXT_COLOR
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(subtitleText, centerX, y, subtitlePaint)

        // Back date text (optional)
        val dateText = photobook.displayBackDateText()
        if (dateText.isNotBlank()) {
            y += mmToPxFloat(10f)
            val datePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                this.typeface = typeface
                textSize = mmToPxFloat(6f)
                color = PDF_GRAY_TEXT_COLOR
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(dateText, centerX, y, datePaint)
        }
    }

    private fun renderImage(context: Context, canvas: Canvas, element: ImageElement) {
        val uri = Uri.parse(element.imageUri)
        val inputStream = context.contentResolver.openInputStream(uri) ?: return

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
