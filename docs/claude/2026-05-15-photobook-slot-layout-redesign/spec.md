# Technical Spec

## Files To Inspect First

- `app/src/main/java/com/yingjian/feature/photobook/PhotobookEditorScreen.kt`
- `app/src/main/java/com/yingjian/feature/photobook/PhotobookPreviewScreen.kt`
- `app/src/main/java/com/yingjian/feature/photobook/PhotobookCanvasPage.kt`
- `app/src/main/java/com/yingjian/feature/photobook/PhotobookCoverPages.kt`
- `app/src/main/java/com/yingjian/feature/photobook/PhotobookViewModel.kt`
- `app/src/main/java/com/yingjian/feature/photobook/export/PdfExportUtil.kt`
- `app/src/main/java/com/yingjian/feature/photobook/layout/AutoLayoutAlgorithm.kt`
- `app/src/main/java/com/yingjian/feature/photobook/model/BookState.kt`
- `app/src/main/java/com/yingjian/feature/photobook/model/PageElement.kt`
- `app/src/main/java/com/yingjian/core/data/repository/PhotobookRepository.kt`
- `app/src/main/java/com/yingjian/core/data/database/YingJianDatabase.kt`

## Core Model

Replace the current single-image-biased page model with an explicit template and slot model.

Recommended shape:

```kotlin
enum class PageTemplate {
    Single,
    TwoHorizontal,
    TwoVertical,
    GridFour
}

data class PageState(
    val pageNumber: Int,
    val template: PageTemplate,
    val slots: List<ImageSlot>,
    val textElements: List<TextElement>,
    val trimWidthMm: Float,
    val trimHeightMm: Float,
    val bleedMm: Float = 3f
)

data class ImageSlot(
    val slotId: String,
    val imageRef: ImageRef?,
    val cropScale: Float = 1f,
    val cropOffsetX: Float = 0f,
    val cropOffsetY: Float = 0f,
    val fitMode: FitMode = FitMode.Crop
)

data class ImageRef(
    val memoryId: Long,
    val imageUri: String,
    val sourceImageIndex: Int?,
    val sourceImageId: Long? = null
)

enum class FitMode {
    Crop,
    Fit
}
```

Notes:

- `PageTemplate` defines the number and placement of image slots.
- `ImageSlot` owns the image and its crop transform.
- `ImageRef` represents a single selected photo from a memory.
- `TextElement` may reuse the current type if it is still needed for mood/date rendering.
- Do not keep using `filterIsInstance<ImageElement>().firstOrNull()` as the main content-page assumption.

## Page Layout Document

No old layout compatibility is required. Persist content pages as the new document shape:

```kotlin
data class PageLayoutDocument(
    val version: Int = 1,
    val template: PageTemplate,
    val slots: List<ImageSlot>,
    val textElements: List<TextElement>
)
```

Implementation options:

- Keep the current DB field name that stores layout JSON, but write `PageLayoutDocument` JSON into it.
- Avoid a large Room schema refactor in this phase unless implementation requires it.
- If schema changes are made, destructive migration is acceptable for this testing-stage app.

## Book Leaves

Use an explicit leaf/page abstraction for editor and preview:

```kotlin
sealed interface PhotobookLeaf {
    data class Cover(...) : PhotobookLeaf
    data class Content(val page: PageState) : PhotobookLeaf
    data class BackCover(...) : PhotobookLeaf
}
```

Leaf list:

```kotlin
val leaves = listOf(Cover(...)) + contentPages.map { Content(it) } + listOf(BackCover(...))
```

This prevents off-by-one bugs around cover, content page index, and back cover.

## Layout Engine

Add a single shared layout engine. Editor, preview, and PDF must all use it.

Recommended types:

```kotlin
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
    fun calculateSlots(input: LayoutInput): List<SlotRectMm>
}
```

Responsibilities:

- Calculate slot rectangles in millimeters.
- Respect trim size.
- Respect bleed configuration.
- Keep images within a safe content area.
- Reserve enough visual spacing so images do not push mood text/date outside the page.
- Apply consistent gutters between slots.

Renderer conversions:

- Compose editor: mm to dp.
- Compose preview: mm to dp.
- PDF export: mm to PDF units.

Do not duplicate template math in editor, preview, and PDF.

## Slot Crop Rendering

Each slot has two coordinate layers:

1. Slot rectangle from `TemplateLayoutEngine`.
2. Image crop transform from `ImageSlot`.

Rendering rules:

- Slot rectangle clips image content.
- `cropScale` scales image around slot center.
- `cropOffsetX` and `cropOffsetY` move the image inside the slot.
- Clamp offset so the image cannot expose blank space unless `FitMode.Fit` intentionally allows letterboxing.
- Save crop values only in the slot model.

## Editor State

Recommended selected state:

```kotlin
data class EditorSelection(
    val leafIndex: Int,
    val pageNumber: Int?,
    val slotId: String?
)
```

Use slot selection instead of element-index selection.

Editor actions:

- Select slot.
- Change page template.
- Fill empty slot.
- Replace slot image.
- Delete slot image.
- Move selected image to previous page.
- Move selected image to next page.
- Move selected image to new page.
- Move selected image to chosen page.
- Swap selected image with another slot on the same page.
- Reset selected image crop.
- Adjust selected image crop position and zoom.

Save behavior:

- User crop/template/slot changes should persist when tapping Save.
- Do not silently discard `cropScale`, `cropOffsetX`, or `cropOffsetY`.

## Memory Photo Selection

Add a picker contract that returns photos, not just memories.

Recommended selection item:

```kotlin
data class SelectedMemoryPhoto(
    val memoryId: Long,
    val imageUri: String,
    val sourceImageIndex: Int?,
    val sourceImageId: Long? = null
)
```

Picker modes:

```kotlin
enum class MemoryPhotoPickerMode {
    BatchImport,
    SingleSlot
}
```

Batch import:

- Allows selecting whole memories.
- Allows selecting individual photos inside a memory.
- Returns multiple `SelectedMemoryPhoto`.

Single slot:

- Allows choosing exactly one photo.
- Returns one `SelectedMemoryPhoto`.

## Template Change Algorithm

Pseudo-rules:

```kotlin
fun changeTemplate(page: PageState, newTemplate: PageTemplate): TemplateChangeResult
```

- Calculate new slot ids from `newTemplate`.
- Preserve existing filled slots by slot order.
- If filled image count fits the new template, return updated page.
- If filled image count exceeds new template capacity, return a result that requires user decision.

Overflow options:

- Keep selected image.
- Keep first N images and move overflow images to new `Single` pages.
- Cancel.

## Move Algorithm

Pseudo-rules:

```kotlin
fun moveImage(
    sourcePage: PageState,
    sourceSlotId: String,
    targetPage: PageState
): MoveResult
```

- If target has empty slot, move into first empty slot.
- If target is full, return `MoveResult.TargetFull`.
- On success, clear source slot.
- Keep source page even if it becomes empty.

For target full:

- UI must ask user whether to replace a slot, change target layout, move to a new page, or cancel.

## Preview

Use `PhotobookLeaf` list.

Rules:

- Start at leaf index `0`.
- Portrait renders one leaf.
- Landscape renders two joined leaves for interior spreads.
- Cover is the first leaf.
- Back cover is the last leaf.
- Navigation should be leaf-index based, not content-page-index based.

Known failure to avoid:

- Tapping next from the second visible page must not jump back to cover.

## PDF Export

PDF export must consume the same page model and layout engine:

- Cover renderer.
- Content page renderer using `TemplateLayoutEngine` and slot crop values.
- Back cover renderer.

Required assertion:

- Exported page count equals `contentPageCount + 2`.

## Suggested File Organization

Possible additions:

- `app/src/main/java/com/yingjian/feature/photobook/model/PageTemplate.kt`
- `app/src/main/java/com/yingjian/feature/photobook/model/ImageSlot.kt`
- `app/src/main/java/com/yingjian/feature/photobook/model/PageLayoutDocument.kt`
- `app/src/main/java/com/yingjian/feature/photobook/layout/TemplateLayoutEngine.kt`
- `app/src/main/java/com/yingjian/feature/photobook/selection/MemoryPhotoPickerMode.kt`

Use existing package conventions if they differ.

