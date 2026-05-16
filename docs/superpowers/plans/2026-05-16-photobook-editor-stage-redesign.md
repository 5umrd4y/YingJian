# Photobook Editor Stage Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the photobook editor so cover, content pages, preview, and PDF share one 285x210 stage, five content templates, reliable slot filling, movable cover text, and consistent export behavior.

**Architecture:** Keep print layout in millimeters and make `TemplateLayoutEngine` the only source of content slot rectangles. Persist image/crop state separately from template geometry, persist cover/back-cover layouts as JSON on `PhotobookEntity`, and make editor, preview, and PDF render from the same state models.

**Tech Stack:** Kotlin, Android Jetpack Compose, Room, kotlinx.serialization, Android `PdfDocument`, Coil, JUnit4, Gradle Android plugin.

---

## Source Documents

- Spec: `docs/superpowers/specs/2026-05-16-photobook-editor-stage-redesign.md`
- Review v2: `docs/superpowers/specs/Photobook-Editor-Stage-Redesign-审阅结果-v2.md`
- Toolbar reference: `stitch_yingjian_huacev2/_1/code.html`
- Cover reference: `stitch_yingjian_fengmian/screen.png`

## Current Baseline

- Current branch at plan time: `codex/photobook-editor-stage-plan`
- Current HEAD at plan time: `a4820d4 docs: address photobook spec review v2`
- Current database version: `5`
- Current templates: `Single`, `TwoHorizontal`, `TwoVertical`, `GridFour`
- Current known untracked files intentionally out of scope:
  - `docs/superpowers/specs/Photobook-Editor-Stage-Redesign-审阅结果-v2.md`
  - `docs/superpowers/plans/2026-05-10-yingjian-android-app.md`
  - generated/reference/build/local files shown by `git status --short`

## File Structure

### Model And Serialization

- Modify `app/src/main/java/com/yingjian/feature/photobook/model/PageTemplate.kt`
  - Replace `Single` with `SingleLandscape` and `SinglePortrait`.
- Modify `app/src/main/java/com/yingjian/feature/photobook/model/ImageRef.kt`
  - Add optional `imageWidth` and `imageHeight`.
- Modify `app/src/main/java/com/yingjian/feature/photobook/model/ImageSlot.kt`
  - Remove persisted slot rectangle fields from the domain model.
- Modify `app/src/main/java/com/yingjian/feature/photobook/model/PageLayoutDocument.kt`
  - Bump default version to `2`.
  - Remove `PageState.elements` dependency for content image layout.
- Create `app/src/main/java/com/yingjian/feature/photobook/model/PhotobookLayoutDefaults.kt`
  - Central 285x210, bleed, safe margin, gutter, and bottom text reserve constants.
- Create `app/src/main/java/com/yingjian/feature/photobook/model/CoverLayout.kt`
  - Cover/back-cover layout model, text roles, app-owned alignment enum, default factories.
- Modify `app/src/main/java/com/yingjian/core/util/PageLayoutDocumentSerializer.kt`
  - Serialize v2 page layout without slot rectangle fields.
  - Accept old fields while decoding only if they appear in existing JSON.
- Create `app/src/main/java/com/yingjian/core/util/CoverLayoutSerializer.kt`
  - Serialize/deserialize `CoverLayout`.

### Database

- Modify `app/src/main/java/com/yingjian/core/data/database/YingJianDatabase.kt`
  - Add `coverLayoutJson` and `backCoverLayoutJson` to `PhotobookEntity`.
  - Bump Room database version from `5` to `6`.
  - Use destructive migration for this internal testing phase.

### Layout And Page Creation

- Modify `app/src/main/java/com/yingjian/feature/photobook/layout/TemplateLayoutEngine.kt`
  - Add `SingleLandscape` and `SinglePortrait` rules.
  - Use shared `PhotobookLayoutDefaults`.
- Modify `app/src/main/java/com/yingjian/feature/photobook/layout/AutoLayoutAlgorithm.kt`
  - Select single template by image ratio.
  - Update `layout()` so no path creates legacy `PageTemplate.Single`.
- Modify `app/src/main/java/com/yingjian/feature/photobook/SelectedMemoryPhoto.kt`
  - Add image dimensions and propagate memory dimensions into selectable photos.
- Modify `app/src/main/java/com/yingjian/feature/photobook/PhotobookViewModel.kt`
  - Pass dimensions into `ImageRef` and auto-layout creation.
  - Preserve recent PDF crash/OOM related behavior.

### State And Slot Actions

- Modify `app/src/main/java/com/yingjian/feature/photobook/model/BookState.kt`
  - Replace slot-only selection with typed `EditorSelection`.
  - Preserve existing `photobook`, `mode`, and `previousManualState`.
  - Add `coverLayout` and `backCoverLayout`.
- Modify `app/src/main/java/com/yingjian/feature/photobook/PhotobookSlotActions.kt`
  - Keep inactive slots when reducing layout capacity.
  - Add add-page, fill-slot, delete-slot, and page-renumber helpers.

### Editor UI

- Create `app/src/main/java/com/yingjian/feature/photobook/PhotobookStage.kt`
  - Shared 285x210 stage wrapper.
- Create `app/src/main/java/com/yingjian/feature/photobook/ContentPageRenderer.kt`
  - Content page renderer using `TemplateLayoutEngine`.
- Replace or heavily modify `app/src/main/java/com/yingjian/feature/photobook/PhotobookCanvasPage.kt`
  - Delegate to `ContentPageRenderer` or keep as thin compatibility wrapper.
- Create `app/src/main/java/com/yingjian/feature/photobook/CoverPageRenderer.kt`
  - Cover/back-cover renderer with draggable text.
- Create `app/src/main/java/com/yingjian/feature/photobook/FloatingPhotobookToolbar.kt`
  - Icon-only floating toolbar.
- Modify `app/src/main/java/com/yingjian/feature/photobook/PhotobookEditorScreen.kt`
  - Stable stage layout, toolbar overlay, leaf navigation, layout/text/move/delete/add actions.
- Modify `app/src/main/java/com/yingjian/feature/photobook/PhotoPickerScreen.kt`
  - Batch memory selection and single-slot mode with one selected photo.

### Preview And PDF

- Modify `app/src/main/java/com/yingjian/feature/photobook/PhotobookPreviewScreen.kt`
  - Keep portrait one-page and landscape two-page behavior.
  - Render cover first and back cover last from shared layout data.
- Modify `app/src/main/java/com/yingjian/feature/photobook/export/PdfExportUtil.kt`
  - Render cover/back-cover from `CoverLayout`.
  - Render content pages from `TemplateLayoutEngine`.
  - Preserve downsampling and memory-safety behavior.

### Tests

- Modify `app/src/test/java/com/yingjian/feature/photobook/model/PageTemplateTest.kt`
- Modify `app/src/test/java/com/yingjian/feature/photobook/layout/TemplateLayoutEngineTest.kt`
- Modify `app/src/test/java/com/yingjian/feature/photobook/layout/AutoLayoutAlgorithmTest.kt`
- Modify `app/src/test/java/com/yingjian/feature/photobook/PhotobookSlotActionsTest.kt`
- Modify `app/src/test/java/com/yingjian/core/util/PageLayoutDocumentSerializerTest.kt`
- Create `app/src/test/java/com/yingjian/feature/photobook/model/CoverLayoutTest.kt`
- Create `app/src/test/java/com/yingjian/core/util/CoverLayoutSerializerTest.kt`

---

## Task 1: Page Templates, Image Models, And Layout Defaults

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/photobook/model/PageTemplate.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/model/ImageRef.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/model/ImageSlot.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/model/PageLayoutDocument.kt`
- Create: `app/src/main/java/com/yingjian/feature/photobook/model/PhotobookLayoutDefaults.kt`
- Modify: `app/src/test/java/com/yingjian/feature/photobook/model/PageTemplateTest.kt`
- Modify: `app/src/test/java/com/yingjian/core/util/PageLayoutDocumentSerializerTest.kt`

- [ ] **Step 1: Write the failing template and document-version tests**

Replace `app/src/test/java/com/yingjian/feature/photobook/model/PageTemplateTest.kt` with tests that assert the new stable enum contract:

```kotlin
package com.yingjian.feature.photobook.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PageTemplateTest {
    @Test
    fun `templates contain five stable entries`() {
        assertEquals(
            listOf(
                PageTemplate.SingleLandscape,
                PageTemplate.SinglePortrait,
                PageTemplate.TwoHorizontal,
                PageTemplate.TwoVertical,
                PageTemplate.GridFour
            ),
            PageTemplate.entries
        )
    }

    @Test
    fun `template slot ids are stable`() {
        assertEquals(listOf("slot-1"), PageTemplate.SingleLandscape.slotIds)
        assertEquals(listOf("slot-1"), PageTemplate.SinglePortrait.slotIds)
        assertEquals(listOf("slot-1", "slot-2"), PageTemplate.TwoHorizontal.slotIds)
        assertEquals(listOf("slot-1", "slot-2"), PageTemplate.TwoVertical.slotIds)
        assertEquals(listOf("slot-1", "slot-2", "slot-3", "slot-4"), PageTemplate.GridFour.slotIds)
    }

    @Test
    fun `template capacities match slot count`() {
        PageTemplate.entries.forEach { template ->
            assertEquals(template.slotIds.size, template.capacity)
        }
    }
}
```

Append this test to `app/src/test/java/com/yingjian/core/util/PageLayoutDocumentSerializerTest.kt`:

```kotlin
@Test
fun `page layout document defaults to version 2`() {
    val document = PageLayoutDocument(
        template = PageTemplate.SingleLandscape,
        slots = listOf(ImageSlot(slotId = "slot-1", imageRef = null))
    )

    assertEquals(2, document.version)
}
```

- [ ] **Step 2: Run the focused failing tests**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.yingjian.feature.photobook.model.PageTemplateTest" --tests "com.yingjian.core.util.PageLayoutDocumentSerializerTest"
```

Expected: fail with unresolved `SingleLandscape`/`SinglePortrait`, or assertions showing document version is still `1`.

- [ ] **Step 3: Implement page templates and shared defaults**

Replace `app/src/main/java/com/yingjian/feature/photobook/model/PageTemplate.kt` with:

```kotlin
package com.yingjian.feature.photobook.model

enum class PageTemplate(val slotIds: List<String>) {
    SingleLandscape(listOf("slot-1")),
    SinglePortrait(listOf("slot-1")),
    TwoHorizontal(listOf("slot-1", "slot-2")),
    TwoVertical(listOf("slot-1", "slot-2")),
    GridFour(listOf("slot-1", "slot-2", "slot-3", "slot-4"));

    val capacity: Int get() = slotIds.size
}
```

Create `app/src/main/java/com/yingjian/feature/photobook/model/PhotobookLayoutDefaults.kt`:

```kotlin
package com.yingjian.feature.photobook.model

object PhotobookLayoutDefaults {
    const val PAGE_WIDTH_MM = 285f
    const val PAGE_HEIGHT_MM = 210f
    const val SAFE_MARGIN_MM = 16f
    const val BOTTOM_TEXT_RESERVE_MM = 20f
    const val GUTTER_MM = 8f
    const val BLEED_MM = 3f
    const val CONTENT_PAGE_COLOR = 0xFFFAF9F6.toInt()
    const val COVER_PAGE_COLOR = 0xFFAAA194.toInt()
}
```

- [ ] **Step 4: Update image models and document version**

Replace `app/src/main/java/com/yingjian/feature/photobook/model/ImageRef.kt` with:

```kotlin
package com.yingjian.feature.photobook.model

data class ImageRef(
    val memoryId: Long,
    val imageUri: String,
    val sourceImageIndex: Int?,
    val sourceImageId: Long? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null
)
```

Replace `app/src/main/java/com/yingjian/feature/photobook/model/ImageSlot.kt` with:

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

Update `PageLayoutDocument` default version and remove image layout dependence from the legacy shim:

```kotlin
data class PageLayoutDocument(
    val version: Int = 2,
    val template: PageTemplate,
    val slots: List<ImageSlot>,
    val textElements: List<TextElement> = emptyList()
)
```

Temporarily keep `PageState.elements` only for text compatibility:

```kotlin
val elements: List<PageElement>
    get() = textElements
```

- [ ] **Step 5: Update serializer DTOs for v2 slots**

In `PageLayoutDocumentSerializer.kt`, make `ImageSlotDto` accept but not emit legacy geometry:

```kotlin
@Serializable
private data class ImageSlotDto(
    val slotId: String,
    val imageRef: ImageRefDto?,
    val cropScale: Float = 1f,
    val cropOffsetX: Float = 0f,
    val cropOffsetY: Float = 0f,
    val fitMode: String = FitMode.Crop.name,
    val widthMm: Float? = null,
    val heightMm: Float? = null,
    val xMm: Float = 0f,
    val yMm: Float = 0f
)

@Serializable
private data class ImageRefDto(
    val memoryId: Long,
    val imageUri: String,
    val sourceImageIndex: Int?,
    val sourceImageId: Long? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null
)
```

Update `ImageSlot.toDto()` so it does not write geometry:

```kotlin
private fun ImageSlot.toDto(): ImageSlotDto = ImageSlotDto(
    slotId = slotId,
    imageRef = imageRef?.let {
        ImageRefDto(
            memoryId = it.memoryId,
            imageUri = it.imageUri,
            sourceImageIndex = it.sourceImageIndex,
            sourceImageId = it.sourceImageId,
            imageWidth = it.imageWidth,
            imageHeight = it.imageHeight
        )
    },
    cropScale = cropScale,
    cropOffsetX = cropOffsetX,
    cropOffsetY = cropOffsetY,
    fitMode = fitMode.name
)
```

Update `ImageSlotDto.toDomain()`:

```kotlin
private fun ImageSlotDto.toDomain(): ImageSlot = ImageSlot(
    slotId = slotId,
    imageRef = imageRef?.let {
        ImageRef(
            memoryId = it.memoryId,
            imageUri = it.imageUri,
            sourceImageIndex = it.sourceImageIndex,
            sourceImageId = it.sourceImageId,
            imageWidth = it.imageWidth,
            imageHeight = it.imageHeight
        )
    },
    cropScale = cropScale,
    cropOffsetX = cropOffsetX,
    cropOffsetY = cropOffsetY,
    fitMode = FitMode.valueOf(fitMode)
)
```

- [ ] **Step 6: Replace all legacy `PageTemplate.Single` references**

Run:

```bash
rg -n "PageTemplate\\.Single\\b|PageTemplate\\.entries|when \\(template\\)" app/src/main/java app/src/test/java
```

Replace legacy `PageTemplate.Single` with `PageTemplate.SingleLandscape` unless the surrounding logic is selecting portrait by image ratio.

- [ ] **Step 7: Run task tests**

Run:

```bash
./gradlew :app:testDebugUnitTest --tests "com.yingjian.feature.photobook.model.PageTemplateTest" --tests "com.yingjian.core.util.PageLayoutDocumentSerializerTest"
```

Expected: all selected tests pass.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/model/PageTemplate.kt \
        app/src/main/java/com/yingjian/feature/photobook/model/ImageRef.kt \
        app/src/main/java/com/yingjian/feature/photobook/model/ImageSlot.kt \
        app/src/main/java/com/yingjian/feature/photobook/model/PageLayoutDocument.kt \
        app/src/main/java/com/yingjian/feature/photobook/model/PhotobookLayoutDefaults.kt \
        app/src/main/java/com/yingjian/core/util/PageLayoutDocumentSerializer.kt \
        app/src/test/java/com/yingjian/feature/photobook/model/PageTemplateTest.kt \
        app/src/test/java/com/yingjian/core/util/PageLayoutDocumentSerializerTest.kt
git commit -m "refactor: update photobook page layout model"
```

---

## Task 2: Template Layout Engine And Auto Import

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/photobook/layout/TemplateLayoutEngine.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/layout/AutoLayoutAlgorithm.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/SelectedMemoryPhoto.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookViewModel.kt`
- Modify: `app/src/test/java/com/yingjian/feature/photobook/layout/TemplateLayoutEngineTest.kt`
- Modify: `app/src/test/java/com/yingjian/feature/photobook/layout/AutoLayoutAlgorithmTest.kt`

- [ ] **Step 1: Write layout engine tests first**

Replace `TemplateLayoutEngineTest.kt` with:

```kotlin
package com.yingjian.feature.photobook.layout

import com.yingjian.feature.photobook.model.PageTemplate
import com.yingjian.feature.photobook.model.PhotobookLayoutDefaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplateLayoutEngineTest {
    private val input = LayoutInput(
        trimWidthMm = 285f,
        trimHeightMm = 210f,
        bleedMm = 3f,
        safeMarginMm = 16f,
        bottomTextReserveMm = 20f,
        gutterMm = 8f,
        template = PageTemplate.SingleLandscape
    )

    @Test
    fun `single landscape is centered with 3 to 2 ratio`() {
        val slot = TemplateLayoutEngine.calculateSlots(input).single()

        assertEquals("slot-1", slot.slotId)
        assertEquals(217.5f, slot.widthMm, 0.01f)
        assertEquals(145f, slot.heightMm, 0.01f)
        assertEquals((285f - 217.5f) / 2f, slot.xMm, 0.01f)
        assertEquals(16f, slot.yMm, 0.01f)
    }

    @Test
    fun `single portrait is centered with 2 to 3 ratio`() {
        val slot = TemplateLayoutEngine.calculateSlots(input.copy(template = PageTemplate.SinglePortrait)).single()

        assertEquals("slot-1", slot.slotId)
        assertEquals(96.666f, slot.widthMm, 0.01f)
        assertEquals(145f, slot.heightMm, 0.01f)
        assertEquals((285f - 96.666f) / 2f, slot.xMm, 0.05f)
        assertEquals(16f, slot.yMm, 0.01f)
    }

    @Test
    fun `all template slots stay inside content area`() {
        PageTemplate.entries.forEach { template ->
            val slots = TemplateLayoutEngine.calculateSlots(input.copy(template = template))
            slots.forEach { slot ->
                assertTrue("${template.name} ${slot.slotId} left", slot.xMm >= PhotobookLayoutDefaults.SAFE_MARGIN_MM)
                assertTrue("${template.name} ${slot.slotId} top", slot.yMm >= PhotobookLayoutDefaults.SAFE_MARGIN_MM)
                assertTrue("${template.name} ${slot.slotId} right", slot.xMm + slot.widthMm <= 285f - 16f)
                assertTrue("${template.name} ${slot.slotId} bottom", slot.yMm + slot.heightMm <= 210f - 16f - 20f)
            }
        }
    }
}
```

- [ ] **Step 2: Write auto-layout tests first**

Replace the template assertions in `AutoLayoutAlgorithmTest.kt` with:

```kotlin
@Test
fun `layout chooses landscape single for landscape memory`() {
    val memories = listOf(testMemory(id = 1, width = 1200, height = 800))
    val photobook = PhotobookEntity(name = "Test", paperSize = "TWELVE_INCH_LANDSCAPE", createdAt = 0, updatedAt = 0)

    val result = AutoLayoutAlgorithm.layout(memories, PaperSize.TWELVE_INCH_LANDSCAPE, photobook)

    assertEquals(PageTemplate.SingleLandscape, result.pages.single().template)
    assertEquals(1200, result.pages.single().slots.single().imageRef?.imageWidth)
    assertEquals(800, result.pages.single().slots.single().imageRef?.imageHeight)
}

@Test
fun `layout chooses portrait single for portrait memory`() {
    val memories = listOf(testMemory(id = 2, width = 800, height = 1200))
    val photobook = PhotobookEntity(name = "Test", paperSize = "TWELVE_INCH_LANDSCAPE", createdAt = 0, updatedAt = 0)

    val result = AutoLayoutAlgorithm.layout(memories, PaperSize.TWELVE_INCH_LANDSCAPE, photobook)

    assertEquals(PageTemplate.SinglePortrait, result.pages.single().template)
}

@Test
fun `create single photo page falls back to landscape when dimensions are unknown`() {
    val page = AutoLayoutAlgorithm.createSinglePhotoPage(
        imageRef = ImageRef(memoryId = 1, imageUri = "content://test/1", sourceImageIndex = 0),
        moodText = null,
        paperSize = PaperSize.TWELVE_INCH_LANDSCAPE,
        pageNumber = 1
    )

    assertEquals(PageTemplate.SingleLandscape, page.template)
}
```

- [ ] **Step 3: Run focused failing tests**

```bash
./gradlew :app:testDebugUnitTest --tests "com.yingjian.feature.photobook.layout.TemplateLayoutEngineTest" --tests "com.yingjian.feature.photobook.layout.AutoLayoutAlgorithmTest"
```

Expected: fail until new layout input fields and template names are implemented.

- [ ] **Step 4: Implement layout engine**

Update `LayoutInput`:

```kotlin
data class LayoutInput(
    val trimWidthMm: Float,
    val trimHeightMm: Float,
    val bleedMm: Float,
    val safeMarginMm: Float,
    val bottomTextReserveMm: Float = PhotobookLayoutDefaults.BOTTOM_TEXT_RESERVE_MM,
    val gutterMm: Float = PhotobookLayoutDefaults.GUTTER_MM,
    val template: PageTemplate
)
```

Update `calculateSlots()` so the top of the function computes the content area:

```kotlin
val contentX = input.safeMarginMm
val contentY = input.safeMarginMm
val contentWidth = input.trimWidthMm - input.safeMarginMm * 2f
val contentHeight = input.trimHeightMm - input.safeMarginMm * 2f - input.bottomTextReserveMm

fun centered(widthMm: Float, heightMm: Float, slotId: String = "slot-1") = SlotRectMm(
    slotId = slotId,
    xMm = contentX + (contentWidth - widthMm) / 2f,
    yMm = contentY + (contentHeight - heightMm) / 2f,
    widthMm = widthMm,
    heightMm = heightMm
)
```

Use this `when` body:

```kotlin
return when (input.template) {
    PageTemplate.SingleLandscape -> {
        val width = minOf(contentWidth, contentHeight * 1.5f)
        val height = width / 1.5f
        listOf(centered(width, height))
    }
    PageTemplate.SinglePortrait -> {
        val height = minOf(contentHeight, contentWidth * 1.5f)
        val width = height * (2f / 3f)
        listOf(centered(width, height))
    }
    PageTemplate.TwoHorizontal -> {
        val slotHeight = (contentHeight - input.gutterMm) / 2f
        listOf(
            SlotRectMm("slot-1", contentX, contentY, contentWidth, slotHeight),
            SlotRectMm("slot-2", contentX, contentY + slotHeight + input.gutterMm, contentWidth, slotHeight)
        )
    }
    PageTemplate.TwoVertical -> {
        val slotWidth = (contentWidth - input.gutterMm) / 2f
        listOf(
            SlotRectMm("slot-1", contentX, contentY, slotWidth, contentHeight),
            SlotRectMm("slot-2", contentX + slotWidth + input.gutterMm, contentY, slotWidth, contentHeight)
        )
    }
    PageTemplate.GridFour -> {
        val slotWidth = (contentWidth - input.gutterMm) / 2f
        val slotHeight = (contentHeight - input.gutterMm) / 2f
        listOf(
            SlotRectMm("slot-1", contentX, contentY, slotWidth, slotHeight),
            SlotRectMm("slot-2", contentX + slotWidth + input.gutterMm, contentY, slotWidth, slotHeight),
            SlotRectMm("slot-3", contentX, contentY + slotHeight + input.gutterMm, slotWidth, slotHeight),
            SlotRectMm("slot-4", contentX + slotWidth + input.gutterMm, contentY + slotHeight + input.gutterMm, slotWidth, slotHeight)
        )
    }
}
```

- [ ] **Step 5: Implement selected photo metadata**

Update `SelectedMemoryPhoto`:

```kotlin
@Serializable
data class SelectedMemoryPhoto(
    val memoryId: Long,
    val imageUri: String,
    val sourceImageIndex: Int?,
    val sourceImageId: Long? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null
)
```

Update `toSelectablePhotos()` so every selected photo carries memory dimensions:

```kotlin
return sourceUris.mapIndexed { index, uri ->
    SelectedMemoryPhoto(
        memoryId = id,
        imageUri = uri,
        sourceImageIndex = index,
        imageWidth = imageWidth,
        imageHeight = imageHeight
    )
}
```

- [ ] **Step 6: Implement auto-layout template selection**

In `AutoLayoutAlgorithm`, add:

```kotlin
private fun chooseSingleTemplate(imageWidth: Int?, imageHeight: Int?): PageTemplate {
    val width = imageWidth ?: return PageTemplate.SingleLandscape
    val height = imageHeight ?: return PageTemplate.SingleLandscape
    if (width <= 0 || height <= 0) return PageTemplate.SingleLandscape
    return if (width.toFloat() / height.toFloat() >= 1f) {
        PageTemplate.SingleLandscape
    } else {
        PageTemplate.SinglePortrait
    }
}
```

Update `layout()` image ref creation:

```kotlin
val imageRef = ImageRef(
    memoryId = memory.id,
    imageUri = memory.imageUri,
    sourceImageIndex = 0,
    imageWidth = memory.imageWidth,
    imageHeight = memory.imageHeight
)
```

Update `PageState` creation:

```kotlin
template = chooseSingleTemplate(memory.imageWidth, memory.imageHeight)
```

Update `createSinglePhotoPage()` signature and body:

```kotlin
fun createSinglePhotoPage(
    imageRef: ImageRef,
    moodText: String?,
    paperSize: PaperSize,
    pageNumber: Int,
    imageWidth: Int? = imageRef.imageWidth,
    imageHeight: Int? = imageRef.imageHeight
): PageState = PageState(
    pageNumber = pageNumber,
    template = chooseSingleTemplate(imageWidth, imageHeight),
    slots = listOf(ImageSlot(slotId = "slot-1", imageRef = imageRef)),
    textElements = if (moodText.isNullOrBlank()) emptyList() else listOf(
        TextElement(
            text = moodText,
            xMm = paperSize.widthMm * 0.15f,
            yMm = paperSize.heightMm - 22f,
            widthMm = paperSize.widthMm * 0.7f,
            heightMm = 10f,
            rotationDeg = 0f,
            zIndex = 1
        )
    ),
    trimWidthMm = paperSize.widthMm,
    trimHeightMm = paperSize.heightMm
)
```

- [ ] **Step 7: Update `PhotobookViewModel` callers**

Where `ImageRef` is created from `SelectedMemoryPhoto`, include dimensions:

```kotlin
ImageRef(
    memoryId = photo.memoryId,
    imageUri = photo.imageUri,
    sourceImageIndex = photo.sourceImageIndex,
    sourceImageId = photo.sourceImageId,
    imageWidth = photo.imageWidth,
    imageHeight = photo.imageHeight
)
```

When calling `createSinglePhotoPage`, pass dimensions explicitly:

```kotlin
AutoLayoutAlgorithm.createSinglePhotoPage(
    imageRef = imageRef,
    moodText = moodText,
    paperSize = PaperSize.TWELVE_INCH_LANDSCAPE,
    pageNumber = index + 1,
    imageWidth = photo.imageWidth,
    imageHeight = photo.imageHeight
)
```

- [ ] **Step 8: Run task tests**

```bash
./gradlew :app:testDebugUnitTest --tests "com.yingjian.feature.photobook.layout.TemplateLayoutEngineTest" --tests "com.yingjian.feature.photobook.layout.AutoLayoutAlgorithmTest"
```

Expected: all selected tests pass.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/layout/TemplateLayoutEngine.kt \
        app/src/main/java/com/yingjian/feature/photobook/layout/AutoLayoutAlgorithm.kt \
        app/src/main/java/com/yingjian/feature/photobook/SelectedMemoryPhoto.kt \
        app/src/main/java/com/yingjian/feature/photobook/PhotobookViewModel.kt \
        app/src/test/java/com/yingjian/feature/photobook/layout/TemplateLayoutEngineTest.kt \
        app/src/test/java/com/yingjian/feature/photobook/layout/AutoLayoutAlgorithmTest.kt
git commit -m "feat: add portrait-aware photobook auto layout"
```

---

## Task 3: Cover Layout Model, Serializer, And Database Fields

**Files:**
- Create: `app/src/main/java/com/yingjian/feature/photobook/model/CoverLayout.kt`
- Create: `app/src/main/java/com/yingjian/core/util/CoverLayoutSerializer.kt`
- Modify: `app/src/main/java/com/yingjian/core/data/database/YingJianDatabase.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/model/BookState.kt`
- Create: `app/src/test/java/com/yingjian/feature/photobook/model/CoverLayoutTest.kt`
- Create: `app/src/test/java/com/yingjian/core/util/CoverLayoutSerializerTest.kt`

- [ ] **Step 1: Write cover layout tests**

Create `CoverLayoutTest.kt`:

```kotlin
package com.yingjian.feature.photobook.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoverLayoutTest {
    @Test
    fun `default cover layout stays inside page bounds`() {
        val layout = CoverLayoutDefaults.defaultCover(title = "My Book", subtitle = "2026")

        assertEquals(CoverPageType.Cover, layout.pageType)
        assertEquals("#AAA194", layout.backgroundColor)
        assertTrue(layout.textElements.any { it.id == "cover-title" && it.role == CoverTextRole.Title })
        layout.textElements.forEach { element ->
            assertTrue(element.xMm >= 0f)
            assertTrue(element.yMm >= 0f)
            assertTrue(element.xMm + element.widthMm <= 285f)
            assertTrue(element.yMm + element.heightMm <= 210f)
        }
    }

    @Test
    fun `alignment maps to compose and pdf values`() {
        assertEquals(androidx.compose.ui.text.style.TextAlign.Start, PhotobookTextAlign.Start.toComposeTextAlign())
        assertEquals(android.graphics.Paint.Align.CENTER, PhotobookTextAlign.Center.toPaintAlign())
        assertEquals(android.text.Layout.Alignment.ALIGN_OPPOSITE, PhotobookTextAlign.End.toStaticLayoutAlignment())
    }
}
```

Create `CoverLayoutSerializerTest.kt`:

```kotlin
package com.yingjian.core.util

import com.yingjian.feature.photobook.model.CoverLayoutDefaults
import org.junit.Assert.assertEquals
import org.junit.Test

class CoverLayoutSerializerTest {
    @Test
    fun `cover layout round trips through json`() {
        val layout = CoverLayoutDefaults.defaultBackCover(
            title = "Back",
            subtitle = "Subtitle",
            dateText = "2026.05.16"
        )

        val json = CoverLayoutSerializer.serialize(layout)
        val decoded = CoverLayoutSerializer.deserialize(json)

        assertEquals(layout, decoded)
    }
}
```

- [ ] **Step 2: Run focused failing tests**

```bash
./gradlew :app:testDebugUnitTest --tests "com.yingjian.feature.photobook.model.CoverLayoutTest" --tests "com.yingjian.core.util.CoverLayoutSerializerTest"
```

Expected: fail because cover layout model and serializer do not exist.

- [ ] **Step 3: Implement `CoverLayout.kt`**

Create `CoverLayout.kt`:

```kotlin
package com.yingjian.feature.photobook.model

import android.graphics.Paint
import android.text.Layout
import androidx.compose.ui.text.style.TextAlign

enum class CoverPageType { Cover, BackCover }

enum class CoverTextRole { Title, Subtitle, Divider, Date }

enum class PhotobookTextAlign {
    Start,
    Center,
    End
}

fun PhotobookTextAlign.toComposeTextAlign(): TextAlign = when (this) {
    PhotobookTextAlign.Start -> TextAlign.Start
    PhotobookTextAlign.Center -> TextAlign.Center
    PhotobookTextAlign.End -> TextAlign.End
}

fun PhotobookTextAlign.toPaintAlign(): Paint.Align = when (this) {
    PhotobookTextAlign.Start -> Paint.Align.LEFT
    PhotobookTextAlign.Center -> Paint.Align.CENTER
    PhotobookTextAlign.End -> Paint.Align.RIGHT
}

fun PhotobookTextAlign.toStaticLayoutAlignment(): Layout.Alignment = when (this) {
    PhotobookTextAlign.Start -> Layout.Alignment.ALIGN_NORMAL
    PhotobookTextAlign.Center -> Layout.Alignment.ALIGN_CENTER
    PhotobookTextAlign.End -> Layout.Alignment.ALIGN_OPPOSITE
}

data class CoverLayout(
    val pageType: CoverPageType,
    val backgroundColor: String,
    val textElements: List<CoverTextElement>
)

data class CoverTextElement(
    val id: String,
    val role: CoverTextRole,
    val text: String,
    val xMm: Float,
    val yMm: Float,
    val widthMm: Float,
    val heightMm: Float,
    val fontSizeMm: Float,
    val letterSpacing: Float,
    val textAlign: PhotobookTextAlign
)

object CoverLayoutDefaults {
    const val BACKGROUND = "#AAA194"

    fun defaultCover(title: String, subtitle: String): CoverLayout = CoverLayout(
        pageType = CoverPageType.Cover,
        backgroundColor = BACKGROUND,
        textElements = listOf(
            CoverTextElement("cover-title", CoverTextRole.Title, title, 62.5f, 82f, 160f, 18f, 10f, 0f, PhotobookTextAlign.Center),
            CoverTextElement("cover-divider", CoverTextRole.Divider, "", 117.5f, 105f, 50f, 1f, 1f, 0f, PhotobookTextAlign.Center),
            CoverTextElement("cover-subtitle", CoverTextRole.Subtitle, subtitle, 72.5f, 112f, 140f, 12f, 5f, 0f, PhotobookTextAlign.Center)
        )
    )

    fun defaultBackCover(title: String, subtitle: String, dateText: String): CoverLayout = CoverLayout(
        pageType = CoverPageType.BackCover,
        backgroundColor = BACKGROUND,
        textElements = listOf(
            CoverTextElement("back-title", CoverTextRole.Title, title, 72.5f, 84f, 140f, 14f, 8f, 0f, PhotobookTextAlign.Center),
            CoverTextElement("back-subtitle", CoverTextRole.Subtitle, subtitle, 72.5f, 103f, 140f, 10f, 5f, 0f, PhotobookTextAlign.Center),
            CoverTextElement("back-date", CoverTextRole.Date, dateText, 92.5f, 118f, 100f, 8f, 4f, 0f, PhotobookTextAlign.Center)
        )
    )
}
```

- [ ] **Step 4: Implement cover layout serializer**

Create `CoverLayoutSerializer.kt`:

```kotlin
package com.yingjian.core.util

import com.yingjian.feature.photobook.model.CoverLayout
import com.yingjian.feature.photobook.model.CoverPageType
import com.yingjian.feature.photobook.model.CoverTextElement
import com.yingjian.feature.photobook.model.CoverTextRole
import com.yingjian.feature.photobook.model.PhotobookTextAlign
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class CoverLayoutDto(
    val pageType: String,
    val backgroundColor: String,
    val textElements: List<CoverTextElementDto>
)

@Serializable
private data class CoverTextElementDto(
    val id: String,
    val role: String,
    val text: String,
    val xMm: Float,
    val yMm: Float,
    val widthMm: Float,
    val heightMm: Float,
    val fontSizeMm: Float,
    val letterSpacing: Float,
    val textAlign: String
)

object CoverLayoutSerializer {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    fun serialize(layout: CoverLayout): String = json.encodeToString(layout.toDto())

    fun deserialize(raw: String): CoverLayout = json.decodeFromString<CoverLayoutDto>(raw).toDomain()

    private fun CoverLayout.toDto(): CoverLayoutDto = CoverLayoutDto(
        pageType = pageType.name,
        backgroundColor = backgroundColor,
        textElements = textElements.map { it.toDto() }
    )

    private fun CoverTextElement.toDto(): CoverTextElementDto = CoverTextElementDto(
        id = id,
        role = role.name,
        text = text,
        xMm = xMm,
        yMm = yMm,
        widthMm = widthMm,
        heightMm = heightMm,
        fontSizeMm = fontSizeMm,
        letterSpacing = letterSpacing,
        textAlign = textAlign.name
    )

    private fun CoverLayoutDto.toDomain(): CoverLayout = CoverLayout(
        pageType = CoverPageType.valueOf(pageType),
        backgroundColor = backgroundColor,
        textElements = textElements.map { it.toDomain() }
    )

    private fun CoverTextElementDto.toDomain(): CoverTextElement = CoverTextElement(
        id = id,
        role = CoverTextRole.valueOf(role),
        text = text,
        xMm = xMm,
        yMm = yMm,
        widthMm = widthMm,
        heightMm = heightMm,
        fontSizeMm = fontSizeMm,
        letterSpacing = letterSpacing,
        textAlign = PhotobookTextAlign.valueOf(textAlign)
    )
}
```

- [ ] **Step 5: Update Room entity and version**

Modify `PhotobookEntity` in `YingJianDatabase.kt`:

```kotlin
val coverLayoutJson: String? = null,
val backCoverLayoutJson: String? = null,
val createdAt: Long,
val updatedAt: Long
```

Modify database annotation:

```kotlin
@androidx.room.Database(
    entities = [MemoryRecordEntity::class, PhotobookEntity::class, PageLayoutEntity::class],
    version = 6,
    autoMigrations = [
        androidx.room.AutoMigration(from = 1, to = 2),
        androidx.room.AutoMigration(from = 2, to = 3),
        androidx.room.AutoMigration(from = 3, to = 4),
        androidx.room.AutoMigration(from = 4, to = 5)
    ],
    exportSchema = true
)
```

Then find the Room builder and add destructive migration from 5 to 6:

```bash
rg -n "databaseBuilder|fallbackToDestructiveMigration|YingJianDatabase" app/src/main/java
```

At the builder site, add:

```kotlin
.fallbackToDestructiveMigration()
```

This is allowed because the app is still in internal testing.

- [ ] **Step 6: Update `BookState` defaults**

Add `coverLayout` and `backCoverLayout` to `BookState` while preserving existing fields:

```kotlin
data class BookState(
    val photobook: com.yingjian.core.data.database.PhotobookEntity,
    val pages: List<PageState>,
    val currentPage: Int,
    val mode: LayoutMode,
    val selection: EditorSelection = EditorSelection.None,
    val coverLayout: CoverLayout = CoverLayoutDefaults.defaultCover(
        title = photobook.coverTitle ?: photobook.name,
        subtitle = photobook.coverSubtitle ?: ""
    ),
    val backCoverLayout: CoverLayout = CoverLayoutDefaults.defaultBackCover(
        title = photobook.backTitle ?: photobook.name,
        subtitle = photobook.backSubtitle ?: "",
        dateText = photobook.backDateText ?: ""
    ),
    val previousManualState: BookState? = null
)
```

Add typed selection in the same file:

```kotlin
sealed interface EditorSelection {
    data class ImageSlot(val pageIndex: Int, val slotId: String) : EditorSelection
    data class CoverText(val pageType: CoverPageType, val textId: String) : EditorSelection
    data object None : EditorSelection
}
```

- [ ] **Step 7: Run task tests**

```bash
./gradlew :app:testDebugUnitTest --tests "com.yingjian.feature.photobook.model.CoverLayoutTest" --tests "com.yingjian.core.util.CoverLayoutSerializerTest"
```

Expected: all selected tests pass.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/model/CoverLayout.kt \
        app/src/main/java/com/yingjian/core/util/CoverLayoutSerializer.kt \
        app/src/main/java/com/yingjian/core/data/database/YingJianDatabase.kt \
        app/src/main/java/com/yingjian/feature/photobook/model/BookState.kt \
        app/src/test/java/com/yingjian/feature/photobook/model/CoverLayoutTest.kt \
        app/src/test/java/com/yingjian/core/util/CoverLayoutSerializerTest.kt
git commit -m "feat: add photobook cover layout model"
```

---

## Task 4: Slot Actions, Typed Selection, And Page Mutation Helpers

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookSlotActions.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/model/BookState.kt`
- Modify: `app/src/test/java/com/yingjian/feature/photobook/PhotobookSlotActionsTest.kt`

- [ ] **Step 1: Write slot action tests**

Add these tests to `PhotobookSlotActionsTest.kt`:

```kotlin
@Test
fun `switching grid to single keeps inactive slot images`() {
    val page = page(
        PageTemplate.GridFour,
        listOf(slot("slot-1", 1), slot("slot-2", 2), slot("slot-3", 3), slot("slot-4", 4))
    )

    val result = PhotobookSlotActions.changeTemplate(page, PageTemplate.SingleLandscape)

    val changed = result as TemplateChangeResult.Changed
    assertEquals(PageTemplate.SingleLandscape, changed.page.template)
    assertEquals(4, changed.page.slots.size)
    assertEquals("content://image/1", changed.page.slots.first { it.slotId == "slot-1" }.imageRef?.imageUri)
    assertEquals("content://image/2", changed.page.slots.first { it.slotId == "slot-2" }.imageRef?.imageUri)
}

@Test
fun `visible slots only returns active template slots`() {
    val page = page(
        PageTemplate.SingleLandscape,
        listOf(slot("slot-1", 1), slot("slot-2", 2))
    )

    val visible = PhotobookSlotActions.visibleSlots(page)

    assertEquals(listOf("slot-1"), visible.map { it.slotId })
}

@Test
fun `fill slot updates exact target slot`() {
    val page = page(PageTemplate.GridFour, listOf(empty("slot-1"), empty("slot-2"), empty("slot-3"), empty("slot-4")))

    val updated = PhotobookSlotActions.fillSlot(page, "slot-3", ImageRef(9, "content://image/9", 0))

    assertEquals("content://image/9", updated.slots.first { it.slotId == "slot-3" }.imageRef?.imageUri)
    assertNull(updated.slots.first { it.slotId == "slot-1" }.imageRef)
}

@Test
fun `insert page after current renumbers pages`() {
    val pages = listOf(
        page(PageTemplate.SingleLandscape, listOf(slot("slot-1", 1)), pageNumber = 1),
        page(PageTemplate.SingleLandscape, listOf(slot("slot-1", 2)), pageNumber = 2)
    )
    val newPage = PhotobookSlotActions.emptyPage(pageNumber = 0, template = PageTemplate.SingleLandscape)

    val updated = PhotobookSlotActions.insertPageAfter(pages, currentPageIndex = 0, newPage = newPage)

    assertEquals(listOf(1, 2, 3), updated.map { it.pageNumber })
    assertTrue(updated[1].slots.single().isEmpty)
}
```

- [ ] **Step 2: Run focused failing tests**

```bash
./gradlew :app:testDebugUnitTest --tests "com.yingjian.feature.photobook.PhotobookSlotActionsTest"
```

Expected: fail until helpers are implemented and template names are updated.

- [ ] **Step 3: Implement action result without overflow blocking**

Update `changeTemplate()` to preserve inactive slots:

```kotlin
fun changeTemplate(page: PageState, newTemplate: PageTemplate): TemplateChangeResult {
    val existingById = page.slots.associateBy { it.slotId }
    val activeSlots = newTemplate.slotIds.map { slotId ->
        existingById[slotId] ?: ImageSlot(slotId = slotId, imageRef = null)
    }
    val inactiveSlots = page.slots.filterNot { it.slotId in newTemplate.slotIds }
    return TemplateChangeResult.Changed(page.copy(template = newTemplate, slots = activeSlots + inactiveSlots))
}
```

Add visible, fill, delete, empty page, and insert helpers:

```kotlin
fun visibleSlots(page: PageState): List<ImageSlot> =
    page.template.slotIds.map { slotId ->
        page.slots.firstOrNull { it.slotId == slotId } ?: ImageSlot(slotId = slotId, imageRef = null)
    }

fun fillSlot(page: PageState, slotId: String, imageRef: ImageRef): PageState {
    val existing = page.slots.associateBy { it.slotId }
    val updated = existing[slotId]?.copy(imageRef = imageRef, cropScale = 1f, cropOffsetX = 0f, cropOffsetY = 0f)
        ?: ImageSlot(slotId = slotId, imageRef = imageRef)
    val slots = page.slots.filterNot { it.slotId == slotId } + updated
    return page.copy(slots = page.template.slotIds.map { id -> slots.first { it.slotId == id } } + slots.filterNot { it.slotId in page.template.slotIds })
}

fun clearSlot(page: PageState, slotId: String): PageState = page.copy(
    slots = page.slots.map { slot ->
        if (slot.slotId == slotId) slot.copy(imageRef = null, cropScale = 1f, cropOffsetX = 0f, cropOffsetY = 0f) else slot
    }
)

fun emptyPage(pageNumber: Int, template: PageTemplate): PageState = PageState(
    pageNumber = pageNumber,
    template = template,
    slots = emptySlotsFor(template),
    trimWidthMm = 285f,
    trimHeightMm = 210f
)

fun insertPageAfter(pages: List<PageState>, currentPageIndex: Int, newPage: PageState): List<PageState> {
    val insertIndex = (currentPageIndex + 1).coerceIn(0, pages.size)
    return (pages.take(insertIndex) + newPage + pages.drop(insertIndex)).mapIndexed { index, page ->
        page.copy(pageNumber = index + 1)
    }
}
```

- [ ] **Step 4: Update tests helpers to new template names**

Use `PageTemplate.SingleLandscape` in helper tests. Update helper function:

```kotlin
private fun slot(id: String, memoryId: Long) = ImageSlot(
    slotId = id,
    imageRef = ImageRef(memoryId, "content://image/$memoryId", 0)
)
```

- [ ] **Step 5: Run task tests**

```bash
./gradlew :app:testDebugUnitTest --tests "com.yingjian.feature.photobook.PhotobookSlotActionsTest"
```

Expected: all selected tests pass.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/PhotobookSlotActions.kt \
        app/src/main/java/com/yingjian/feature/photobook/model/BookState.kt \
        app/src/test/java/com/yingjian/feature/photobook/PhotobookSlotActionsTest.kt
git commit -m "feat: add photobook slot mutation helpers"
```

---

## Task 5: Shared Stage, Content Renderer, And Floating Toolbar

**Files:**
- Create: `app/src/main/java/com/yingjian/feature/photobook/PhotobookStage.kt`
- Create: `app/src/main/java/com/yingjian/feature/photobook/ContentPageRenderer.kt`
- Create: `app/src/main/java/com/yingjian/feature/photobook/FloatingPhotobookToolbar.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookCanvasPage.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookEditorScreen.kt`

- [ ] **Step 1: Create shared stage component**

Create `PhotobookStage.kt`:

```kotlin
package com.yingjian.feature.photobook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yingjian.feature.photobook.model.PhotobookLayoutDefaults

@Composable
fun PhotobookStage(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 340.dp,
    backgroundColor: Color = Color(PhotobookLayoutDefaults.CONTENT_PAGE_COLOR),
    content: @Composable BoxScope.(scaleFactor: Float) -> Unit
) {
    val scaleFactor = maxWidth.value / PhotobookLayoutDefaults.PAGE_WIDTH_MM
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(PhotobookLayoutDefaults.PAGE_WIDTH_MM / PhotobookLayoutDefaults.PAGE_HEIGHT_MM)
            .shadow(4.dp, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
    ) {
        content(scaleFactor)
    }
}
```

- [ ] **Step 2: Create content page renderer**

Create `ContentPageRenderer.kt` by moving slot rendering logic from `PhotobookCanvasPage.kt`. The public API should be:

```kotlin
@Composable
fun ContentPageRenderer(
    pageState: PageState,
    selectedSlotId: String?,
    onSlotSelected: (String) -> Unit,
    onEmptySlotAddClicked: (String) -> Unit,
    onSlotImageAdjusted: (slotId: String, offsetXMm: Float, offsetYMm: Float, scale: Float) -> Unit,
    modifier: Modifier = Modifier,
    moodText: String? = null,
    memoryDate: Long? = null
)
```

When rendering slots, use only visible slots:

```kotlin
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
```

Empty slot click must call the exact target:

```kotlin
if (slot?.imageRef == null) {
    Text(
        text = "+",
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.clickable { onEmptySlotAddClicked(rect.slotId) }
    )
}
```

- [ ] **Step 3: Create floating toolbar**

Create `FloatingPhotobookToolbar.kt`:

```kotlin
package com.yingjian.feature.photobook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ToolbarAction { Layout, Text, Move, Delete, AddPage }

@Composable
fun FloatingPhotobookToolbar(
    onLayoutClicked: () -> Unit,
    onTextClicked: () -> Unit,
    onMoveClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onAddPageClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconButton(onClick = onLayoutClicked, modifier = Modifier.size(44.dp)) { LayoutTemplateIcon() }
        IconButton(onClick = onTextClicked, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.TextFields, contentDescription = "文本") }
        IconButton(onClick = onMoveClicked, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.DriveFileMove, contentDescription = "移动") }
        IconButton(onClick = onDeleteClicked, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.Delete, contentDescription = "删除") }
        IconButton(onClick = onAddPageClicked, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.Add, contentDescription = "添加页面") }
    }
}

@Composable
private fun LayoutTemplateIcon() {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.size(24.dp)) {
        drawRoundRect(color, Offset(3.dp.toPx(), 3.dp.toPx()), Size(10.dp.toPx(), 18.dp.toPx()), CornerRadius(2.dp.toPx()))
        drawRoundRect(color, Offset(15.dp.toPx(), 3.dp.toPx()), Size(6.dp.toPx(), 8.dp.toPx()), CornerRadius(1.5.dp.toPx()))
        drawRoundRect(color, Offset(15.dp.toPx(), 13.dp.toPx()), Size(6.dp.toPx(), 8.dp.toPx()), CornerRadius(1.5.dp.toPx()))
    }
}
```

- [ ] **Step 4: Refactor editor screen structure**

In `PhotobookEditorScreen.kt`, remove the bottom "添加照片" button branch and replace the bottom template selector with toolbar actions. Keep `TopAppBar`, then use a workspace `Box`:

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
) {
    AnimatedContent(
        targetState = currentLeafIndex,
        modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 24.dp)
    ) { leafIndex ->
        // cover/content/back rendering
    }

    FloatingPhotobookToolbar(
        onLayoutClicked = { showLayoutSheet = true },
        onTextClicked = { handleTextAction() },
        onMoveClicked = { showMoveSheet = true },
        onDeleteClicked = { handleDeleteAction() },
        onAddPageClicked = { handleAddPage() },
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 12.dp)
    )

    PageNavigationRow(
        currentLeafIndex = currentLeafIndex,
        leafCount = leafCount,
        onSelectLeaf = ::selectLeaf,
        modifier = Modifier.align(Alignment.BottomCenter)
    )
}
```

The actual helper functions can be local composable functions or private composables in the same file. Keep leaf navigation behavior: cover first, back cover last.

- [ ] **Step 5: Run compile check**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: compile succeeds.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/PhotobookStage.kt \
        app/src/main/java/com/yingjian/feature/photobook/ContentPageRenderer.kt \
        app/src/main/java/com/yingjian/feature/photobook/FloatingPhotobookToolbar.kt \
        app/src/main/java/com/yingjian/feature/photobook/PhotobookCanvasPage.kt \
        app/src/main/java/com/yingjian/feature/photobook/PhotobookEditorScreen.kt
git commit -m "feat: add shared photobook editor stage"
```

---

## Task 6: Photo Picker Modes And Empty Slot Fill Flow

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotoPickerScreen.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookEditorScreen.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookViewModel.kt`
- Modify: `app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt`

- [ ] **Step 1: Add explicit pending slot fill state**

Create this model near other editor state models in `BookState.kt`:

```kotlin
data class PendingSlotFill(
    val pageIndex: Int,
    val slotId: String
)
```

Use route-level `rememberSaveable` state in `YingJianNavHost.kt` for the navigation handoff:

```kotlin
var pendingSlotFillPageIndex by rememberSaveable { mutableStateOf<Int?>(null) }
var pendingSlotFillSlotId by rememberSaveable { mutableStateOf<String?>(null) }

fun setPendingSlotFill(fill: PendingSlotFill) {
    pendingSlotFillPageIndex = fill.pageIndex
    pendingSlotFillSlotId = fill.slotId
}

fun clearPendingSlotFill() {
    pendingSlotFillPageIndex = null
    pendingSlotFillSlotId = null
}
```

- [ ] **Step 2: Ensure picker returns dimensions**

Update `toSelectablePhotos()` from Task 2. Confirm each returned item has:

```kotlin
SelectedMemoryPhoto(
    memoryId = id,
    imageUri = uri,
    sourceImageIndex = index,
    imageWidth = imageWidth,
    imageHeight = imageHeight
)
```

- [ ] **Step 3: Make `SingleSlot` return exactly one selected photo**

In `PhotoPickerScreen`, keep existing immediate return behavior for single slot, but make the equality check stable:

```kotlin
private fun SelectedMemoryPhoto.sameImageAs(other: SelectedMemoryPhoto): Boolean =
    memoryId == other.memoryId &&
        imageUri == other.imageUri &&
        sourceImageIndex == other.sourceImageIndex
```

Use it in selection checks:

```kotlin
val isSelected = selectedPhotos.any { it.sameImageAs(photo) }
```

For `SingleSlot` click:

```kotlin
selectedPhotos.clear()
selectedPhotos.add(photo)
onComplete(listOf(photo))
```

- [ ] **Step 4: Wire empty slot plus to picker navigation**

In `ContentPageRenderer` empty slot click, call `onEmptySlotAddClicked(slotId)`.

In `PhotobookEditorScreen`, when current content page is `cp`:

```kotlin
onEmptySlotAddClicked = { slotId ->
    onUpdatePendingSlotFill(PendingSlotFill(cp, slotId))
    onFillSelectedSlot()
}
```

If `PhotobookEditorScreen` cannot own navigation directly, expose:

```kotlin
onEmptySlotAddClicked: (pageIndex: Int, slotId: String) -> Unit
```

and route it through `YingJianNavHost.kt`.

- [ ] **Step 5: Fill exact slot on picker return**

In the navigation return handler, use:

```kotlin
val pending = pendingSlotFill ?: return@handler
val selectedPhoto = selectedPhotos.singleOrNull() ?: return@handler
val imageRef = ImageRef(
    memoryId = selectedPhoto.memoryId,
    imageUri = selectedPhoto.imageUri,
    sourceImageIndex = selectedPhoto.sourceImageIndex,
    sourceImageId = selectedPhoto.sourceImageId,
    imageWidth = selectedPhoto.imageWidth,
    imageHeight = selectedPhoto.imageHeight
)
val updatedPages = currentBookState.pages.mapIndexed { index, page ->
    if (index == pending.pageIndex) PhotobookSlotActions.fillSlot(page, pending.slotId, imageRef) else page
}
currentBookState = currentBookState.copy(pages = updatedPages)
pendingSlotFill = null
```

- [ ] **Step 6: Verify with compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: compile succeeds.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/PhotoPickerScreen.kt \
        app/src/main/java/com/yingjian/feature/photobook/PhotobookEditorScreen.kt \
        app/src/main/java/com/yingjian/feature/photobook/PhotobookViewModel.kt \
        app/src/main/java/com/yingjian/feature/photobook/model/BookState.kt \
        app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt
git commit -m "feat: fix photobook empty slot fill flow"
```

---

## Task 7: Cover And Back-Cover Rendering With Movable Text

**Files:**
- Create: `app/src/main/java/com/yingjian/feature/photobook/CoverPageRenderer.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookCoverPages.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookEditorScreen.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookViewModel.kt`

- [ ] **Step 1: Create `CoverPageRenderer`**

Create `CoverPageRenderer.kt`:

```kotlin
package com.yingjian.feature.photobook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yingjian.feature.photobook.model.CoverLayout
import com.yingjian.feature.photobook.model.CoverTextRole
import com.yingjian.feature.photobook.model.PhotobookLayoutDefaults
import com.yingjian.feature.photobook.model.toComposeTextAlign
import kotlin.math.roundToInt

@Composable
fun CoverPageRenderer(
    layout: CoverLayout,
    selectedTextId: String?,
    onTextSelected: (String) -> Unit,
    onTextMoved: (textId: String, xMm: Float, yMm: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    PhotobookStage(
        modifier = modifier,
        backgroundColor = Color(android.graphics.Color.parseColor(layout.backgroundColor))
    ) { scaleFactor ->
        layout.textElements.forEach { element ->
            val isSelected = selectedTextId == element.id
            if (element.role == CoverTextRole.Divider) {
                Canvas(
                    modifier = Modifier
                        .offset {
                            with(density) {
                                IntOffset((element.xMm * scaleFactor).dp.roundToPx(), (element.yMm * scaleFactor).dp.roundToPx())
                            }
                        }
                        .size((element.widthMm * scaleFactor).dp, (element.heightMm * scaleFactor).dp)
                ) {
                    drawLine(
                        color = Color(0xFF4C463E),
                        start = Offset.Zero,
                        end = Offset(size.width, 0f),
                        strokeWidth = size.height.coerceAtLeast(1f)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .offset {
                            with(density) {
                                IntOffset((element.xMm * scaleFactor).dp.roundToPx(), (element.yMm * scaleFactor).dp.roundToPx())
                            }
                        }
                        .size((element.widthMm * scaleFactor).dp, (element.heightMm * scaleFactor).dp)
                        .then(if (isSelected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary) else Modifier)
                        .pointerInput(element.id) {
                            detectDragGestures(
                                onDragStart = { onTextSelected(element.id) },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val dxMm = dragAmount.x / density.density / scaleFactor
                                    val dyMm = dragAmount.y / density.density / scaleFactor
                                    val nextX = (element.xMm + dxMm).coerceIn(0f, PhotobookLayoutDefaults.PAGE_WIDTH_MM - element.widthMm)
                                    val nextY = (element.yMm + dyMm).coerceIn(0f, PhotobookLayoutDefaults.PAGE_HEIGHT_MM - element.heightMm)
                                    onTextMoved(element.id, nextX, nextY)
                                }
                            )
                        }
                ) {
                    Text(
                        text = element.text,
                        fontSize = (element.fontSizeMm * scaleFactor).sp,
                        fontWeight = FontWeight.Light,
                        textAlign = element.textAlign.toComposeTextAlign(),
                        color = Color(0xFF4C463E)
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Replace cover editor fields with renderer plus text action**

In `PhotobookEditorScreen`, render cover leaf:

```kotlin
CoverPageRenderer(
    layout = bookState.coverLayout,
    selectedTextId = (bookState.selection as? EditorSelection.CoverText)
        ?.takeIf { it.pageType == CoverPageType.Cover }
        ?.textId,
    onTextSelected = { textId ->
        onUpdateState(bookState.copy(selection = EditorSelection.CoverText(CoverPageType.Cover, textId)))
    },
    onTextMoved = { textId, xMm, yMm ->
        onUpdateState(bookState.copy(coverLayout = bookState.coverLayout.moveText(textId, xMm, yMm)))
    }
)
```

Add extension in `CoverLayout.kt`:

```kotlin
fun CoverLayout.moveText(textId: String, xMm: Float, yMm: Float): CoverLayout = copy(
    textElements = textElements.map { element ->
        if (element.id == textId) element.copy(xMm = xMm, yMm = yMm) else element
    }
)
```

Repeat with `CoverPageType.BackCover` and `bookState.backCoverLayout`.

- [ ] **Step 3: Persist moved cover layouts on save**

In `PhotobookViewModel` save/update logic, serialize layouts:

```kotlin
val updatedPhotobook = bookState.photobook.copy(
    coverLayoutJson = CoverLayoutSerializer.serialize(bookState.coverLayout),
    backCoverLayoutJson = CoverLayoutSerializer.serialize(bookState.backCoverLayout),
    updatedAt = System.currentTimeMillis()
)
```

When loading a photobook, decode layouts with fallback:

```kotlin
val coverLayout = photobook.coverLayoutJson
    ?.let { runCatching { CoverLayoutSerializer.deserialize(it) }.getOrNull() }
    ?: CoverLayoutDefaults.defaultCover(photobook.coverTitle ?: photobook.name, photobook.coverSubtitle ?: "")

val backCoverLayout = photobook.backCoverLayoutJson
    ?.let { runCatching { CoverLayoutSerializer.deserialize(it) }.getOrNull() }
    ?: CoverLayoutDefaults.defaultBackCover(photobook.backTitle ?: photobook.name, photobook.backSubtitle ?: "", photobook.backDateText ?: "")
```

- [ ] **Step 4: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: compile succeeds.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/CoverPageRenderer.kt \
        app/src/main/java/com/yingjian/feature/photobook/PhotobookCoverPages.kt \
        app/src/main/java/com/yingjian/feature/photobook/PhotobookEditorScreen.kt \
        app/src/main/java/com/yingjian/feature/photobook/PhotobookViewModel.kt \
        app/src/main/java/com/yingjian/feature/photobook/model/CoverLayout.kt
git commit -m "feat: render movable photobook cover text"
```

---

## Task 8: Preview And PDF Shared Rendering Data

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookPreviewScreen.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/export/PdfExportUtil.kt`
- Modify: `app/src/test/java/com/yingjian/feature/photobook/layout/TemplateLayoutEngineTest.kt`

- [ ] **Step 1: Update preview page list**

In `PhotobookPreviewScreen.kt`, keep the current orientation behavior:

```kotlin
val showSpread = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
```

Build leaves in this order:

```kotlin
sealed interface PreviewLeaf {
    data object Cover : PreviewLeaf
    data class Content(val pageIndex: Int) : PreviewLeaf
    data object BackCover : PreviewLeaf
}

val leaves = remember(bookState.pages.size) {
    listOf(PreviewLeaf.Cover) +
        bookState.pages.indices.map { PreviewLeaf.Content(it) } +
        listOf(PreviewLeaf.BackCover)
}
```

Render `CoverPageRenderer` for cover/back with no movement callbacks:

```kotlin
CoverPageRenderer(
    layout = bookState.coverLayout,
    selectedTextId = null,
    onTextSelected = {},
    onTextMoved = { _, _, _ -> }
)
```

Render content with `ContentPageRenderer` and no selection:

```kotlin
ContentPageRenderer(
    pageState = bookState.pages[pageIndex],
    selectedSlotId = null,
    onSlotSelected = {},
    onEmptySlotAddClicked = {},
    onSlotImageAdjusted = { _, _, _, _ -> }
)
```

- [ ] **Step 2: Update PDF cover rendering**

In `PdfExportUtil.kt`, replace hardcoded `renderCoverPage` and `renderBackPage` text positions with:

```kotlin
private fun renderCoverLayout(canvas: Canvas, layout: CoverLayout, typeface: Typeface) {
    canvas.drawColor(Color.parseColor(layout.backgroundColor))
    layout.textElements.forEach { element ->
        if (element.role == CoverTextRole.Divider) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = PDF_TEXT_COLOR
                strokeWidth = mmToPxFloat(element.heightMm).coerceAtLeast(1f)
            }
            canvas.drawLine(
                mmToPxFloat(element.xMm),
                mmToPxFloat(element.yMm),
                mmToPxFloat(element.xMm + element.widthMm),
                mmToPxFloat(element.yMm),
                paint
            )
        } else {
            renderCoverText(canvas, element, typeface)
        }
    }
}
```

Add:

```kotlin
private fun renderCoverText(canvas: Canvas, element: CoverTextElement, typeface: Typeface) {
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = mmToPxFloat(element.fontSizeMm)
        color = PDF_TEXT_COLOR
        textAlign = element.textAlign.toPaintAlign()
    }
    val widthPx = mmToPxFloat(element.widthMm).toInt()
    val layout = StaticLayout.Builder
        .obtain(element.text, 0, element.text.length, paint, widthPx)
        .setAlignment(element.textAlign.toStaticLayoutAlignment())
        .setLineSpacing(0f, 1.2f)
        .setIncludePad(false)
        .build()
    canvas.save()
    canvas.translate(mmToPxFloat(element.xMm), mmToPxFloat(element.yMm))
    layout.draw(canvas)
    canvas.restore()
}
```

Use it in `exportPdf()`:

```kotlin
renderCoverLayout(coverPage.canvas, bookState.coverLayout, typeface)
renderCoverLayout(backPage.canvas, bookState.backCoverLayout, typeface)
```

- [ ] **Step 3: Preserve content PDF memory safety**

Keep the existing `MAX_DECODE_DIMENSION_PX`, `calculateInSampleSize()`, `bitmap.recycle()`, and one-image-at-a-time rendering flow. Do not replace it with eager full-resolution image loading.

- [ ] **Step 4: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: compile succeeds.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/PhotobookPreviewScreen.kt \
        app/src/main/java/com/yingjian/feature/photobook/export/PdfExportUtil.kt \
        app/src/test/java/com/yingjian/feature/photobook/layout/TemplateLayoutEngineTest.kt
git commit -m "feat: align photobook preview and pdf rendering"
```

---

## Task 9: Final Verification, Manual QA, And APK

**Files:**
- Modify only if a previous task left compile or test failures.

- [ ] **Step 1: Run full unit tests**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Run Kotlin compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Build debug APK**

Use Android Studio bundled JBR when local Java is not already configured:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

APK path:

```text
app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 4: Manual QA checklist on device**

Install the APK and validate:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Manual checks:

- Create a photobook from mixed landscape and portrait photos.
- Confirm landscape photos create `SingleLandscape` pages.
- Confirm portrait photos create `SinglePortrait` pages.
- Confirm every content image frame is centered inside the page with bottom text still visible.
- Confirm cover is first and back cover is last.
- Confirm portrait device orientation preview shows one page.
- Confirm landscape device orientation preview shows two-page spread.
- Open editor and confirm stage position does not jump between cover, content, and back cover.
- Tap layout toolbar and switch through all five templates.
- Switch four-grid to single and back to four-grid; previously hidden images reappear in their old slots.
- Tap an empty slot `+`, choose a photo, and confirm that exact slot fills.
- Use move action to previous page, next page, new page, and chosen page.
- Try moving to a full page and confirm replacement/change-layout/new-page/cancel options appear.
- Move cover title, save, reopen, and confirm coordinates persist.
- Export PDF and confirm first page is cover, last page is back cover, and content slots match editor.

- [ ] **Step 5: Inspect git status before final commit**

```bash
git status --short
```

Expected: only intended source/test/schema files are modified. Do not stage:

- `.DS_Store`
- `.gradle/`
- `.idea/`
- `app/build/`
- `build/`
- `.superpowers/`
- reference folders such as `stitch_yingjian_*`
- review reports unless explicitly requested

- [ ] **Step 6: Final commit if verification required fixes**

```bash
git add <only files fixed during final verification>
git commit -m "fix: stabilize photobook editor redesign"
```

Skip this commit when Task 9 made no source changes.

---

## Execution Notes For Claude

- Start from the repository branch that contains this plan and the approved spec.
- Before coding, run:

```bash
git status --short
git branch --show-current
git log --oneline -5
```

- Do not run `git reset --hard`, `git checkout -- .`, `git restore .`, or `git clean -fd` unless the human explicitly approves after a snapshot.
- Do not stage broad paths with `git add .` because this repository has many unrelated untracked files.
- Commit after each task when tests pass.
- Keep each task's write set close to the files listed in that task.
- Preserve recent PDF crash/OOM protections in `PdfExportUtil`.

## Self-Review

### Spec Coverage

- Fixed 285x210 stage: covered by Tasks 1, 5, 8.
- Five content templates: covered by Tasks 1, 2, 4, 5.
- Landscape/portrait single auto import: covered by Task 2.
- Empty slot `+` fill flow: covered by Tasks 5 and 6.
- Remove bottom add-photo button: covered by Task 5.
- Icon-only floating toolbar: covered by Task 5.
- Cover/back-cover same size and movable text: covered by Tasks 3, 7, 8.
- Shared preview/PDF render data: covered by Task 8.
- Destructive internal migration policy: covered by Task 3.
- `BookState` field preservation: covered by Task 3.
- `AutoLayoutAlgorithm.layout()` no legacy `Single`: covered by Task 2.
- Manual QA and APK: covered by Task 9.

### Placeholder Scan

This plan contains no forbidden placeholder markers. Where implementation depends on current call sites, the exact search command and required decision are provided.

### Type Consistency

- `sourceImageId` remains `Long?` across `SelectedMemoryPhoto` and `ImageRef`.
- `PageTemplate.SingleLandscape` and `PageTemplate.SinglePortrait` replace legacy `Single`.
- `PhotobookTextAlign` is used only for new cover/back-cover layout JSON.
- `EditorSelection` replaces `selectedSlotId` as the single editor selection source.
- `CoverLayout` is persisted through `coverLayoutJson` and `backCoverLayoutJson`.
