# Photobook Editor Stage Redesign Spec

Date: 2026-05-16
Project: `/Users/haos/Project/Android/YingJian`
Implementation owner: Claude
Review owner: Codex

## Goal

Redesign the photobook editor around a fixed 285x210 page stage, five content-page templates, a minimal floating toolbar, reliable empty-slot image filling, and movable cover/back-cover text. Editor, preview, and PDF must share the same page dimensions and layout data.

## Confirmed Product Decisions

- Cover, content pages, and back cover are all 285x210 landscape pages.
- Page orientation never changes. "Landscape single" and "portrait single" only describe image-slot shape.
- New content pages default to landscape single-image layout.
- First import creates one page per selected photo.
- First import automatically chooses landscape single or portrait single based on photo aspect ratio.
- Empty slots show a centered `+`.
- Clicking an empty slot `+` opens single-photo selection and fills that exact slot.
- The old bottom "add photo" button is removed.
- Top toolbar contains icon-only actions: layout, text, move, delete, add page.
- The layout icon uses the supplied shape: one tall rectangle on the left, two stacked rectangles on the right.
- The floating toolbar uses the style from `stitch_yingjian_huacev2/_1/code.html`: translucent pill, soft border, light shadow, minimal line icons.
- Cover/back cover visual style follows `stitch_yingjian_fengmian`.
- Cover/back-cover text can be moved and saved.

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

## Current Problems To Fix

1. Single-image layout cannot distinguish landscape and portrait image slots, so portrait photos are cropped poorly.
2. Editor toolbar and add-photo controls affect vertical layout, causing cover, back cover, and content pages to appear at different vertical positions.
3. Cover/back cover are visually different sizes from content pages.
4. Cover/back-cover text is edited through fields, not through page-positioned text elements.
5. Empty slot image filling has been unreliable because the return path does not carry a stable `(pageIndex, slotId)` target.
6. Content image slots are not reliably centered because layout math is not strict enough in the shared layout engine.

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

Migration inside the testing app can be simple:

- If existing code still creates `Single`, replace it with `SingleLandscape`.
- No old-user compatibility is required.

## Auto Import Rules

When creating pages from selected photos:

- Each photo creates one content page.
- If photo width/height ratio is `>= 1.0`, use `SingleLandscape`.
- If photo width/height ratio is `< 1.0`, use `SinglePortrait`.
- If width/height is unknown or invalid, use `SingleLandscape`.
- Initial slot state:
  - `cropScale = 1f`
  - `cropOffsetX = 0f`
  - `cropOffsetY = 0f`
  - `fitMode = FitMode.Crop`

Claude must use actual image metadata when available. If the selected photo model does not include width/height, extend it or resolve metadata before page creation.

## Layout Engine Requirements

`TemplateLayoutEngine` is the only source of content-page slot rectangles.

Input:

```kotlin
data class LayoutInput(
    val trimWidthMm: Float,
    val trimHeightMm: Float,
    val bleedMm: Float,
    val safeMarginMm: Float,
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

Rules:

- Work in millimeters.
- Page size is 285x210.
- Calculate a safe content area first.
- Reserve bottom room for page number and text.
- Center each template's slot group inside the safe content area.
- `SingleLandscape` produces one centered landscape rectangle.
- `SinglePortrait` produces one centered portrait rectangle.
- `TwoHorizontal` produces two centered horizontal slots.
- `TwoVertical` produces two centered vertical slots.
- `GridFour` produces four centered slots.
- Editor, preview, and PDF must all consume this same output.

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
- Pill-shaped floating container.
- Translucent surface background.
- Soft blur if practical in Compose; if not, use translucent surface.
- Soft outline using `outlineVariant` with low alpha.
- Low-opacity shadow.
- Button hit target: 42-48dp.
- Icon visual size: 22-24dp.
- Selected action uses a light warm container.

Layout icon:

- Left tall rounded rectangle.
- Right two stacked rounded rectangles.
- Use primary or warm dark gray.
- Must not use dense mosaic/grid details.

Add-page behavior:

- Insert a new content page after the current content page.
- If currently on cover, insert before first content page.
- If currently on back cover, append after last content page.
- New page template is `SingleLandscape`.
- New page has one empty slot.
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

Flow:

1. Empty slot displays a centered `+`.
2. User taps `+`.
3. Editor stores `pendingFillSlot = PendingSlotFill(pageIndex, slotId)`.
4. Navigate to `PhotoPickerScreen(mode = SingleSlot)`.
5. User selects one photo.
6. Photo picker returns `selectedSlotPhoto`.
7. Editor fills exactly `pendingFillSlot.pageIndex + pendingFillSlot.slotId`.
8. Do not create a new page.
9. Do not replace another slot.
10. Clear `pendingFillSlot`.

If the target page or slot no longer exists when returning, show a lightweight error and do nothing.

## Move Action

The toolbar `Move` action reuses the previous image movement feature.

Expected behavior:

- User selects a filled image slot.
- Tap move.
- Show movement choices:
  - Move to previous page.
  - Move to next page.
  - Move to new page.
  - Move to chosen page.
- Moving to a page with an empty slot fills the first empty slot.
- Moving to a full page must not silently fail; show replacement/change-layout/new-page/cancel choices.
- Moving away from a page does not auto-delete the source page.

## Delete Action

Delete applies to current selection:

- Filled image slot: clear image from the slot.
- Empty image slot: no-op with optional message.
- Cover/back-cover text: clear or restore text, but do not break the default cover structure.

Deleting an image must not delete the page automatically.

## Text Action

Text applies by page type:

- Content page: edit page mood/text element if selected; otherwise select existing text element or create one.
- Cover: select title by default if nothing is selected.
- Back cover: select title by default if nothing is selected.

Text edit UI can be a bottom sheet or dialog, but the text itself must render on the page stage at saved coordinates.

## Cover And Back Cover Model

Add layout-based cover data.

Recommended model:

```kotlin
enum class CoverPageType { Cover, BackCover }

enum class CoverTextRole { Title, Subtitle, Divider, Date }

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
    val textAlign: TextAlign
)
```

Persistence options:

- Preferred: add `coverLayoutJson` and `backCoverLayoutJson` to `PhotobookEntity`.
- Destructive migration is acceptable because the app is still in testing.
- Existing fields `coverTitle`, `coverSubtitle`, `backTitle`, `backSubtitle`, `backDateText` may be used to seed default layouts.

Default cover layout:

- Background color: warm gray cover color from reference.
- Title: horizontally centered, above vertical center.
- Divider: centered line below title.
- Subtitle: centered below divider.

Default back cover layout:

- Background color: same warm gray cover color.
- Title, subtitle, date: centered vertical stack.

Cover/back-cover text interaction:

- Tap text to select.
- Selected text shows a subtle dashed border.
- Drag text to move.
- Coordinates update in memory during editing.
- Save persists coordinates.
- Text cannot be dragged outside the 285x210 page boundary.

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

- `PhotobookStage`: owns fixed page aspect ratio and centering.
- `ContentPageRenderer`: renders slots and images from `TemplateLayoutEngine`.
- `CoverPageRenderer`: renders cover/back-cover background and text elements.
- `FloatingPhotobookToolbar`: displays icon-only actions.

Preview and PDF must consume the same page data:

- Content pages: `TemplateLayoutEngine` slot rectangles.
- Cover/back cover: `CoverLayout` text coordinates.

## Editor Selection Model

Replace slot-only selection with a typed selection.

Recommended:

```kotlin
sealed interface EditorSelection {
    data class ImageSlot(val pageIndex: Int, val slotId: String) : EditorSelection
    data class CoverText(val pageType: CoverPageType, val textId: String) : EditorSelection
    data object None : EditorSelection
}
```

Do not rely only on `selectedSlotId`; it is insufficient for slot filling and cross-page actions.

## Preview Requirements

- Preview first leaf is cover.
- Final leaf is back cover.
- Cover/back cover use same 285x210 stage size as content pages.
- Portrait orientation shows one page.
- Landscape orientation shows two-page spread.
- Content slots and cover text positions must match editor.

## PDF Requirements

- PDF page size remains 285x210.
- First page is cover.
- Content pages follow.
- Last page is back cover.
- Cover/back-cover text positions must match editor.
- Content page image slots must match editor and preview.
- PDF export must continue to be crash-safe and memory-safe after recent fixes.

## Tests Required

Add or update unit tests for:

- `PageTemplate` contains five templates and stable slot ids.
- Auto import chooses `SingleLandscape` for landscape images.
- Auto import chooses `SinglePortrait` for portrait images.
- `TemplateLayoutEngine` centers `SingleLandscape`.
- `TemplateLayoutEngine` centers `SinglePortrait`.
- `TemplateLayoutEngine` keeps every slot inside safe area.
- Empty slot fill updates the exact `(pageIndex, slotId)`.
- Add-page inserts a `SingleLandscape` empty page.
- Cover layout defaults have expected text ids and coordinates inside page bounds.
- Cover text move clamps coordinates inside page bounds.

Manual QA:

- Create a photobook from mixed landscape and portrait photos.
- Confirm portrait photo pages use portrait slot.
- Tap layout toolbar and switch between all five templates.
- Add a new page from the toolbar and confirm it is landscape single empty page.
- Tap empty slot `+`, choose a photo, confirm that slot fills.
- Confirm no bottom add-photo button appears.
- Switch between cover, content page, and back cover; stage position must not jump.
- Move cover title, save, reopen, confirm position remains.
- Export PDF and confirm page sizes/positions match editor.

## Implementation Phases

1. Update model and serialization.
2. Update `TemplateLayoutEngine` and tests.
3. Add auto import aspect-ratio template selection.
4. Build fixed `PhotobookStage`.
5. Replace editor toolbar with floating icon-only toolbar.
6. Implement add-page action.
7. Fix empty slot `+` fill flow using `PendingSlotFill`.
8. Add cover/back-cover layout model and renderer.
9. Add cover/back-cover text drag/save.
10. Update preview and PDF to consume shared models.
11. Run unit tests, build APK, and perform manual QA.

## Open Implementation Notes For Claude

- Inspect latest commit before coding. Current recent commit is expected to include PDF crash/OOM fixes.
- Do not revert the recent PDF crash fixes.
- Stage only intended source/test/schema/doc files.
- Do not stage `.DS_Store`, build outputs, `.gradle`, `.idea`, or reference folders.
- Use focused commits after stable phases.

## Spec Self-Review

- Completeness scan: all sections contain concrete behavior, data fields, and validation expectations.
- Internal consistency: page size is consistently 285x210 landscape for cover, content pages, and back cover.
- Scope check: this is one bounded photobook editor redesign; it should be implemented as one phased plan.
- Ambiguity check: add-page, empty-slot fill, template selection, cover text movement, preview, and PDF behavior are explicitly defined.
