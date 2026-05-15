# Photobook Slot Layout Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the photobook module around fixed page templates, image slots, photo-level memory selection, saved crop/zoom, unified preview, and unified PDF layout.

**Architecture:** Add a slot-based page document model and make it the only persisted content-page format. A shared `TemplateLayoutEngine` calculates slot rectangles in millimeters, and editor, preview, and PDF consume those rectangles instead of duplicating layout math. UI actions operate on selected slots, not first image elements.

**Tech Stack:** Kotlin, Jetpack Compose, Room, kotlinx.serialization, Android PdfDocument, JUnit 4.

---

## Scope Check

This plan covers one bounded subsystem: photobook page composition. It touches model, serialization, editor, picker, preview, and PDF because those currently share the same flawed assumptions. Do not split this into independent projects; each task below produces a compileable checkpoint.

## File Responsibility Map

- Create `app/src/main/java/com/yingjian/feature/photobook/model/ImageRef.kt`: photo-level source reference for one image from one memory.
- Create `app/src/main/java/com/yingjian/feature/photobook/model/ImageSlot.kt`: slot image state and crop transform.
- Create `app/src/main/java/com/yingjian/feature/photobook/model/PageTemplate.kt`: fixed content-page layout types and slot ids.
- Create `app/src/main/java/com/yingjian/feature/photobook/model/PageLayoutDocument.kt`: persisted page document.
- Modify `app/src/main/java/com/yingjian/feature/photobook/model/BookState.kt`: replace element-index selection with slot selection and add template/slot fields to `PageState`.
- Create `app/src/main/java/com/yingjian/core/util/PageLayoutDocumentSerializer.kt`: JSON serializer for new page layout documents.
- Modify `app/src/main/java/com/yingjian/feature/photobook/layout/AutoLayoutAlgorithm.kt`: create one `Single` page per selected photo.
- Create `app/src/main/java/com/yingjian/feature/photobook/layout/TemplateLayoutEngine.kt`: shared slot rectangle calculation.
- Create `app/src/main/java/com/yingjian/feature/photobook/layout/SlotImageTransform.kt`: pure crop/zoom math shared by Compose and PDF.
- Create `app/src/main/java/com/yingjian/feature/photobook/PhotobookSlotActions.kt`: pure page mutation functions for template change, move, swap, delete, reset crop.
- Modify `app/src/main/java/com/yingjian/feature/photobook/PhotoPickerScreen.kt`: support batch import and single-slot modes.
- Modify `app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt`: use selected photos instead of selected memory ids and serialize page documents.
- Modify `app/src/main/java/com/yingjian/feature/photobook/PhotobookCanvasPage.kt`: render slots from the layout engine and persist slot crop gestures.
- Modify `app/src/main/java/com/yingjian/feature/photobook/PhotobookEditorScreen.kt`: add template controls, slot commands, move/swap dialogs.
- Modify `app/src/main/java/com/yingjian/feature/photobook/PhotobookPreviewScreen.kt`: keep leaf-based preview and make content leaves use slot renderer.
- Modify `app/src/main/java/com/yingjian/feature/photobook/export/PdfExportUtil.kt`: render slots from the layout engine and remove cover image rendering.
- Add tests under `app/src/test/java/com/yingjian/feature/photobook/`.

## Task 1: Add Slot-Based Domain Model

**Files:**
- Create: `app/src/main/java/com/yingjian/feature/photobook/model/PageTemplate.kt`
- Create: `app/src/main/java/com/yingjian/feature/photobook/model/ImageRef.kt`
- Create: `app/src/main/java/com/yingjian/feature/photobook/model/ImageSlot.kt`
- Create: `app/src/main/java/com/yingjian/feature/photobook/model/PageLayoutDocument.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/model/BookState.kt`
- Test: `app/src/test/java/com/yingjian/feature/photobook/model/PageTemplateTest.kt`

- [ ] **Step 1: Write tests for template slot ids**

Create `app/src/test/java/com/yingjian/feature/photobook/model/PageTemplateTest.kt`:

```kotlin
package com.yingjian.feature.photobook.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PageTemplateTest {
    @Test
    fun `slot ids are stable and ordered`() {
        assertEquals(listOf("slot-1"), PageTemplate.Single.slotIds)
        assertEquals(listOf("slot-1", "slot-2"), PageTemplate.TwoHorizontal.slotIds)
        assertEquals(listOf("slot-1", "slot-2"), PageTemplate.TwoVertical.slotIds)
        assertEquals(listOf("slot-1", "slot-2", "slot-3", "slot-4"), PageTemplate.GridFour.slotIds)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest --tests com.yingjian.feature.photobook.model.PageTemplateTest
```

Expected: FAIL because `PageTemplate` does not exist.

- [ ] **Step 3: Add `PageTemplate`**

Create `app/src/main/java/com/yingjian/feature/photobook/model/PageTemplate.kt`:

```kotlin
package com.yingjian.feature.photobook.model

enum class PageTemplate(val slotIds: List<String>) {
    Single(listOf("slot-1")),
    TwoHorizontal(listOf("slot-1", "slot-2")),
    TwoVertical(listOf("slot-1", "slot-2")),
    GridFour(listOf("slot-1", "slot-2", "slot-3", "slot-4"));

    val capacity: Int get() = slotIds.size
}
```

- [ ] **Step 4: Add image reference and slot model**

Create `app/src/main/java/com/yingjian/feature/photobook/model/ImageRef.kt`:

```kotlin
package com.yingjian.feature.photobook.model

data class ImageRef(
    val memoryId: Long,
    val imageUri: String,
    val sourceImageIndex: Int?,
    val sourceImageId: Long? = null
)
```

Create `app/src/main/java/com/yingjian/feature/photobook/model/ImageSlot.kt`:

```kotlin
package com.yingjian.feature.photobook.model

enum class FitMode {
    Crop,
    Fit
}

data class ImageSlot(
    val slotId: String,
    val imageRef: ImageRef?,
    val cropScale: Float = 1f,
    val cropOffsetX: Float = 0f,
    val cropOffsetY: Float = 0f,
    val fitMode: FitMode = FitMode.Crop
) {
    val isEmpty: Boolean get() = imageRef == null
}
```

- [ ] **Step 5: Add persisted page document**

Create `app/src/main/java/com/yingjian/feature/photobook/model/PageLayoutDocument.kt`:

```kotlin
package com.yingjian.feature.photobook.model

data class PageLayoutDocument(
    val version: Int = 1,
    val template: PageTemplate,
    val slots: List<ImageSlot>,
    val textElements: List<TextElement> = emptyList()
)
```

- [ ] **Step 6: Update `BookState.kt`**

Replace `PageState` and `BookState` in `app/src/main/java/com/yingjian/feature/photobook/model/BookState.kt` with:

```kotlin
package com.yingjian.feature.photobook.model

enum class LayoutMode { AUTO, MANUAL }

enum class PaperSize(val widthMm: Float, val heightMm: Float) {
    TWELVE_INCH_LANDSCAPE(285f, 210f)
}

data class PageState(
    val pageNumber: Int,
    val template: PageTemplate,
    val slots: List<ImageSlot>,
    val textElements: List<TextElement> = emptyList(),
    val trimWidthMm: Float,
    val trimHeightMm: Float,
    val bleedMm: Float = 3.0f
) {
    fun toDocument(): PageLayoutDocument = PageLayoutDocument(
        template = template,
        slots = slots,
        textElements = textElements
    )
}

data class EditorSelection(
    val leafIndex: Int,
    val pageIndex: Int?,
    val slotId: String?
)

data class BookState(
    val photobook: com.yingjian.core.data.database.PhotobookEntity,
    val pages: List<PageState>,
    val currentPage: Int,
    val mode: LayoutMode,
    val selectedSlotId: String? = null,
    val previousManualState: BookState? = null
)
```

- [ ] **Step 7: Run model tests**

Run:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest --tests com.yingjian.feature.photobook.model.PageTemplateTest
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/model app/src/test/java/com/yingjian/feature/photobook/model/PageTemplateTest.kt
git commit -m "feat: add photobook slot layout model"
```

## Task 2: Add Page Document Serialization

**Files:**
- Create: `app/src/main/java/com/yingjian/core/util/PageLayoutDocumentSerializer.kt`
- Modify: `app/src/main/java/com/yingjian/core/data/database/Converters.kt`
- Test: `app/src/test/java/com/yingjian/core/util/PageLayoutDocumentSerializerTest.kt`

- [ ] **Step 1: Write serializer round-trip test**

Create `app/src/test/java/com/yingjian/core/util/PageLayoutDocumentSerializerTest.kt`:

```kotlin
package com.yingjian.core.util

import com.yingjian.feature.photobook.model.FitMode
import com.yingjian.feature.photobook.model.ImageRef
import com.yingjian.feature.photobook.model.ImageSlot
import com.yingjian.feature.photobook.model.PageLayoutDocument
import com.yingjian.feature.photobook.model.PageTemplate
import org.junit.Assert.assertEquals
import org.junit.Test

class PageLayoutDocumentSerializerTest {
    @Test
    fun `serializes and deserializes slot crop state`() {
        val document = PageLayoutDocument(
            template = PageTemplate.GridFour,
            slots = listOf(
                ImageSlot(
                    slotId = "slot-1",
                    imageRef = ImageRef(
                        memoryId = 7L,
                        imageUri = "content://memory/7/1",
                        sourceImageIndex = 1
                    ),
                    cropScale = 1.35f,
                    cropOffsetX = -2.5f,
                    cropOffsetY = 4.25f,
                    fitMode = FitMode.Crop
                )
            )
        )

        val json = PageLayoutDocumentSerializer.serialize(document)
        val decoded = PageLayoutDocumentSerializer.deserialize(json)

        assertEquals(PageTemplate.GridFour, decoded.template)
        assertEquals("slot-1", decoded.slots.first().slotId)
        assertEquals("content://memory/7/1", decoded.slots.first().imageRef?.imageUri)
        assertEquals(1.35f, decoded.slots.first().cropScale, 0.001f)
        assertEquals(-2.5f, decoded.slots.first().cropOffsetX, 0.001f)
        assertEquals(4.25f, decoded.slots.first().cropOffsetY, 0.001f)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest --tests com.yingjian.core.util.PageLayoutDocumentSerializerTest
```

Expected: FAIL because `PageLayoutDocumentSerializer` does not exist.

- [ ] **Step 3: Add serializer DTOs**

Create `app/src/main/java/com/yingjian/core/util/PageLayoutDocumentSerializer.kt`:

```kotlin
package com.yingjian.core.util

import androidx.compose.ui.text.style.TextAlign
import com.yingjian.feature.photobook.model.FitMode
import com.yingjian.feature.photobook.model.ImageRef
import com.yingjian.feature.photobook.model.ImageSlot
import com.yingjian.feature.photobook.model.PageLayoutDocument
import com.yingjian.feature.photobook.model.PageTemplate
import com.yingjian.feature.photobook.model.TextElement
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class PageLayoutDocumentDto(
    val version: Int = 1,
    val template: String,
    val slots: List<ImageSlotDto>,
    val textElements: List<TextElementDto> = emptyList()
)

@Serializable
private data class ImageSlotDto(
    val slotId: String,
    val imageRef: ImageRefDto?,
    val cropScale: Float = 1f,
    val cropOffsetX: Float = 0f,
    val cropOffsetY: Float = 0f,
    val fitMode: String = FitMode.Crop.name
)

@Serializable
private data class ImageRefDto(
    val memoryId: Long,
    val imageUri: String,
    val sourceImageIndex: Int?,
    val sourceImageId: Long? = null
)

object PageLayoutDocumentSerializer {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun serialize(document: PageLayoutDocument): String = json.encodeToString(document.toDto())

    fun deserialize(raw: String): PageLayoutDocument = json.decodeFromString<PageLayoutDocumentDto>(raw).toDomain()

    private fun PageLayoutDocument.toDto(): PageLayoutDocumentDto = PageLayoutDocumentDto(
        version = version,
        template = template.name,
        slots = slots.map { it.toDto() },
        textElements = textElements.map { it.toDto() }
    )

    private fun ImageSlot.toDto(): ImageSlotDto = ImageSlotDto(
        slotId = slotId,
        imageRef = imageRef?.let {
            ImageRefDto(
                memoryId = it.memoryId,
                imageUri = it.imageUri,
                sourceImageIndex = it.sourceImageIndex,
                sourceImageId = it.sourceImageId
            )
        },
        cropScale = cropScale,
        cropOffsetX = cropOffsetX,
        cropOffsetY = cropOffsetY,
        fitMode = fitMode.name
    )

    private fun PageLayoutDocumentDto.toDomain(): PageLayoutDocument = PageLayoutDocument(
        version = version,
        template = PageTemplate.valueOf(template),
        slots = slots.map { it.toDomain() },
        textElements = textElements.map { it.toDomain() }
    )

    private fun ImageSlotDto.toDomain(): ImageSlot = ImageSlot(
        slotId = slotId,
        imageRef = imageRef?.let {
            ImageRef(
                memoryId = it.memoryId,
                imageUri = it.imageUri,
                sourceImageIndex = it.sourceImageIndex,
                sourceImageId = it.sourceImageId
            )
        },
        cropScale = cropScale,
        cropOffsetX = cropOffsetX,
        cropOffsetY = cropOffsetY,
        fitMode = FitMode.valueOf(fitMode)
    )

    private fun TextElement.toDto(): TextElementDto = TextElementDto(
        text = text,
        fontSizeMm = fontSizeMm,
        textAlign = when (textAlign) {
            TextAlign.Left -> "Left"
            TextAlign.Right -> "Right"
            TextAlign.Center -> "Center"
            TextAlign.Justify -> "Justify"
            TextAlign.Start -> "Start"
            TextAlign.End -> "End"
            else -> "Center"
        },
        xMm = xMm,
        yMm = yMm,
        widthMm = widthMm,
        heightMm = heightMm,
        rotationDeg = rotationDeg,
        zIndex = zIndex
    )
}
```

- [ ] **Step 4: Reuse existing `TextElementDto` domain conversion**

In `app/src/main/java/com/yingjian/core/util/ElementSerializer.kt`, change `TextElementDto` from `data class` private package default to reusable internal by leaving it top-level and adding this extension below `ElementSerializer`:

```kotlin
internal fun TextElementDto.toDomain(): TextElement = TextElement(
    text = text,
    fontSizeMm = fontSizeMm,
    textAlign = when (textAlign) {
        "Left" -> TextAlign.Left
        "Right" -> TextAlign.Right
        "Center" -> TextAlign.Center
        "Justify" -> TextAlign.Justify
        "Start" -> TextAlign.Start
        "End" -> TextAlign.End
        else -> TextAlign.Center
    },
    xMm = xMm,
    yMm = yMm,
    widthMm = widthMm,
    heightMm = heightMm,
    rotationDeg = rotationDeg,
    zIndex = zIndex
)
```

If keeping `ElementSerializer` causes name collisions, move `TextElementDto` into `PageLayoutDocumentSerializer.kt` and duplicate only this small DTO. Do not keep old element serialization as the default path for photobook page layouts.

- [ ] **Step 5: Run serializer test**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest --tests com.yingjian.core.util.PageLayoutDocumentSerializerTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/yingjian/core/util/PageLayoutDocumentSerializer.kt app/src/main/java/com/yingjian/core/util/ElementSerializer.kt app/src/test/java/com/yingjian/core/util/PageLayoutDocumentSerializerTest.kt
git commit -m "feat: serialize photobook page documents"
```

## Task 3: Add Shared Template Layout Engine

**Files:**
- Create: `app/src/main/java/com/yingjian/feature/photobook/layout/TemplateLayoutEngine.kt`
- Create: `app/src/main/java/com/yingjian/feature/photobook/layout/SlotImageTransform.kt`
- Test: `app/src/test/java/com/yingjian/feature/photobook/layout/TemplateLayoutEngineTest.kt`
- Test: `app/src/test/java/com/yingjian/feature/photobook/layout/SlotImageTransformTest.kt`

- [ ] **Step 1: Write layout engine tests**

Create `app/src/test/java/com/yingjian/feature/photobook/layout/TemplateLayoutEngineTest.kt`:

```kotlin
package com.yingjian.feature.photobook.layout

import com.yingjian.feature.photobook.model.PageTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateLayoutEngineTest {
    @Test
    fun `single template returns one centered slot inside safe area`() {
        val slots = TemplateLayoutEngine.calculateSlots(
            LayoutInput(
                trimWidthMm = 285f,
                trimHeightMm = 210f,
                bleedMm = 3f,
                safeMarginMm = 16f,
                template = PageTemplate.Single
            )
        )

        assertEquals(1, slots.size)
        assertEquals("slot-1", slots.first().slotId)
        assertTrue(slots.first().xMm >= 16f)
        assertTrue(slots.first().yMm >= 16f)
        assertTrue(slots.first().xMm + slots.first().widthMm <= 285f - 16f)
        assertTrue(slots.first().yMm + slots.first().heightMm <= 210f - 28f)
    }

    @Test
    fun `grid four template returns four non-overlapping slots`() {
        val slots = TemplateLayoutEngine.calculateSlots(
            LayoutInput(285f, 210f, 3f, 16f, PageTemplate.GridFour)
        )

        assertEquals(listOf("slot-1", "slot-2", "slot-3", "slot-4"), slots.map { it.slotId })
        assertTrue(slots[0].xMm < slots[1].xMm)
        assertEquals(slots[0].yMm, slots[1].yMm, 0.001f)
        assertEquals(slots[2].yMm, slots[3].yMm, 0.001f)
        assertTrue(slots[2].yMm > slots[0].yMm)
    }
}
```

- [ ] **Step 2: Write crop transform tests**

Create `app/src/test/java/com/yingjian/feature/photobook/layout/SlotImageTransformTest.kt`:

```kotlin
package com.yingjian.feature.photobook.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class SlotImageTransformTest {
    @Test
    fun `clamps crop scale to minimum one`() {
        val result = SlotImageTransform.clampScale(0.5f)
        assertEquals(1f, result, 0.001f)
    }

    @Test
    fun `clamps crop scale to maximum three`() {
        val result = SlotImageTransform.clampScale(4.2f)
        assertEquals(3f, result, 0.001f)
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest --tests com.yingjian.feature.photobook.layout.TemplateLayoutEngineTest --tests com.yingjian.feature.photobook.layout.SlotImageTransformTest
```

Expected: FAIL because the new layout classes do not exist.

- [ ] **Step 4: Add `TemplateLayoutEngine`**

Create `app/src/main/java/com/yingjian/feature/photobook/layout/TemplateLayoutEngine.kt`:

```kotlin
package com.yingjian.feature.photobook.layout

import com.yingjian.feature.photobook.model.PageTemplate

data class LayoutInput(
    val trimWidthMm: Float,
    val trimHeightMm: Float,
    val bleedMm: Float,
    val safeMarginMm: Float,
    val template: PageTemplate
)

data class SlotRectMm(
    val slotId: String,
    val xMm: Float,
    val yMm: Float,
    val widthMm: Float,
    val heightMm: Float
)

object TemplateLayoutEngine {
    private const val GUTTER_MM = 8f
    private const val BOTTOM_TEXT_RESERVE_MM = 20f

    fun calculateSlots(input: LayoutInput): List<SlotRectMm> {
        val left = maxOf(input.bleedMm, input.safeMarginMm)
        val top = maxOf(input.bleedMm, input.safeMarginMm)
        val right = input.trimWidthMm - maxOf(input.bleedMm, input.safeMarginMm)
        val bottom = input.trimHeightMm - maxOf(input.bleedMm, input.safeMarginMm) - BOTTOM_TEXT_RESERVE_MM
        val width = right - left
        val height = bottom - top

        return when (input.template) {
            PageTemplate.Single -> listOf(
                SlotRectMm("slot-1", left, top, width, height)
            )
            PageTemplate.TwoHorizontal -> {
                val slotHeight = (height - GUTTER_MM) / 2f
                listOf(
                    SlotRectMm("slot-1", left, top, width, slotHeight),
                    SlotRectMm("slot-2", left, top + slotHeight + GUTTER_MM, width, slotHeight)
                )
            }
            PageTemplate.TwoVertical -> {
                val slotWidth = (width - GUTTER_MM) / 2f
                listOf(
                    SlotRectMm("slot-1", left, top, slotWidth, height),
                    SlotRectMm("slot-2", left + slotWidth + GUTTER_MM, top, slotWidth, height)
                )
            }
            PageTemplate.GridFour -> {
                val slotWidth = (width - GUTTER_MM) / 2f
                val slotHeight = (height - GUTTER_MM) / 2f
                listOf(
                    SlotRectMm("slot-1", left, top, slotWidth, slotHeight),
                    SlotRectMm("slot-2", left + slotWidth + GUTTER_MM, top, slotWidth, slotHeight),
                    SlotRectMm("slot-3", left, top + slotHeight + GUTTER_MM, slotWidth, slotHeight),
                    SlotRectMm("slot-4", left + slotWidth + GUTTER_MM, top + slotHeight + GUTTER_MM, slotWidth, slotHeight)
                )
            }
        }
    }
}
```

- [ ] **Step 5: Add `SlotImageTransform`**

Create `app/src/main/java/com/yingjian/feature/photobook/layout/SlotImageTransform.kt`:

```kotlin
package com.yingjian.feature.photobook.layout

object SlotImageTransform {
    const val MIN_CROP_SCALE = 1f
    const val MAX_CROP_SCALE = 3f

    fun clampScale(value: Float): Float = value.coerceIn(MIN_CROP_SCALE, MAX_CROP_SCALE)

    fun clampOffset(valueMm: Float, slotSizeMm: Float, cropScale: Float): Float {
        val extra = (slotSizeMm * cropScale - slotSizeMm) / 2f
        return valueMm.coerceIn(-extra, extra)
    }
}
```

- [ ] **Step 6: Run layout tests**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest --tests com.yingjian.feature.photobook.layout.TemplateLayoutEngineTest --tests com.yingjian.feature.photobook.layout.SlotImageTransformTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/layout/TemplateLayoutEngine.kt app/src/main/java/com/yingjian/feature/photobook/layout/SlotImageTransform.kt app/src/test/java/com/yingjian/feature/photobook/layout/TemplateLayoutEngineTest.kt app/src/test/java/com/yingjian/feature/photobook/layout/SlotImageTransformTest.kt
git commit -m "feat: add photobook template layout engine"
```

## Task 4: Add Pure Slot Actions

**Files:**
- Create: `app/src/main/java/com/yingjian/feature/photobook/PhotobookSlotActions.kt`
- Test: `app/src/test/java/com/yingjian/feature/photobook/PhotobookSlotActionsTest.kt`

- [ ] **Step 1: Write action tests**

Create `app/src/test/java/com/yingjian/feature/photobook/PhotobookSlotActionsTest.kt`:

```kotlin
package com.yingjian.feature.photobook

import com.yingjian.feature.photobook.model.ImageRef
import com.yingjian.feature.photobook.model.ImageSlot
import com.yingjian.feature.photobook.model.PageState
import com.yingjian.feature.photobook.model.PageTemplate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotobookSlotActionsTest {
    @Test
    fun `single to grid keeps image in first slot`() {
        val page = page(PageTemplate.Single, listOf(slot("slot-1", 1)))

        val result = PhotobookSlotActions.changeTemplate(page, PageTemplate.GridFour)

        val updated = result as TemplateChangeResult.Changed
        assertEquals(PageTemplate.GridFour, updated.page.template)
        assertEquals(4, updated.page.slots.size)
        assertEquals("content://image/1", updated.page.slots[0].imageRef?.imageUri)
        assertNull(updated.page.slots[1].imageRef)
    }

    @Test
    fun `move fills first empty target slot and clears source`() {
        val source = page(PageTemplate.Single, listOf(slot("slot-1", 1)))
        val target = page(PageTemplate.GridFour, listOf(slot("slot-1", 2), empty("slot-2"), empty("slot-3"), empty("slot-4")), pageNumber = 2)

        val result = PhotobookSlotActions.moveImage(source, "slot-1", target)

        val moved = result as MoveResult.Moved
        assertNull(moved.sourcePage.slots.first().imageRef)
        assertEquals("content://image/1", moved.targetPage.slots[1].imageRef?.imageUri)
    }

    @Test
    fun `move to full page returns target full`() {
        val source = page(PageTemplate.Single, listOf(slot("slot-1", 1)))
        val target = page(PageTemplate.TwoVertical, listOf(slot("slot-1", 2), slot("slot-2", 3)), pageNumber = 2)

        val result = PhotobookSlotActions.moveImage(source, "slot-1", target)

        assertEquals(MoveResult.TargetFull, result)
    }

    private fun page(template: PageTemplate, slots: List<ImageSlot>, pageNumber: Int = 1) = PageState(
        pageNumber = pageNumber,
        template = template,
        slots = slots,
        trimWidthMm = 285f,
        trimHeightMm = 210f
    )

    private fun slot(id: String, memoryId: Long) = ImageSlot(
        slotId = id,
        imageRef = ImageRef(memoryId, "content://image/$memoryId", 0)
    )

    private fun empty(id: String) = ImageSlot(slotId = id, imageRef = null)
}
```

- [ ] **Step 2: Run tests to verify they fail**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest --tests com.yingjian.feature.photobook.PhotobookSlotActionsTest
```

Expected: FAIL because `PhotobookSlotActions` does not exist.

- [ ] **Step 3: Add action implementation**

Create `app/src/main/java/com/yingjian/feature/photobook/PhotobookSlotActions.kt`:

```kotlin
package com.yingjian.feature.photobook

import com.yingjian.feature.photobook.model.ImageSlot
import com.yingjian.feature.photobook.model.PageState
import com.yingjian.feature.photobook.model.PageTemplate

sealed interface TemplateChangeResult {
    data class Changed(val page: PageState) : TemplateChangeResult
    data class Overflow(val page: PageState, val requestedTemplate: PageTemplate, val overflowSlots: List<ImageSlot>) : TemplateChangeResult
}

sealed interface MoveResult {
    data class Moved(val sourcePage: PageState, val targetPage: PageState) : MoveResult
    data object SourceEmpty : MoveResult
    data object TargetFull : MoveResult
}

object PhotobookSlotActions {
    fun emptySlotsFor(template: PageTemplate): List<ImageSlot> =
        template.slotIds.map { ImageSlot(slotId = it, imageRef = null) }

    fun changeTemplate(page: PageState, newTemplate: PageTemplate): TemplateChangeResult {
        val filled = page.slots.filter { it.imageRef != null }
        if (filled.size > newTemplate.capacity) {
            return TemplateChangeResult.Overflow(
                page = page,
                requestedTemplate = newTemplate,
                overflowSlots = filled.drop(newTemplate.capacity)
            )
        }

        val newSlots = newTemplate.slotIds.mapIndexed { index, slotId ->
            val old = filled.getOrNull(index)
            if (old == null) ImageSlot(slotId = slotId, imageRef = null) else old.copy(slotId = slotId)
        }

        return TemplateChangeResult.Changed(page.copy(template = newTemplate, slots = newSlots))
    }

    fun moveImage(sourcePage: PageState, sourceSlotId: String, targetPage: PageState): MoveResult {
        val sourceSlot = sourcePage.slots.firstOrNull { it.slotId == sourceSlotId } ?: return MoveResult.SourceEmpty
        if (sourceSlot.imageRef == null) return MoveResult.SourceEmpty
        val targetSlot = targetPage.slots.firstOrNull { it.imageRef == null } ?: return MoveResult.TargetFull

        val updatedSource = sourcePage.copy(
            slots = sourcePage.slots.map { slot ->
                if (slot.slotId == sourceSlotId) slot.copy(imageRef = null, cropScale = 1f, cropOffsetX = 0f, cropOffsetY = 0f) else slot
            }
        )
        val updatedTarget = targetPage.copy(
            slots = targetPage.slots.map { slot ->
                if (slot.slotId == targetSlot.slotId) sourceSlot.copy(slotId = targetSlot.slotId) else slot
            }
        )

        return MoveResult.Moved(updatedSource, updatedTarget)
    }

    fun swapSlots(page: PageState, firstSlotId: String, secondSlotId: String): PageState {
        val first = page.slots.firstOrNull { it.slotId == firstSlotId } ?: return page
        val second = page.slots.firstOrNull { it.slotId == secondSlotId } ?: return page
        return page.copy(
            slots = page.slots.map { slot ->
                when (slot.slotId) {
                    firstSlotId -> second.copy(slotId = firstSlotId)
                    secondSlotId -> first.copy(slotId = secondSlotId)
                    else -> slot
                }
            }
        )
    }
}
```

- [ ] **Step 4: Run action tests**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest --tests com.yingjian.feature.photobook.PhotobookSlotActionsTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/PhotobookSlotActions.kt app/src/test/java/com/yingjian/feature/photobook/PhotobookSlotActionsTest.kt
git commit -m "feat: add photobook slot actions"
```

## Task 5: Convert Auto Layout And Save/Load To Page Documents

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/photobook/layout/AutoLayoutAlgorithm.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookViewModel.kt`
- Modify: `app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt`
- Test: `app/src/test/java/com/yingjian/feature/photobook/layout/AutoLayoutAlgorithmTest.kt`

- [ ] **Step 1: Update auto-layout tests**

Replace image-element assertions in `app/src/test/java/com/yingjian/feature/photobook/layout/AutoLayoutAlgorithmTest.kt` with slot assertions:

```kotlin
@Test
fun `layout creates single template page per memory`() {
    val memories = listOf(
        testMemory(id = 1, width = 1200, height = 800),
        testMemory(id = 2, width = 800, height = 1200)
    )
    val photobook = PhotobookEntity(name = "Test", paperSize = "TWELVE_INCH_LANDSCAPE", createdAt = 0, updatedAt = 0)

    val result = AutoLayoutAlgorithm.layout(memories, PaperSize.TWELVE_INCH_LANDSCAPE, photobook)

    assertEquals(2, result.pages.size)
    assertEquals(PageTemplate.Single, result.pages[0].template)
    assertEquals("content://test/1", result.pages[0].slots.first().imageRef?.imageUri)
    assertEquals(1f, result.pages[0].slots.first().cropScale, 0.001f)
}
```

Add imports:

```kotlin
import com.yingjian.feature.photobook.model.PageTemplate
```

- [ ] **Step 2: Run test to verify it fails**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest --tests com.yingjian.feature.photobook.layout.AutoLayoutAlgorithmTest
```

Expected: FAIL because auto layout still writes `elements`.

- [ ] **Step 3: Update `AutoLayoutAlgorithm` page creation**

In `app/src/main/java/com/yingjian/feature/photobook/layout/AutoLayoutAlgorithm.kt`, replace `ImageElement` creation with:

```kotlin
val page = PageState(
    pageNumber = index + 1,
    template = PageTemplate.Single,
    slots = listOf(
        ImageSlot(
            slotId = "slot-1",
            imageRef = ImageRef(
                memoryId = memory.id,
                imageUri = memory.imageUri,
                sourceImageIndex = 0
            )
        )
    ),
    textElements = buildMoodTextElements(memory, paperSize),
    trimWidthMm = paperSize.widthMm,
    trimHeightMm = paperSize.heightMm
)
```

Add this private helper in the same object:

```kotlin
private fun buildMoodTextElements(memory: MemoryRecordEntity, paperSize: PaperSize): List<TextElement> {
    if (memory.moodText.isNullOrBlank()) return emptyList()
    return listOf(
        TextElement(
            text = memory.moodText,
            xMm = paperSize.widthMm * 0.15f,
            yMm = paperSize.heightMm - 22f,
            widthMm = paperSize.widthMm * 0.7f,
            heightMm = 10f,
            rotationDeg = 0f,
            zIndex = 1
        )
    )
}
```

Use the same page shape in `createSinglePhotoPage`.

- [ ] **Step 4: Persist `PageLayoutDocument` in ViewModel**

In `PhotobookViewModel.createPhotobook` and `appendPhotosToBook`, replace:

```kotlin
elementsJson = ElementSerializer.serialize(page.elements)
```

with:

```kotlin
elementsJson = PageLayoutDocumentSerializer.serialize(page.toDocument())
```

Add import:

```kotlin
import com.yingjian.core.util.PageLayoutDocumentSerializer
```

- [ ] **Step 5: Load `PageLayoutDocument` in navigation**

In `YingJianNavHost.kt`, replace both editor and preview load blocks that deserialize `ElementSerializer.deserialize(layout.elementsJson)` with:

```kotlin
val document = PageLayoutDocumentSerializer.deserialize(layout.elementsJson)
PageState(
    pageNumber = layout.pageNumber,
    template = document.template,
    slots = document.slots,
    textElements = document.textElements,
    trimWidthMm = PaperSize.valueOf(photobook.paperSize).widthMm,
    trimHeightMm = PaperSize.valueOf(photobook.paperSize).heightMm
)
```

Add import:

```kotlin
import com.yingjian.core.util.PageLayoutDocumentSerializer
```

- [ ] **Step 6: Save `PageLayoutDocument` in editor route**

In `YingJianNavHost.kt` editor `onSave`, replace:

```kotlin
elementsJson = ElementSerializer.serialize(page.elements)
```

with:

```kotlin
elementsJson = PageLayoutDocumentSerializer.serialize(page.toDocument())
```

- [ ] **Step 7: Run compile and auto-layout tests**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest --tests com.yingjian.feature.photobook.layout.AutoLayoutAlgorithmTest
./gradlew :app:compileDebugKotlin
```

Expected: both commands PASS.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/layout/AutoLayoutAlgorithm.kt app/src/main/java/com/yingjian/feature/photobook/PhotobookViewModel.kt app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt app/src/test/java/com/yingjian/feature/photobook/layout/AutoLayoutAlgorithmTest.kt
git commit -m "feat: save photobook pages as slot documents"
```

## Task 6: Replace Canvas Rendering With Slot Rendering

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookCanvasPage.kt`

- [ ] **Step 1: Add slot rendering parameters**

Change `PhotobookCanvasPage` signature to:

```kotlin
fun PhotobookCanvasPage(
    pageState: PageState,
    containerWidthDp: Dp,
    moodText: String? = null,
    memoryDate: Long? = null,
    selectedSlotId: String? = null,
    onSlotSelected: (String) -> Unit = {},
    onDeselect: () -> Unit = {},
    onSlotImageAdjusted: (slotId: String, offsetXMm: Float, offsetYMm: Float, scale: Float) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
)
```

- [ ] **Step 2: Calculate slot rectangles from the engine**

Inside `PhotobookCanvasPage`, add:

```kotlin
val slotRects = remember(pageState.template, pageState.trimWidthMm, pageState.trimHeightMm, pageState.bleedMm) {
    TemplateLayoutEngine.calculateSlots(
        LayoutInput(
            trimWidthMm = pageState.trimWidthMm,
            trimHeightMm = pageState.trimHeightMm,
            bleedMm = pageState.bleedMm,
            safeMarginMm = 16f,
            template = pageState.template
        )
    )
}
```

- [ ] **Step 3: Render each slot**

Replace `pageState.elements.forEach` with:

```kotlin
slotRects.forEach { rect ->
    val slot = pageState.slots.firstOrNull { it.slotId == rect.slotId }
    RenderImageSlot(
        rect = rect,
        slot = slot,
        scaleFactor = scaleFactor,
        isSelected = selectedSlotId == rect.slotId,
        onSelect = { onSlotSelected(rect.slotId) },
        onAdjusted = { offsetXMm, offsetYMm, scale ->
            onSlotImageAdjusted(rect.slotId, offsetXMm, offsetYMm, scale)
        }
    )
}
pageState.textElements.forEach { element ->
    RenderPrinterTextElement(
        element = element,
        scaleFactor = scaleFactor,
        dateText = memoryDate?.let { formatDate(it) },
        density = density
    )
}
```

- [ ] **Step 4: Add `RenderImageSlot`**

Add this composable to the same file:

```kotlin
@Composable
private fun RenderImageSlot(
    rect: SlotRectMm,
    slot: ImageSlot?,
    scaleFactor: Float,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onAdjusted: (offsetXMm: Float, offsetYMm: Float, scale: Float) -> Unit
) {
    val density = LocalDensity.current
    var gestureOffsetX by remember(slot?.slotId, slot?.cropOffsetX) { mutableFloatStateOf(0f) }
    var gestureOffsetY by remember(slot?.slotId, slot?.cropOffsetY) { mutableFloatStateOf(0f) }
    var gestureScale by remember(slot?.slotId, slot?.cropScale) { mutableFloatStateOf(slot?.cropScale ?: 1f) }

    fun pxToMm(px: Float): Float = with(density) { px / scaleFactor / density.density }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (rect.xMm * scaleFactor).dp.roundToPx(),
                    (rect.yMm * scaleFactor).dp.roundToPx()
                )
            }
            .size((rect.widthMm * scaleFactor).dp, (rect.heightMm * scaleFactor).dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color.White.copy(alpha = 0.5f))
            .border(
                width = if (isSelected) 1.5.dp else 0.5.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(2.dp)
            )
            .clickable(onClick = onSelect)
            .pointerInput(isSelected) {
                if (isSelected && slot?.imageRef != null) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        gestureOffsetX += pan.x
                        gestureOffsetY += pan.y
                        gestureScale = SlotImageTransform.clampScale(gestureScale * zoom)
                        val offsetXMm = SlotImageTransform.clampOffset((slot.cropOffsetX + pxToMm(gestureOffsetX)), rect.widthMm, gestureScale)
                        val offsetYMm = SlotImageTransform.clampOffset((slot.cropOffsetY + pxToMm(gestureOffsetY)), rect.heightMm, gestureScale)
                        onAdjusted(offsetXMm, offsetYMm, gestureScale)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (slot?.imageRef == null) {
            Text("+", color = MaterialTheme.colorScheme.outline)
        } else {
            AsyncImage(
                model = Uri.parse(slot.imageRef.imageUri),
                contentDescription = null,
                contentScale = if (slot.fitMode == FitMode.Crop) ContentScale.Crop else ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = slot.cropScale
                        scaleY = slot.cropScale
                        translationX = with(density) { (slot.cropOffsetX * scaleFactor).dp.toPx() }
                        translationY = with(density) { (slot.cropOffsetY * scaleFactor).dp.toPx() }
                    }
            )
        }
    }
}
```

Add imports for `clickable`, `graphicsLayer`, `LayoutInput`, `SlotRectMm`, `SlotImageTransform`, `TemplateLayoutEngine`, `FitMode`, and `ImageSlot`.

- [ ] **Step 5: Update call sites to compile**

In `PhotobookPreviewScreen.kt`, replace old `isSelected = false` argument with:

```kotlin
selectedSlotId = null
```

In `PhotobookEditorScreen.kt`, replace old selection arguments with `selectedSlotId`, `onSlotSelected`, and `onSlotImageAdjusted`. Use this update pattern:

```kotlin
val updatedPages = bookState.pages.toMutableList()
updatedPages[cp] = currentPageState.copy(
    slots = currentPageState.slots.map { slot ->
        if (slot.slotId == slotId) {
            slot.copy(cropOffsetX = offsetXMm, cropOffsetY = offsetYMm, cropScale = scale)
        } else {
            slot
        }
    }
)
onUpdateState(bookState.copy(pages = updatedPages, selectedSlotId = slotId))
```

- [ ] **Step 6: Run compile**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/PhotobookCanvasPage.kt app/src/main/java/com/yingjian/feature/photobook/PhotobookEditorScreen.kt app/src/main/java/com/yingjian/feature/photobook/PhotobookPreviewScreen.kt
git commit -m "feat: render photobook pages from image slots"
```

## Task 7: Add Template Controls, Move Commands, And Swap In Editor

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookEditorScreen.kt`
- Modify: `app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt`

- [ ] **Step 1: Add editor callback contract**

Update `PhotobookEditorScreen` parameters:

```kotlin
onChangeTemplate: (PageTemplate) -> Unit = {},
onMoveSelectedToPreviousPage: () -> Unit = {},
onMoveSelectedToNextPage: () -> Unit = {},
onMoveSelectedToNewPage: () -> Unit = {},
onSwapSelectedWithSlot: (String) -> Unit = {},
onFillSelectedSlot: () -> Unit = {},
onDeleteSelectedSlotImage: () -> Unit = {},
onResetSelectedSlotImage: () -> Unit = {},
```

- [ ] **Step 2: Add template selector UI**

In the content-page controls area, add:

```kotlin
SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(horizontal = 16.dp)) {
    PageTemplate.entries.forEachIndexed { index, template ->
        SegmentedButton(
            selected = currentPageState.template == template,
            onClick = { onChangeTemplate(template) },
            shape = SegmentedButtonDefaults.itemShape(index = index, count = PageTemplate.entries.size),
            label = {
                Text(
                    when (template) {
                        PageTemplate.Single -> "单图"
                        PageTemplate.TwoHorizontal -> "上下"
                        PageTemplate.TwoVertical -> "左右"
                        PageTemplate.GridFour -> "四宫格"
                    }
                )
            }
        )
    }
}
```

- [ ] **Step 3: Add selected-slot command row**

Add this row only when `bookState.selectedSlotId != null`:

```kotlin
Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.SpaceEvenly
) {
    TextButton(onClick = onFillSelectedSlot) { Text("换图") }
    TextButton(onClick = onMoveSelectedToPreviousPage) { Text("上一页") }
    TextButton(onClick = onMoveSelectedToNextPage) { Text("下一页") }
    TextButton(onClick = onMoveSelectedToNewPage) { Text("新页") }
    TextButton(onClick = onDeleteSelectedSlotImage) { Text("删除") }
    TextButton(onClick = onResetSelectedSlotImage) { Text("重置") }
}
```

- [ ] **Step 4: Wire template changes in navigation**

In `YingJianNavHost.kt`, pass:

```kotlin
onChangeTemplate = { newTemplate ->
    val state = loadedBookState ?: return@PhotobookEditorScreen
    val pageIndex = state.currentPage
    val page = state.pages.getOrNull(pageIndex) ?: return@PhotobookEditorScreen
    when (val result = PhotobookSlotActions.changeTemplate(page, newTemplate)) {
        is TemplateChangeResult.Changed -> {
            val updatedPages = state.pages.toMutableList()
            updatedPages[pageIndex] = result.page
            loadedBookState = state.copy(pages = updatedPages)
        }
        is TemplateChangeResult.Overflow -> {
            loadedBookState = state
        }
    }
}
```

If overflow occurs in this phase, show a dialog in `PhotobookEditorScreen` before calling this callback. The dialog must offer "保留当前选中图片", "保留前 N 张并移到新页面", and "取消".

- [ ] **Step 5: Wire movement commands**

Add a helper in `YingJianNavHost.kt` near the editor route:

```kotlin
fun moveSelectedToPage(targetPageIndex: Int) {
    val state = loadedBookState ?: return
    val sourceIndex = state.currentPage
    val selectedSlotId = state.selectedSlotId ?: return
    val sourcePage = state.pages.getOrNull(sourceIndex) ?: return
    val targetPage = state.pages.getOrNull(targetPageIndex) ?: return
    when (val result = PhotobookSlotActions.moveImage(sourcePage, selectedSlotId, targetPage)) {
        is MoveResult.Moved -> {
            val pages = state.pages.toMutableList()
            pages[sourceIndex] = result.sourcePage
            pages[targetPageIndex] = result.targetPage
            loadedBookState = state.copy(pages = pages, currentPage = targetPageIndex, selectedSlotId = null)
        }
        MoveResult.SourceEmpty -> Unit
        MoveResult.TargetFull -> {
            loadedBookState = state
        }
    }
}
```

Pass:

```kotlin
onMoveSelectedToPreviousPage = { moveSelectedToPage((loadedBookState?.currentPage ?: 0) - 1) },
onMoveSelectedToNextPage = { moveSelectedToPage((loadedBookState?.currentPage ?: 0) + 1) },
```

Guard indices before use:

```kotlin
if (targetPageIndex !in state.pages.indices) return
```

- [ ] **Step 6: Wire move to new page**

Pass:

```kotlin
onMoveSelectedToNewPage = {
    val state = loadedBookState ?: return@PhotobookEditorScreen
    val pageIndex = state.currentPage
    val selectedSlotId = state.selectedSlotId ?: return@PhotobookEditorScreen
    val page = state.pages.getOrNull(pageIndex) ?: return@PhotobookEditorScreen
    val selected = page.slots.firstOrNull { it.slotId == selectedSlotId && it.imageRef != null } ?: return@PhotobookEditorScreen
    val clearedPage = page.copy(
        slots = page.slots.map { if (it.slotId == selectedSlotId) it.copy(imageRef = null, cropScale = 1f, cropOffsetX = 0f, cropOffsetY = 0f) else it }
    )
    val newPage = PageState(
        pageNumber = state.pages.size + 1,
        template = PageTemplate.Single,
        slots = listOf(selected.copy(slotId = "slot-1")),
        trimWidthMm = page.trimWidthMm,
        trimHeightMm = page.trimHeightMm,
        bleedMm = page.bleedMm
    )
    val pages = state.pages.toMutableList()
    pages[pageIndex] = clearedPage
    loadedBookState = state.copy(pages = pages + newPage, currentPage = pages.size, selectedSlotId = "slot-1")
}
```

- [ ] **Step 7: Run compile**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/PhotobookEditorScreen.kt app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt
git commit -m "feat: add photobook slot editing commands"
```

## Task 8: Redesign Memory Photo Picker Contract

**Files:**
- Create: `app/src/main/java/com/yingjian/feature/photobook/SelectedMemoryPhoto.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotoPickerScreen.kt`
- Modify: `app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookViewModel.kt`

- [ ] **Step 1: Add selected photo contract**

Create `app/src/main/java/com/yingjian/feature/photobook/SelectedMemoryPhoto.kt`:

```kotlin
package com.yingjian.feature.photobook

import com.yingjian.core.data.database.MemoryRecordEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class MemoryPhotoPickerMode {
    BatchImport,
    SingleSlot
}

@Serializable
data class SelectedMemoryPhoto(
    val memoryId: Long,
    val imageUri: String,
    val sourceImageIndex: Int?,
    val sourceImageId: Long? = null
)

object SelectedMemoryPhotoCodec {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    fun encode(value: List<SelectedMemoryPhoto>): String = json.encodeToString(value)
    fun decode(raw: String): List<SelectedMemoryPhoto> = json.decodeFromString(raw)
}

fun MemoryRecordEntity.toSelectablePhotos(): List<SelectedMemoryPhoto> {
    val uris = runCatching {
        kotlinx.serialization.json.Json.decodeFromString<List<String>>(imageUrisJson)
    }.getOrDefault(emptyList())
    val sourceUris = if (uris.isEmpty()) listOf(imageUri) else uris
    return sourceUris.mapIndexed { index, uri ->
        SelectedMemoryPhoto(
            memoryId = id,
            imageUri = uri,
            sourceImageIndex = index
        )
    }
}
```

- [ ] **Step 2: Change picker callback**

Change `PhotoPickerScreen` signature:

```kotlin
fun PhotoPickerScreen(
    memories: List<MemoryRecordEntity>,
    mode: MemoryPhotoPickerMode,
    onBack: () -> Unit,
    onComplete: (List<SelectedMemoryPhoto>) -> Unit
)
```

- [ ] **Step 3: Replace selected ids with selected photos**

In `PhotoPickerScreen`, replace `selectedIds` with:

```kotlin
val selectedPhotos = remember { mutableStateListOf<SelectedMemoryPhoto>() }
```

For a whole memory click in batch mode:

```kotlin
val photos = memory.toSelectablePhotos()
val allSelected = photos.all { photo -> selectedPhotos.any { it.memoryId == photo.memoryId && it.imageUri == photo.imageUri } }
if (mode == MemoryPhotoPickerMode.BatchImport) {
    if (allSelected) {
        selectedPhotos.removeAll { selected -> photos.any { it.memoryId == selected.memoryId && it.imageUri == selected.imageUri } }
    } else {
        photos.forEach { photo ->
            if (selectedPhotos.none { it.memoryId == photo.memoryId && it.imageUri == photo.imageUri }) {
                selectedPhotos.add(photo)
            }
        }
    }
}
```

For single-slot mode:

```kotlin
val firstPhoto = memory.toSelectablePhotos().firstOrNull()
if (mode == MemoryPhotoPickerMode.SingleSlot && firstPhoto != null) {
    selectedPhotos.clear()
    selectedPhotos.add(firstPhoto)
    onComplete(selectedPhotos.toList())
}
```

- [ ] **Step 4: Store selected photos in navigation**

In the photo picker route `onComplete`, replace id comma serialization with:

```kotlin
val encoded = SelectedMemoryPhotoCodec.encode(selectedPhotos)
navController.previousBackStackEntry?.savedStateHandle?.apply {
    if (isAppendMode) {
        set("appendMemoryPhotos", encoded)
    } else {
        set("selectedMemoryPhotos", encoded)
    }
}
```

- [ ] **Step 5: Update create and append APIs**

In `AutoLayoutAlgorithm`, change `createSinglePhotoPage` to:

```kotlin
fun createSinglePhotoPage(
    imageRef: ImageRef,
    moodText: String?,
    paperSize: PaperSize,
    pageNumber: Int
): PageState = PageState(
    pageNumber = pageNumber,
    template = PageTemplate.Single,
    slots = listOf(ImageSlot(slotId = "slot-1", imageRef = imageRef)),
    textElements = if (moodText.isNullOrBlank()) {
        emptyList()
    } else {
        listOf(
            TextElement(
                text = moodText,
                xMm = paperSize.widthMm * 0.15f,
                yMm = paperSize.heightMm - 22f,
                widthMm = paperSize.widthMm * 0.7f,
                heightMm = 10f,
                rotationDeg = 0f,
                zIndex = 1
            )
        )
    },
    trimWidthMm = paperSize.widthMm,
    trimHeightMm = paperSize.heightMm
)
```

In `PhotobookViewModel`, add:

```kotlin
fun createPhotobookFromPhotos(name: String, selectedPhotos: List<SelectedMemoryPhoto>) {
    viewModelScope.launch {
        uiState = uiState.copy(isLoading = true)
        val paperSize = PaperSize.TWELVE_INCH_LANDSCAPE
        val photobook = PhotobookEntity(
            name = name,
            paperSize = paperSize.name,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val bookId = withContext(Dispatchers.IO) {
            photobookRepository.createPhotobook(photobook)
        }
        val memoryById = withContext(Dispatchers.IO) {
            selectedPhotos.mapNotNull { photo ->
                memoryRepository.getMemoryById(photo.memoryId)?.let { photo.memoryId to it }
            }.toMap()
        }
        val pages = selectedPhotos.mapIndexed { index, photo ->
            AutoLayoutAlgorithm.createSinglePhotoPage(
                imageRef = ImageRef(
                    memoryId = photo.memoryId,
                    imageUri = photo.imageUri,
                    sourceImageIndex = photo.sourceImageIndex,
                    sourceImageId = photo.sourceImageId
                ),
                moodText = memoryById[photo.memoryId]?.moodText,
                paperSize = paperSize,
                pageNumber = index + 1
            )
        }
        val bookState = BookState(
            photobook = photobook.copy(id = bookId),
            pages = pages,
            currentPage = 0,
            mode = LayoutMode.AUTO
        )
        withContext(Dispatchers.IO) {
            pages.forEach { page ->
                photobookRepository.savePageLayout(
                    PageLayoutEntity(
                        photobookId = bookId,
                        pageNumber = page.pageNumber,
                        elementsJson = PageLayoutDocumentSerializer.serialize(page.toDocument()),
                        mode = bookState.mode.name
                    )
                )
            }
        }
        uiState = uiState.copy(currentBookState = bookState, isLoading = false)
        loadPhotobooks()
    }
}

fun appendPhotosToBook(selectedPhotos: List<SelectedMemoryPhoto>) {
    viewModelScope.launch {
        val currentState = uiState.currentBookState ?: return@launch
        uiState = uiState.copy(isLoading = true)
        val paperSize = runCatching {
            PaperSize.valueOf(currentState.photobook.paperSize)
        }.getOrDefault(PaperSize.TWELVE_INCH_LANDSCAPE)
        val memoryById = withContext(Dispatchers.IO) {
            selectedPhotos.mapNotNull { photo ->
                memoryRepository.getMemoryById(photo.memoryId)?.let { photo.memoryId to it }
            }.toMap()
        }
        val startPageNumber = currentState.pages.size + 1
        val newPages = selectedPhotos.mapIndexed { index, photo ->
            AutoLayoutAlgorithm.createSinglePhotoPage(
                imageRef = ImageRef(
                    memoryId = photo.memoryId,
                    imageUri = photo.imageUri,
                    sourceImageIndex = photo.sourceImageIndex,
                    sourceImageId = photo.sourceImageId
                ),
                moodText = memoryById[photo.memoryId]?.moodText,
                paperSize = paperSize,
                pageNumber = startPageNumber + index
            )
        }
        val updatedState = currentState.copy(
            pages = currentState.pages + newPages,
            currentPage = currentState.pages.size
        )
        withContext(Dispatchers.IO) {
            newPages.forEach { page ->
                photobookRepository.savePageLayout(
                    PageLayoutEntity(
                        photobookId = currentState.photobook.id,
                        pageNumber = page.pageNumber,
                        elementsJson = PageLayoutDocumentSerializer.serialize(page.toDocument()),
                        mode = currentState.mode.name
                    )
                )
            }
        }
        uiState = uiState.copy(currentBookState = updatedState, isLoading = false)
    }
}
```

- [ ] **Step 6: Decode selected photos in navigation**

Replace `selectedMemoryIds` and `appendMemoryIds` handling with:

```kotlin
val selectedPhotosStr = backStackEntry.savedStateHandle.get<String>("selectedMemoryPhotos")
val selectedPhotos = selectedPhotosStr?.let { SelectedMemoryPhotoCodec.decode(it) }.orEmpty()
if (selectedPhotos.isNotEmpty()) {
    viewModel.createPhotobookFromPhotos(photobookName, selectedPhotos)
    backStackEntry.savedStateHandle.remove<String>("selectedMemoryPhotos")
}
```

For append mode:

```kotlin
val appendPhotosStr = backStackEntry.savedStateHandle.get<String>("appendMemoryPhotos")
val appendPhotos = appendPhotosStr?.let { SelectedMemoryPhotoCodec.decode(it) }.orEmpty()
if (appendPhotos.isNotEmpty()) {
    viewModel.appendPhotosToBook(appendPhotos)
    backStackEntry.savedStateHandle.remove<String>("appendMemoryPhotos")
}
```

- [ ] **Step 7: Run compile**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/SelectedMemoryPhoto.kt app/src/main/java/com/yingjian/feature/photobook/PhotoPickerScreen.kt app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt app/src/main/java/com/yingjian/feature/photobook/PhotobookViewModel.kt app/src/main/java/com/yingjian/feature/photobook/layout/AutoLayoutAlgorithm.kt
git commit -m "feat: select photobook photos at image level"
```

## Task 9: Update PDF Export To Use Slots

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/photobook/export/PdfExportUtil.kt`

- [ ] **Step 1: Remove cover image rendering**

In `renderCoverPage`, delete the block that renders `photobook.coverImageUri`. Set title Y to the text-only value:

```kotlin
val titleY = mmToPxFloat(paperSize.heightMm * 0.45f)
```

- [ ] **Step 2: Render content slots**

Replace content element rendering with:

```kotlin
val slotRects = TemplateLayoutEngine.calculateSlots(
    LayoutInput(
        trimWidthMm = pageState.trimWidthMm,
        trimHeightMm = pageState.trimHeightMm,
        bleedMm = pageState.bleedMm,
        safeMarginMm = 16f,
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
```

- [ ] **Step 3: Add `renderImageSlot`**

Add:

```kotlin
private fun renderImageSlot(context: Context, canvas: Canvas, rect: SlotRectMm, slot: ImageSlot) {
    val imageRef = slot.imageRef ?: return
    val uri = Uri.parse(imageRef.imageUri)
    val inputStream = context.contentResolver.openInputStream(uri) ?: return
    val targetWidthPx = mmToPx(rect.widthMm * slot.cropScale)
    val targetHeightPx = mmToPx(rect.heightMm * slot.cropScale)
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeStream(inputStream, null, options)
    inputStream.close()

    val bitmapStream = context.contentResolver.openInputStream(uri)
    val bitmap = BitmapFactory.decodeStream(
        bitmapStream,
        null,
        BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(options.outWidth, options.outHeight, targetWidthPx, targetHeightPx)
        }
    )
    bitmapStream?.close()
    if (bitmap == null) return

    val x = mmToPxFloat(rect.xMm)
    val y = mmToPxFloat(rect.yMm)
    val w = mmToPxFloat(rect.widthMm)
    val h = mmToPxFloat(rect.heightMm)

    canvas.save()
    canvas.clipRect(RectF(x, y, x + w, y + h))
    val scaledW = w * slot.cropScale
    val scaledH = h * slot.cropScale
    val dx = mmToPxFloat(slot.cropOffsetX)
    val dy = mmToPxFloat(slot.cropOffsetY)
    val dest = RectF(
        x - (scaledW - w) / 2f + dx,
        y - (scaledH - h) / 2f + dy,
        x + w + (scaledW - w) / 2f + dx,
        y + h + (scaledH - h) / 2f + dy
    )
    canvas.drawBitmap(bitmap, null, dest, null)
    canvas.restore()
    bitmap.recycle()
}
```

Add imports:

```kotlin
import com.yingjian.feature.photobook.layout.LayoutInput
import com.yingjian.feature.photobook.layout.SlotRectMm
import com.yingjian.feature.photobook.layout.TemplateLayoutEngine
import com.yingjian.feature.photobook.model.ImageSlot
```

- [ ] **Step 4: Run compile**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/export/PdfExportUtil.kt
git commit -m "feat: export photobook slots to pdf"
```

## Task 10: Preview Spread And Final Regression

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookPreviewScreen.kt`
- Modify: `docs/claude/2026-05-15-photobook-slot-layout-redesign/validation.md`

- [ ] **Step 1: Keep preview leaf model**

Confirm `PhotobookLeaf` still exists and is built as:

```kotlin
val leaves = remember(bookState.photobook.id, bookState.pages.size) {
    buildList {
        add(PhotobookLeaf.Cover(bookState.photobook))
        bookState.pages.forEach { page -> add(PhotobookLeaf.Content(page)) }
        add(PhotobookLeaf.BackCover(bookState.photobook))
    }
}
```

- [ ] **Step 2: Fix landscape spread count**

Use:

```kotlin
val spreadCount = if (leaves.isEmpty()) 0 else (leaves.size + 1) / 2
```

Keep navigation based on spread index, not content-page index.

- [ ] **Step 3: Ensure content preview uses slot renderer**

In `PreviewLeaf`, content branch must call:

```kotlin
PhotobookCanvasPage(
    pageState = leaf.page,
    containerWidthDp = containerWidthDp,
    selectedSlotId = null,
    modifier = modifier
)
```

- [ ] **Step 4: Run full validation**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew testDebugUnitTest
./gradlew :app:assembleDebug
```

Expected: both commands PASS.

- [ ] **Step 5: Manual QA**

Use `docs/claude/2026-05-15-photobook-slot-layout-redesign/validation.md` and record results in the Claude handoff message. Required checks:

- Batch-import a memory with multiple photos and confirm one page per photo.
- Change a page from single to four-grid and confirm only slot 1 is filled.
- Fill an empty grid slot from picker.
- Move a selected image to the previous page with an empty slot.
- Move an image away from a page and confirm the empty page remains.
- Drag and zoom an image inside a slot, tap Save, reopen editor, and confirm crop persists.
- Preview in portrait: one page.
- Preview in landscape: two joined pages.
- Export PDF and confirm page count is content pages plus 2.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/PhotobookPreviewScreen.kt docs/claude/2026-05-15-photobook-slot-layout-redesign/validation.md
git commit -m "test: validate photobook slot layout redesign"
```

## Final Integration Checklist

- [ ] `rg "firstOrNull\\(\\).*ImageElement|filterIsInstance<ImageElement>" app/src/main/java/com/yingjian/feature/photobook app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt` returns no active content-page assumptions except deliberate compatibility-free removed code.
- [ ] `rg "ElementSerializer.serialize\\(page.elements\\)|ElementSerializer.deserialize\\(layout.elementsJson\\)" app/src/main/java` returns no photobook save/load usage.
- [ ] `./gradlew testDebugUnitTest` passes.
- [ ] `./gradlew :app:assembleDebug` passes.
- [ ] APK exists at `/Users/haos/Project/Android/YingJian/app/build/outputs/apk/debug/app-debug.apk`.
- [ ] `git diff --stat HEAD` only contains intentional post-commit changes or is empty.

## Self-Review Result

- Spec coverage: covered model, persistence, layout engine, editor controls, picker, preview, PDF, no old compatibility, empty source page preservation, and photo-level import.
- Placeholder scan: passed. The plan uses concrete file paths, code snippets, commands, and expected results.
- Type consistency: `PageTemplate`, `ImageSlot`, `ImageRef`, `PageLayoutDocument`, `TemplateLayoutEngine`, `PhotobookSlotActions`, and `SelectedMemoryPhoto` names match across tasks.
