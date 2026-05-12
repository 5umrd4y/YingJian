# Photobook 285x210 Redesign Spec

**Date:** 2026-05-13  
**Status:** Design approved (v2, post-review)  
**Review:** `Photobook-285x210-重新设计审阅结果.md`

---

## Overview

Redesign the photobook module with three subsystems:
- **A: Photobook List** — paper-textured 285x210 covers, selection mode, cover management
- **B: Photobook Editor** — single page 285x210 canvas, gesture-based image editing, button-based page switching, PDF export
- **C: Photobook Preview** — portrait single-page / landscape spread view, page flip, spine effect

All design follows `DESIGN.md` specifications: Eastern Minimalism, warm paper palette, Noto Sans SC + Source Sans 3 typography, 28dp corner radius, low-contrast outlines, Ma (whitespace) emphasis.

---

## Design System Reference

Per `stitch_yingjian_huacev2/` design files:

- **Colors**: background `#fbf9f6`, primary `#665a4a`, paper texture `#a49a8e` (warm taupe, defined outside MD3 scheme as `paperTexture`), on-surface `#1b1c1a`
- **Typography**: Noto Sans SC (headings), Source Sans 3 (body), Noto Serif SC (printer-style text, to be added to `res/font/`)
- **Corners**: cards `RoundedCornerShape(4.dp)` (rounded-sm per HTML), inner frames `RoundedCornerShape(12.dp)`
- **Shadows**: `Modifier.shadow(elevation = 4.dp, shape = ...)` approximate; low-contrast outlines + light shadows
- **Spacing**: 8dp baseline, 16dp gutter, 24dp margins

### Noto Serif SC Font

Download and place in `app/src/main/res/font/noto_serif_sc_extralight.ttf` and `app/src/main/assets/fonts/noto_serif_sc_extralight.ttf`. Define in Typography for printer-style text (mood + date on canvas pages).

---

## Subsystem A: Photobook List Page

### Files
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookListScreen.kt`
- Modify: `app/src/main/java/com/yingjian/core/data/database/YingJianDatabase.kt`
- Modify: `app/src/main/java/com/yingjian/core/data/repository/PhotobookRepository.kt`

### TopAppBar

```
┌──────────────────────────────┐
│  画册                   🔍   │
└──────────────────────────────┘
```

- Title "画册" left-aligned, matching MemoriesScreen style
- No menu icon on left (per user decision)
- Search button on right (MVP: placeholder, non-functional)

### Section Header

- "所有画册" headline (Noto Sans SC, title-lg)
- "共 N 册" subtitle (Source Sans 3, body-md, on-surface-variant)
- Ink divider: 1px horizontal, outline color at 40% opacity, 80% width centered

### Grid Layout

- 2-column `LazyVerticalGrid`, `GridCells.Fixed(2)`
- Horizontal gap: 16dp, vertical gap: 32dp
- Content padding: `PaddingValues(24.dp)` (margin-mobile)

### Photobook Cover Card

```
┌─────────────────┐
│ ▎               │  ← spine-line (1px left edge, `Modifier.drawBehind`)
│ ▎  ┌─────────┐  │
│ ▎  │         │  │  ← inner cover image area
│ ▎  │  image  │  │     padding: 12dp
│ ▎  │         │  │     aspect: 2:1 landscape
│ ▎  └─────────┘  │
│ ▎               │
└─────────────────┘  ← `Modifier.aspectRatio(285f / 210f)`
                       `Modifier.background(Color(0xFFA49A8E))`
                       `RoundedCornerShape(4.dp)`
                       `Modifier.shadow(elevation = 4.dp)`
画册名称          42页
↑ left-aligned    ↑ right-aligned

```

- **Paper texture**: solid color `#A49A8E` (warm taupe). Defined as `val PaperTexture = Color(0xFFA49A8E)` in theme.
- **Spine line**: 1px vertical line at 8dp from left edge, drawn via `Modifier.drawBehind { drawLine(...) }` with `Color.Black.copy(alpha = 0.15f)`. Subtle highlight with `Color.White.copy(alpha = 0.05f)`.
- **Cover image area**: inner Box with `Modifier.padding(12.dp)`, `AspectRatio(2f)`, `ContentScale.Crop`
- **Title row**: `Row(horizontalArrangement = Arrangement.SpaceBetween)` — title left, page count right
- **Title**: `Noto Sans SC`, 16sp, `FontWeight.Light`, `letterSpacing = 0.1.em`
- **Page count**: `Source Sans 3`, 10sp, `onSurface.copy(alpha = 0.5f)`
- **Tap feedback**: slight scale-down via `Modifier.animateContentSize()` or `graphicsLayer { scaleX/Y }` on press

### CreateNewAlbumCard

Same 285x210 proportions:
- Paper texture background (`#A49A8E`)
- Centered circle: 40dp, `Modifier.border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape)` — **outline style, no solid fill** (per HTML reference)
- "add" icon inside: `Icons.Default.Add`, tint `Color.Black.copy(alpha = 0.6f)`, size 20dp
- "新建画册" text below, same styling as other titles

### Selection Mode

Entry: long press on any photobook cover → enter selection mode.

State management in `PhotobookUiState`:
```kotlin
data class PhotobookUiState(
    val photobooks: List<PhotobookEntity> = emptyList(),
    val isLoading: Boolean = false,
    val isSelectionMode: Boolean = false,   // NEW
    val selectedIds: Set<Long> = emptySet()  // NEW
)
```

TopAppBar in selection mode:
```
┌──────────────────────────────┐
│  ✕ 取消    已选择 N 册  🗑  │
└──────────────────────────────┘
```

- **✕ Cancel**: exits selection mode, clears selectedIds
- **Count text**: dynamically updates
- **🗑 Delete**: enabled only when `selectedIds.isNotEmpty()`, shows confirmation dialog

Card state in selection mode:
- Selected: `Modifier.background(primary.copy(alpha = 0.15f))` overlay + checkmark icon top-left
- Unselected: normal appearance
- Tap toggles selection state
- "新建画册" card: tap ignored in selection mode

Batch delete flow:
1. Tap delete → AlertDialog: "确定删除 N 本画册？此操作不可撤销"
2. Confirm → `viewModel.deletePhotobooks(ids)` → exits selection mode
3. DAO method: `@Query("DELETE FROM photobook WHERE id IN (:ids)") suspend fun deleteByIds(ids: List<Long>)`

### Cover Image Data

- `PhotobookEntity` adds `coverImageUri: String? = null` — null means default paper texture
- Cover set in editor page via TopAppBar cover button (see Subsystem B)
- Cover image loaded via Coil: `model = coverImageUri?.let { Uri.parse(it) }`
- `coverImageUri` stores content URI string from the image picker

### Database Migration

**Current version: 3 → New version: 4**

```kotlin
@Database(
    version = 4,
    entities = [MemoryRecordEntity::class, PhotobookEntity::class, PageLayoutEntity::class],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4)  // adds coverImageUri column
    ]
)
abstract class YingJianDatabase : RoomDatabase() { ... }
```

`PhotobookEntity` changes:
```kotlin
data class PhotobookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val paperSize: String,
    val coverImageUri: String? = null,  // NEW: v3→v4, nullable TEXT
    val createdAt: Long,
    val updatedAt: Long
)
```

### Interaction Flow

```
Photobook List
  ├─ Tap "新建画册" → BottomSheet(name only, size=285x210 implicitly) → PhotoPicker → create
  ├─ Tap photobook cover → navigate to Editor (Subsystem B)
  ├─ Long press cover → enter selection mode
  │    ├─ Tap 🗑 → confirmation dialog → batch delete → exit
  │    └─ Tap ✕ → exit selection mode
  └─ Search button → placeholder (non-functional MVP)
```

---

## Subsystem B: Photobook Editor Page

### Files
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookEditorScreen.kt` (incremental changes)
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookCanvasPage.kt` (incremental changes)
- Modify: `app/src/main/java/com/yingjian/feature/photobook/model/PageElement.kt` (no changes — `ImageElement.memoryId` already exists)
- Modify: `app/src/main/java/com/yingjian/feature/photobook/export/PdfExportUtil.kt`
- Modify: `app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt` (add preview route, update editor route)
- Modify: `app/src/main/java/com/yingjian/core/ui/navigation/NavDestinations.kt` (add Preview route)

### PaperSize Simplification

Per user decision: **only keep 285x210 (方12寸横版)**. No paper size selection in UI.

```kotlin
enum class PaperSize(val widthMm: Float, val heightMm: Float) {
    TWELVE_INCH_LANDSCAPE(285f, 210f)  // 方12寸横版 — the only size
}
```

`CreatePhotobookBottomSheet` simplified: no paper size selector, only name input. Paper size is always 285x210.

**Note:** Existing photobooks with other paper sizes (A4, SIX_INCH, SQUARE) will still display correctly — their `paperSize` string is preserved. For new photobooks, only 285x210 is created.

### TopAppBar

Per review P3-4, use standard Material 3 pattern:
```
┌──────────────────────────────┐
│  ←  画册名称    👁️ 📤 💾     │
└──────────────────────────────┘
```

- Left: back arrow (no text label, standard M3 navigationIcon)
- Center: photobook name as title
- Right: three icon buttons (no text):
  - `Icons.Default.Visibility` (👁️ Preview)
  - `Icons.Default.PictureAsPdf` (📤 Export PDF)
  - `Icons.Default.Save` (💾 Save)

### Single Page Canvas

```
     ┌──────────────────────┐
     │                      │
     │    (whitespace)      │
     │                      │
     │   ┌──────────────┐   │  ← Image centered
     │   │              │   │     ContentScale.Fit
     │   │    photo     │   │     aspect ratio from metadata
     │   │              │   │
     │   └──────────────┘   │
     │                      │
     │   心情描述文字         │  ← Noto Serif SC, 11sp, FontWeight.ExtraLight
     │   2024.11.15         │     letterSpacing = 0.2.em
     │                      │
     │                 03   │  ← Page number, outline-variant, 11sp
     └──────────────────────┘
```

- Aspect ratio: `285/210` (landscape), width fills screen max 600dp
- Paper texture: `#FAF9F6` background
- Image: centered, default margins ~40dp on all sides
- Mood text + date: below image, centered, Noto Serif SC ExtraLight
- Page number: bottom-right corner
- Mood text and date derived from `ImageElement.memoryId` — look up `MemoryRecordEntity` by id to get `moodText` and `timestamp`
- `PageState` does NOT need a new `memoryId` field — derive from `elements.firstOrNull { it is ImageElement }?.memoryId`

### Page Navigation (Button-Based)

Per user decision: **remove HorizontalPager**, use button-based navigation with custom slide animation. This eliminates gesture conflicts — all touch gestures go to image editing.

```
   ◀                          ▶
          ● ○ ○ ○ ○
        (page indicator)
```

- **◀ ▶ buttons**: flanking the page indicator, navigate previous/next page
- **Page indicator**: row of dots, current page elongated (16dp wide vs 6dp)
- **No swipe-based page flipping** — single-finger drag reserved for moving images
- **Transition**: `AnimatedContent` with `slideInHorizontally` / `slideOutHorizontally` for page change animation

Implementation approach:
```kotlin
var currentPage by remember { mutableIntStateOf(0) }
AnimatedContent(
    targetState = currentPage,
    transitionSpec = {
        if (targetState > initialState) {
            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
        } else {
            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
        }
    }
) { pageIndex ->
    PhotobookCanvasPage(pageState = bookState.pages[pageIndex], ...)
}
```

### Gesture-Based Image Editing

All touch events go to image manipulation (no HorizontalPager conflict):

**Default state:**
- Single-finger drag on image: move position (update `ImageElement.xMm/yMm`)
- Two-finger pinch: scale (update `ImageElement.widthMm/heightMm`)
- Double-tap on image: enter selected state

**Selected state:**
- Selection border: `primary` color, 1px, `Modifier.border(1.dp, primary, RoundedCornerShape(2.dp))`
- Four corner handles: 8x8dp white squares with `Modifier.border(1.dp, primary, ...)`
- Drag corner handle: resize image (maintain aspect ratio)
- Bottom toolbar appears: [🗑 Delete] [↺ Reset]
- Tap outside image → exit selected state

Gesture implementation note: use `Modifier.pointerInput` with custom `detectTransformGestures` for pan/zoom, `detectTapGestures(onDoubleTap = ...)` for selection.

### Bottom Toolbar

| State | Content |
|-------|---------|
| No pages / empty | None |
| Has image, not selected | `[+]` Add Photo (bottom center, `FloatingActionButton`-style) |
| Image selected | `Row`: [🗑 Delete] [↺ Reset] (bottom center) |

Delete action: removes image from current page. If page becomes empty, remove the page entirely.

### Add Photo to Book (Append Mode)

Tap `[+]` button:
1. Opens PhotoPicker showing memories from 影记
2. `onComplete` returns selected memory IDs
3. For each selected memory:
   - Fetch `MemoryRecordEntity` from repository
   - Call `AutoLayoutAlgorithm.createSinglePhotoPage(memory, paperSize)` to generate a new `PageState` with the image centered
   - Append to `bookState.pages`
4. Auto-navigate to the first new page

### Cover Photo Setting

- TopAppBar: add cover icon button (`Icons.Default.Style`) visible on the first page (or always)
- Tap → BottomSheet: "设为封面" → saves current page's `ImageElement.imageUri` to `PhotobookEntity.coverImageUri`
- Avoids gesture conflict with long-press (per review P2-3)

### PDF Export

- Icon button in TopAppBar: `Icons.Default.PictureAsPdf`
- Uses `PdfExportUtil` to render all pages at 285x210mm
- Pages rendered with paper texture background, images at their positioned coordinates, mood text + dates
- Output: PDF file, shared via Android share sheet using `ActivityResultContracts.CreateDocument`

### Save

- Persists all `PageState` → `PageLayoutEntity` records
- Updates `PhotobookEntity.updatedAt` and `coverImageUri`
- Runs on IO dispatcher, shows brief Snackbar feedback

### Interaction Flow

```
Editor
  ├─ ◀/▶ buttons → flip pages (animated slide)
  ├─ Single-finger drag on image → move
  ├─ Two-finger pinch → scale
  ├─ Double-tap image → select (show handles + bottom toolbar)
  │    ├─ Drag corner handle → resize
  │    ├─ 🗑 Delete → remove image from page
  │    └─ ↺ Reset → image back to default position
  ├─ [+] button → PhotoPicker → append new pages
  ├─ Cover icon → BottomSheet "设为封面"
  ├─ 👁️ Preview → navigate to Preview
  ├─ 📤 Export → generate PDF → share sheet
  └─ 💾 Save → persist all pages → back to list
```

---

## Subsystem C: Photobook Preview

### Files
- Create: `app/src/main/java/com/yingjian/feature/photobook/PhotobookPreviewScreen.kt`
- Modify: `app/src/main/java/com/yingjian/core/ui/navigation/NavDestinations.kt` (add `Preview` route with `photobookId` param)
- Modify: `app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt` (add Preview composable)

### Preview Route

```kotlin
// NavDestinations.kt
data object Preview : NavDestinations("photobook_preview/{photobookId}") {
    fun createRoute(photobookId: Long) = "photobook_preview/$photobookId"
}

// YingJianNavHost.kt
composable(NavDestinations.Preview.route) { backStackEntry ->
    val photobookId = backStackEntry.arguments?.getString("photobookId")?.toLongOrNull()
    // Load book state from repository, render PhotobookPreviewScreen
}
```

### TopBar

```
┌──────────────────────────────┐
│  ←                      📤   │
└──────────────────────────────┘
```

- Semi-transparent, floating over content (`Modifier.background(Color.Transparent)`)
- No title text
- Back arrow (left) + share icon (right)
- No blur backdrop in MVP (simplify implementation)

### Portrait Mode (Single Page)

Vertical phone orientation — displays one 285x210 page filling screen width:

- Page rendered identically to editor canvas (paper texture, image, mood text, date, page number)
- Swipe left → next page (+1)
- Swipe right → previous page (-1)
- Tap left half → previous page
- Tap right half → next page
- ◀/▶ arrow buttons + page indicator at bottom

### Landscape Mode (Spread View)

Horizontal phone orientation — displays two pages side by side:

```
┌─────────────────────────────────────────────┐
│  ┌──────────┐  ┃  ┌──────────┐             │
│  │  Page N  │  ┃  │ Page N+1  │             │
│  │  (left)  │  ┃  │  (right) │             │
│  └──────────┘  ┃  └──────────┘             │
│           spine crease                      │
└─────────────────────────────────────────────┘
```

- Spread width: 90% of screen width, aspect ~2.7:1
- Each half: one 285x210 page
- Spine crease center:
  - `Canvas`-drawn gradient shadow: transparent → `Color.Black.copy(alpha = 0.15f)` → transparent, width ~40dp
  - 1px white line at center for inner fold highlight: `Color.White.copy(alpha = 0.3f)`
  - This is drawn as a single composable overlay on the spread container

Spine implementation (simplified):
```kotlin
Canvas(modifier = Modifier.fillMaxSize()) {
    // Center gradient shadow
    val centerX = size.width / 2
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.15f), Color.Transparent),
            startX = centerX - 20.dp.toPx(),
            endX = centerX + 20.dp.toPx()
        ),
        topLeft = Offset(centerX - 20.dp.toPx(), 0f),
        size = Size(40.dp.toPx(), size.height)
    )
    // Inner highlight
    drawLine(Color.White.copy(alpha = 0.3f), Offset(centerX, 0f), Offset(centerX, size.height), 1.dp.toPx())
}
```

### Page Flip Interaction

| Action | Portrait | Landscape |
|--------|----------|-----------|
| Swipe left | Next page (+1) | Next spread (+2 pages) |
| Swipe right | Previous page (-1) | Previous spread (-2 pages) |
| Tap left half | Previous page | Previous spread |
| Tap right half | Next page | Next spread |
| ◀ / ▶ arrows | Previous / next | Previous / next spread |

Use `HorizontalPager` for swipe-based page navigation in preview mode.

### Page Indicator

- Bottom centered, above navigation buttons
- Dots: current page elongated (16dp × 6dp), others (6dp × 6dp)
- ◀ ▶ arrow buttons flanking indicator

### Orientation Handling

- `LocalConfiguration.current.orientation` to detect portrait/landscape
- Portrait: single page view
- Landscape: spread (two-page) view
- `AnimatedContent` or `Crossfade` for smooth transition on orientation change

### Interaction Flow

```
Preview
  ├─ Swipe left / tap right → next page/spread
  ├─ Swipe right / tap left → previous
  ├─ ◀/▶ buttons → flip pages
  ├─ ← Back → return to editor
  └─ 📤 Share → share PDF via system share sheet
```

---

## Implementation Order

1. **Phase 1: Database + PaperSize** — v3→v4 migration, add `TWELVE_INCH_LANDSCAPE`, simplify PaperSize, Noto Serif SC font
2. **Phase 2: Subsystem A** — PhotobookListScreen incremental redesign (285x210 cards, selection mode, batch delete)
3. **Phase 3: Subsystem B** — Editor incremental changes (285x210 canvas, button nav, gesture editing, [+] append, PDF export)
4. **Phase 4: Subsystem C** — Preview page (single/spread views, spine effect, orientation handling)

Each phase is independently testable.

---

## Database Migration Summary

| Version | Change |
|---------|--------|
| 3 (current) | Existing schema with `memory_record`, `photobook`, `page_layout` tables |
| 4 (new) | Add `coverImageUri TEXT` to `photobook` table (nullable, default null). `AutoMigration(3, 4)`. |

Full `@Database` configuration:
```kotlin
@Database(
    version = 4,
    entities = [MemoryRecordEntity::class, PhotobookEntity::class, PageLayoutEntity::class],
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4)
    ]
)
abstract class YingJianDatabase : RoomDatabase() { ... }
```
