# Photobook Editor Stage Redesign Spec

Date: 2026-05-16
Project: `/Users/haos/Project/Android/YingJian`
Implementation owner: Claude
Review owner: Codex
Status: Ready for implementation after review fixes

## Goal

Redesign the photobook editor around one fixed 285x210 landscape page stage, five content-page templates, a minimal floating toolbar, reliable empty-slot image filling, movable cover/back-cover text, and shared rendering logic for editor, preview, and PDF.

The implementation must keep layout data and rendering math unified. Editor, preview, and PDF must consume the same page dimensions, template rectangles, crop data, and cover text coordinates.

## Confirmed Product Decisions

- Cover, content pages, and back cover are all 285x210 landscape pages.
- Page orientation never changes. "Landscape single" and "portrait single" describe only the image-slot shape.
- New content pages default to landscape single-image layout.
- First import creates one page per selected photo.
- First import automatically chooses landscape single or portrait single based on photo aspect ratio.
- Empty slots show a centered `+`.
- Clicking an empty slot `+` opens single-photo selection and fills that exact slot.
- The old bottom "add photo" button is removed.
- Top toolbar contains icon-only actions: layout, text, move, delete, add page.
- The toolbar uses the reference floating pill style, but does not show text labels.
- The layout icon uses the supplied shape: one tall rectangle on the left, two stacked rectangles on the right.
- Cover/back cover visual style follows `stitch_yingjian_fengmian`.
- Cover/back-cover text can be moved and saved.
- Old serialized test data does not need compatibility. If needed, uninstall/reinstall or use a destructive database migration.

## Reference Assets

- Editor toolbar style reference: `/Users/haos/Project/Android/YingJian/stitch_yingjian_huacev2/_1/code.html`
- Editor screenshot reference: `/Users/haos/Project/Android/YingJian/stitch_yingjian_huacev2/_1/screen.png`
- Cover/back-cover style reference: `/Users/haos/Project/Android/YingJian/stitch_yingjian_fengmian/screen.png`
- Cover/back-cover design tokens: `/Users/haos/Project/Android/YingJian/stitch_yingjian_fengmian/DESIGN.md`

## Non-Goals

- Do not build a free-form full canvas editor.
- Do not change the physical page size away from 285x210.
- Do not make portrait pages.
- Do not auto-absorb photos from adjacent pages when changing layout.
- Do not restore the bottom add-photo button.
- Do not solve unrelated memory/timeline UI issues in this task.
- Do not add snapping/guides for cover/back-cover text in this phase.

## Current Problems To Fix

1. Single-image layout cannot distinguish landscape and portrait image slots, so portrait photos are cropped poorly.
2. Editor toolbar and add-photo controls affect vertical layout, causing cover, back cover, and content pages to appear at different vertical positions.
3. Cover/back cover are visually different sizes from content pages.
4. Cover/back-cover text is edited through fields, not through page-positioned text elements.
5. Empty slot image filling is unreliable because the return path does not carry a stable `(pageIndex, slotId)` target.
6. Content image slots are not reliably centered because layout math is not strict enough in the shared layout engine.
7. Some current slot models duplicate layout rectangles, which can drift away from `TemplateLayoutEngine`.

## Data Compatibility Policy

This app is still in internal testing. No old-user compatibility is required for photobook layout data.

Implementation requirements:

- Bump `PageLayoutDocument.version` to `2`.
- If Room schema changes are needed, bump the database version from the current version to the next version.
- Destructive migration is acceptable for photobook layout changes.
- Existing test photobooks may be discarded.
- Existing legacy enum value `Single` may be deleted or mapped to `SingleLandscape`; mapping is optional because old data compatibility is not required.

## Page Templates

Replace the current four-template model with five templates:

```kotlin
enum class PageTemplate(val slotIds: List<String>) {
    SingleLandscape(listOf("slot-1")),
    SinglePortrait(listOf("slot-1")),
    TwoHorizontal(listOf("slot-1", "slot-2")),
    TwoVertical(listOf("slot-1", "slot-2")),
    GridFour(listOf("slot-1", "slot-2", "slot-3", "slot-4"));

    val capacity: Int get() = slotIds.size
}
```

Implementation notes:

- Replace all current references to `PageTemplate.Single` with `PageTemplate.SingleLandscape`, except where auto import selects `SinglePortrait`.
- Keep slot ids stable exactly as shown.
- Add-page always creates `SingleLandscape`.
- User layout selection can switch between all five templates.

## Selected Photo Metadata

Current code paths use selected photo objects to create pages. Auto layout needs deterministic image dimensions.

Use this model or an equivalent extension:

```kotlin
data class SelectedMemoryPhoto(
    val memoryId: Long,
    val imageUri: String,
    val sourceImageIndex: Int,
    val sourceImageId: Long? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null
)
```

Rules:

- The photo picker should populate `imageWidth` and `imageHeight` when available from metadata.
- If dimensions are not available in the picker, resolve them before calling the page factory.
- If metadata resolution fails, fall back to `SingleLandscape`.
- Keep `sourceImageId` as `Long?` to match the current `SelectedMemoryPhoto` and `ImageRef` model.

Also extend `ImageRef` with optional image dimensions if the dimensions need to travel with saved page data:

```kotlin
data class ImageRef(
    val uri: String,
    val memoryId: Long,
    val sourceImageIndex: Int = 0,
    val sourceImageId: Long? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null
)
```

## Auto Import Rules

When creating pages from selected photos:

- Each selected photo creates one content page.
- If `imageWidth / imageHeight >= 1.0`, use `SingleLandscape`.
- If `imageWidth / imageHeight < 1.0`, use `SinglePortrait`.
- If width/height is unknown, zero, or invalid, use `SingleLandscape`.
- Initial slot state:
  - `cropScale = 1f`
  - `cropOffsetX = 0f`
  - `cropOffsetY = 0f`
  - `fitMode = FitMode.Crop`

Current `AutoLayoutAlgorithm.createSinglePhotoPage(...)` should be aligned with the current code signature:

```kotlin
fun createSinglePhotoPage(
    imageRef: ImageRef,
    moodText: String?,
    paperSize: PaperSize,
    pageNumber: Int,
    imageWidth: Int? = null,
    imageHeight: Int? = null
): PageState
```

Alternative acceptable implementation:

- Keep the existing signature.
- Add a new wrapper/factory that accepts `SelectedMemoryPhoto`, determines the template, and then constructs `PageState`.

Do not make `AutoLayoutAlgorithm` calculate slot rectangles. Its role is page creation and template choice. `TemplateLayoutEngine` remains the only source of slot geometry.

`AutoLayoutAlgorithm.layout(records: List<MemoryRecordEntity>, ...)` still exists in the current code. Before changing it:

- Search all call sites.
- If no current feature path uses it, mark it deprecated or remove it with tests updated.
- If any feature path still uses it, update it to produce `SingleLandscape`/`SinglePortrait` pages with the same metadata rules.
- Do not leave a batch import path that still creates legacy `PageTemplate.Single`.

## Content Slot Model

`TemplateLayoutEngine` is the only source of slot rectangles. Persisted image slots must not store rectangle geometry.

Use this slot model shape:

```kotlin
data class ImageSlot(
    val slotId: String,
    val imageRef: ImageRef? = null,
    val cropScale: Float = 1f,
    val cropOffsetX: Float = 0f,
    val cropOffsetY: Float = 0f,
    val fitMode: FitMode = FitMode.Crop
)
```

Requirements:

- Remove `xMm`, `yMm`, `widthMm`, and `heightMm` from persisted `ImageSlot`.
- Remove or stop using legacy `PageState.elements` rectangle shims for content images.
- If removing all legacy fields is too broad for one implementation pass, mark them deprecated and ensure editor, preview, and PDF ignore them.
- Layout changes should keep existing filled images in matching slot ids where possible.
- Images in slots that are not part of the current template are retained as inactive page data, not rendered, not moved, and not deleted automatically.
- If the user switches back to a template containing that slot id, the retained image appears again.

## Layout Change Behavior

Changing a page template must not automatically pull photos from adjacent pages and must not automatically delete photos.

Rules:

- Switching from a lower-capacity template to a higher-capacity template keeps existing matching slots and creates empty visible slots for the new capacity.
- Switching from a higher-capacity template to a lower-capacity template keeps non-visible slot images as inactive page data.
- Inactive slot images are excluded from editor rendering, preview rendering, and PDF export while their slot id is not part of the active template.
- Delete action can remove an inactive image only through an explicit management UI if such UI is implemented; otherwise inactive images are simply preserved until the user switches to a template that exposes them.
- Empty visible slots still use the centered `+` fill flow.

## Layout Defaults

Define shared print-layout constants in one place, for example:

```kotlin
object PhotobookLayoutDefaults {
    const val PAGE_WIDTH_MM = 285f
    const val PAGE_HEIGHT_MM = 210f
    const val SAFE_MARGIN_MM = 16f
    const val BOTTOM_TEXT_RESERVE_MM = 20f
    const val GUTTER_MM = 8f
    const val BLEED_MM = 3f
}
```

Rules:

- Layout math is in millimeters.
- Compose converts millimeters to pixels/dp through the stage scale.
- Do not use arbitrary Compose `.padding(16.dp)` as print margin.
- PDF uses the same millimeter constants and conversion path.

## Layout Engine Requirements

`TemplateLayoutEngine` is the only source of content-page slot rectangles.

Input:

```kotlin
data class LayoutInput(
    val trimWidthMm: Float,
    val trimHeightMm: Float,
    val bleedMm: Float,
    val safeMarginMm: Float,
    val bottomTextReserveMm: Float,
    val gutterMm: Float,
    val template: PageTemplate
)
```

Output:

```kotlin
data class SlotRectMm(
    val slotId: String,
    val xMm: Float,
    val yMm: Float,
    val widthMm: Float,
    val heightMm: Float
)
```

Shared content area:

```kotlin
val contentX = safeMarginMm
val contentY = safeMarginMm
val contentWidth = trimWidthMm - safeMarginMm * 2
val contentHeight = trimHeightMm - safeMarginMm * 2 - bottomTextReserveMm
```

Single-template sizing:

- `SingleLandscape`
  - Desired aspect ratio: `3f / 2f`.
  - `widthMm = min(contentWidth, contentHeight * 1.5f)`.
  - `heightMm = widthMm / 1.5f`.
  - Center inside the content area.
- `SinglePortrait`
  - Desired aspect ratio: `2f / 3f`.
  - `heightMm = min(contentHeight, contentWidth * 1.5f)`.
  - `widthMm = heightMm * (2f / 3f)`.
  - Center inside the content area.

Multi-template rules:

- `TwoHorizontal`: two landscape-like slots stacked vertically with one gutter.
- `TwoVertical`: two portrait-like slots side by side with one gutter.
- `GridFour`: 2x2 grid with one horizontal and one vertical gutter.
- The full slot group must be centered inside the content area.
- Every slot must remain inside the content area.

The "image frame not centered" bug is fixed by changing this engine and its tests, not by patching editor or PDF coordinates separately.

## Editor Layout

The editor screen is divided into stable regions:

1. `TopAppBar`
   - Fixed height.
   - Contains back, title, preview, export, save.
2. `Workspace`
   - Fills remaining area.
   - Background: warm surface container.
3. `FloatingToolbar`
   - Absolute/floating overlay at top center of workspace.
   - Does not participate in the stage's size or vertical centering.
4. `PhotobookStage`
   - One fixed 285x210 landscape stage.
   - Used for cover, content pages, and back cover.
   - Centered consistently for all leaf types.
5. `PageNavigation`
   - Fixed bottom region.
   - Does not move when toolbar state changes.

Switching between cover, content pages, and back cover must not change the stage center.

`BookState.currentPage` remains the current content-page index. The editor route owns a separate `currentLeafIndex` or equivalent value for cover/content/back navigation.

## Floating Toolbar

Actions:

- Layout
- Text
- Move
- Delete
- Add page

Visual style:

- Icon-only.
- No text labels below icons.
- `contentDescription` must provide accessibility names.
- Use the reference for floating pill container, icon stroke weight, spacing, border, and shadow.
- Do not copy any reference labels or dense details.
- Pill-shaped floating container.
- Translucent surface background.
- Soft blur if practical in Compose; if not, use translucent surface.
- Soft outline using `outlineVariant` with low alpha.
- Low-opacity shadow.
- Button hit target: 42-48dp.
- Icon visual size: 22-24dp.
- Selected action uses a light warm container.

Layout icon:

- Use a custom icon.
- 24dp viewport.
- Left rect: `x=3`, `y=3`, `w=10`, `h=18`, corner radius `2`.
- Right top rect: `x=15`, `y=3`, `w=6`, `h=8`, corner radius `1.5`.
- Right bottom rect: `x=15`, `y=13`, `w=6`, `h=8`, corner radius `1.5`.
- Use primary or warm dark gray.
- Must not use dense mosaic/grid details.

Add-page behavior:

- Insert a new content page after the current content page.
- If currently on cover, insert before first content page.
- If currently on back cover, append after last content page.
- New page template is `SingleLandscape`.
- New page has one empty slot.
- Renumber subsequent content pages after insertion.
- Navigate to the new page and select the empty slot.

## Empty Slot Fill Flow

This flow must be fixed explicitly.

State:

```kotlin
data class PendingSlotFill(
    val pageIndex: Int,
    val slotId: String
)
```

Recommended ownership:

- `BookState.selection` stores the current editor selection.
- `PendingSlotFill` can live in `BookState` or in route-level `rememberSaveable` state.
- If route-level state is used, it must survive navigation to the photo picker and back.

Flow:

1. Empty slot displays a centered `+`.
2. User taps `+`.
3. Editor stores `pendingSlotFill = PendingSlotFill(pageIndex, slotId)`.
4. Navigate to `PhotoPickerScreen(mode = SingleSlot)`.
5. User selects one photo.
6. Photo picker returns exactly one selected photo.
7. Editor fills exactly `pendingSlotFill.pageIndex + pendingSlotFill.slotId`.
8. Do not create a new page.
9. Do not replace another slot.
10. Clear `pendingSlotFill`.

If the target page or slot no longer exists when returning, show a lightweight error and do nothing.

## Photo Picker Requirements

The photo picker must support both batch import and single-slot fill.

Required API shape:

```kotlin
enum class MemoryPhotoPickerMode {
    BatchImport,
    SingleSlot
}

@Composable
fun PhotoPickerScreen(
    memories: List<Memory>,
    mode: MemoryPhotoPickerMode,
    initialSelected: List<SelectedMemoryPhoto> = emptyList(),
    onComplete: (List<SelectedMemoryPhoto>) -> Unit,
    onCancel: () -> Unit
)
```

Behavior:

- `BatchImport` supports selecting a whole memory or individual photos from a memory.
- Whole-memory selection selects all photos in that memory.
- Individual-photo selection selects only the tapped photo.
- `SingleSlot` allows exactly one photo and returns a list with one item.
- The picker result must include `memoryId`, `imageUri`, `sourceImageIndex`, optional `sourceImageId`, and image dimensions when available.

## Move Action

The toolbar `Move` action reuses the previous image movement feature.

Expected behavior:

- User selects a filled image slot.
- Tap move.
- Show a `ModalBottomSheet` with movement choices:
  - Move to previous page.
  - Move to next page.
  - Move to new page.
  - Move to chosen page.
- Moving to a page with an empty slot fills the first empty slot.
- Moving to a full page must not silently fail.
- If the target page is full, show a second sheet/dialog with:
  - Replace a selected target slot.
  - Change target page layout.
  - Move to new page.
  - Cancel.
- Moving away from a page does not auto-delete the source page.

`Move to chosen page`:

- Show page numbers and small template thumbnails.
- Disable the current page as a target.
- Cover and back cover are not valid image move targets.

## Delete Action

Delete applies to current selection:

- Filled image slot: clear image from the slot.
- Empty image slot: no-op with optional message.
- Cover/back-cover text: clear or restore text, but do not break the default cover structure.

Deleting an image must not delete the page automatically.

## Text Action

Text applies by page type:

- Content page: if a text element is selected, edit it.
- Content page: if no text element is selected, select the existing mood text.
- Content page: if no text exists, create a default mood text near the bottom center.
- Cover: select title by default if nothing is selected.
- Back cover: select title by default if nothing is selected.

Text edit UI can be a bottom sheet or dialog, but the text itself must render on the page stage at saved coordinates.

## Cover And Back Cover Model

Add layout-based cover data.

Persist text alignment as an app enum, not as Compose `TextAlign`.

Recommended model:

```kotlin
enum class CoverPageType { Cover, BackCover }

enum class CoverTextRole { Title, Subtitle, Divider, Date }

enum class PhotobookTextAlign { Start, Center, End }

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
```

Alignment mapping:

| `PhotobookTextAlign` | Compose | `Paint.Align` | `StaticLayout` alignment |
|---|---|---|---|
| `Start` | `TextAlign.Start` | `Paint.Align.LEFT` | `Layout.Alignment.ALIGN_NORMAL` |
| `Center` | `TextAlign.Center` | `Paint.Align.CENTER` | `Layout.Alignment.ALIGN_CENTER` |
| `End` | `TextAlign.End` | `Paint.Align.RIGHT` | `Layout.Alignment.ALIGN_OPPOSITE` |

The app currently serializes some content text with Compose `TextAlign`. New cover/back-cover layout JSON must use `PhotobookTextAlign`; legacy content text migration can remain separate.

Persistence:

- Add `coverLayoutJson` and `backCoverLayoutJson` to `PhotobookEntity`.
- Current database version should be bumped by one.
- Destructive migration is acceptable.
- Existing fields `coverTitle`, `coverSubtitle`, `backTitle`, `backSubtitle`, `backDateText` may remain temporarily and may be used to seed defaults.
- Do not persist Compose-specific types.

Color tokens:

- Cover/background color: `#AAA194`.
- Content page paper color: `#FAF9F6`.
- Preview outer background may use a dark neutral such as `#30312F`, but this color must not become PDF page background.

Default cover layout:

- Background color: `#AAA194`.
- Title: horizontally centered, above vertical center.
- Divider: centered line below title.
- Subtitle: centered below divider.

Default back cover layout:

- Background color: `#AAA194`.
- Title, subtitle, date: centered vertical stack.

Cover/back-cover text interaction:

- Tap text to select.
- Selected text shows a subtle dashed border.
- Drag text to move.
- Use Compose's default touch slop; no custom snapping/guides in this phase.
- Coordinates update in memory during editing.
- Save persists coordinates.
- Dragging is clamped inside the 285x210 page boundary.
- Reopen after save must restore the moved coordinates.

## Unified Stage And Renderers

Recommended components:

```kotlin
@Composable
fun PhotobookStage(
    leaf: PhotobookLeaf,
    selected: EditorSelection,
    onSelectionChange: (EditorSelection) -> Unit,
    modifier: Modifier = Modifier
)

@Composable
fun ContentPageRenderer(
    page: PageState,
    selectedSlotId: String?,
    onSlotSelected: (String) -> Unit,
    onEmptySlotAddClicked: (String) -> Unit,
    modifier: Modifier = Modifier
)

@Composable
fun CoverPageRenderer(
    layout: CoverLayout,
    selectedTextId: String?,
    onTextSelected: (String) -> Unit,
    onTextMoved: (textId: String, xMm: Float, yMm: Float) -> Unit,
    modifier: Modifier = Modifier
)

@Composable
fun FloatingPhotobookToolbar(
    selectedAction: ToolbarAction?,
    onLayoutClicked: () -> Unit,
    onTextClicked: () -> Unit,
    onMoveClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onAddPageClicked: () -> Unit,
    modifier: Modifier = Modifier
)
```

Responsibilities:

- `PhotobookStage`: owns fixed page aspect ratio, scale, and centering.
- `ContentPageRenderer`: renders slots and images from `TemplateLayoutEngine`.
- `CoverPageRenderer`: renders cover/back-cover background and text elements.
- `FloatingPhotobookToolbar`: displays icon-only actions.

Preview and PDF must consume the same page data:

- Content pages: `TemplateLayoutEngine` slot rectangles.
- Cover/back cover: `CoverLayout` text coordinates.

## Editor State Model

Replace slot-only selection with a typed selection.

Recommended:

```kotlin
sealed interface EditorSelection {
    data class ImageSlot(val pageIndex: Int, val slotId: String) : EditorSelection
    data class CoverText(val pageType: CoverPageType, val textId: String) : EditorSelection
    data object None : EditorSelection
}
```

`BookState` should use typed selection. This is a change list, not a full replacement of the current model:

```kotlin
data class BookState(
    val photobook: PhotobookEntity,
    val pages: List<PageState>,
    val currentPage: Int,
    val selection: EditorSelection = EditorSelection.None,
    val coverLayout: CoverLayout,
    val backCoverLayout: CoverLayout,
    val mode: LayoutMode,
    val previousManualState: BookState? = null,
    val hasUnsavedChanges: Boolean = false
)
```

Notes:

- Preserve existing fields that are still used by current behavior, including `photobook`, `mode`, and `previousManualState`.
- Replace `selectedSlotId: String?` with `selection: EditorSelection`; do not keep two competing selection sources.
- `currentPage` is content-page index only.
- Route/UI layer may maintain `currentLeafIndex` to include cover and back cover.
- `PendingSlotFill` may be route-level state, but must be explicit and navigation-safe.

## Preview Requirements

- Preview first leaf is cover.
- Final leaf is back cover.
- Cover/back cover use the same 285x210 stage size as content pages.
- Portrait device orientation shows one page.
- Landscape device orientation shows two-page spread.
- Do not change the existing one-page vs two-page orientation rule.
- Content slots and cover text positions must match editor.

## PDF Requirements

- PDF page size remains 285x210.
- First page is cover.
- Content pages follow.
- Last page is back cover.
- Cover/back-cover text positions must match editor.
- Content page image slots must match editor and preview.
- PDF uses `PhotobookTextAlign` mapped to platform drawing alignment.
- PDF export must continue to be crash-safe and memory-safe after recent fixes.
- Do not load all full-resolution images into memory at once.

## Tests Required

Add or update unit tests for:

- `PageTemplate` contains five templates and stable slot ids.
- `PageLayoutDocument.version == 2`.
- Auto import chooses `SingleLandscape` for landscape images.
- Auto import chooses `SinglePortrait` for portrait images.
- Auto import falls back to `SingleLandscape` for missing dimensions.
- `TemplateLayoutEngine` centers `SingleLandscape`.
- `TemplateLayoutEngine` centers `SinglePortrait`.
- `TemplateLayoutEngine` keeps every slot inside safe area.
- `ImageSlot` persistence does not depend on rectangle fields.
- `AutoLayoutAlgorithm.layout()` has no path that creates legacy `PageTemplate.Single`.
- Empty slot fill updates the exact `(pageIndex, slotId)`.
- Add-page inserts a `SingleLandscape` empty page.
- Add-page renumbers subsequent content pages.
- Cover layout defaults have expected text ids and coordinates inside page bounds.
- Cover text move clamps coordinates inside page bounds.
- `PhotobookTextAlign` maps correctly to Compose and PDF alignment.

Manual QA:

- Create a photobook from mixed landscape and portrait photos.
- Confirm portrait photo pages use portrait slot.
- Tap layout toolbar and switch between all five templates.
- Add a new page from the toolbar and confirm it is landscape single empty page.
- Tap empty slot `+`, choose a photo, confirm that exact slot fills.
- Confirm batch import can select a whole memory and individual photos.
- Confirm no bottom add-photo button appears.
- Switch between cover, content page, and back cover; stage position must not jump.
- Move cover title, save, reopen, confirm position remains.
- Move an image to previous/next/new/chosen page.
- Export PDF and confirm page sizes/positions match editor.

## Implementation Phases

1. Update model and serialization.
2. Update `SelectedMemoryPhoto` metadata flow.
3. Update `PageTemplate`, `PageLayoutDocument.version`, and page factory logic.
4. Update `TemplateLayoutEngine` and tests.
5. Remove persisted slot rectangle dependency.
6. Add auto import aspect-ratio template selection.
7. Build fixed `PhotobookStage`.
8. Replace editor toolbar with floating icon-only toolbar.
9. Implement add-page action and page renumbering.
10. Fix empty slot `+` fill flow using `PendingSlotFill`.
11. Update photo picker batch/single-slot modes.
12. Add cover/back-cover layout model and renderer.
13. Add cover/back-cover text drag/save.
14. Update preview and PDF to consume shared models.
15. Run unit tests, build APK, and perform manual QA.

## Open Implementation Notes For Claude

- Inspect latest commit before coding. Current recent code commits include PDF crash/OOM fixes.
- Do not revert the recent PDF crash fixes.
- Do not stage `.DS_Store`, build outputs, `.gradle`, `.idea`, reference folders, or review scratch files.
- Stage only intended source/test/schema/doc files.
- Use focused commits after stable phases.
- If any implementation choice conflicts with this spec, update the spec first and ask for review before coding.

## Spec Self-Review

- Completeness scan: product behavior, model fields, layout constants, navigation flow, preview, PDF, and tests are concrete.
- Internal consistency: page size is consistently 285x210 landscape for cover, content pages, and back cover.
- Source-of-truth check: content slot rectangles come only from `TemplateLayoutEngine`; persisted `ImageSlot` stores image/crop state only.
- Current-code alignment: selected-photo metadata, `AutoLayoutAlgorithm`, `BookState`, photo picker modes, and Room migration boundaries are explicitly covered.
- Scope check: this remains one bounded photobook editor redesign and does not introduce a free-form canvas editor.
- Ambiguity check: add-page, empty-slot fill, template selection, cover text movement, move action conflict handling, preview, and PDF behavior are explicitly defined.
