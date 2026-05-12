# Photobook 285x210 Redesign Spec

**Date:** 2026-05-13
**Status:** Design approved

---

## Overview

Redesign the photobook module with three subsystems:
- **A: Photobook List** — paper-textured 285x210 covers, selection mode, cover management
- **B: Photobook Editor** — single page 285x210 canvas, gesture-based image editing, flip animation, PDF export
- **C: Photobook Preview** — portrait single-page / landscape spread view, page flip, spine effect

All design follows `DESIGN.md` specifications: Eastern Minimalism, warm paper palette, Noto Sans SC + Source Sans 3 typography, 28dp corner radius, low-contrast outlines, Ma (whitespace) emphasis.

---

## Design System Reference

Per `stitch_yingjian_huacev2/` design files and `docs/superpowers/specs/2026-05-12-ui-adjustments-phase1-design.md`:

- **Colors**: background `#fbf9f6`, primary `#665a4a`, paper texture `#a49a8e`, on-surface `#1b1c1a`
- **Typography**: Noto Sans SC (headings), Source Sans 3 (body), Noto Serif SC (printer-style text)
- **Corners**: 28dp primary, 16dp cards, 12dp inner frames
- **Shadows**: low-contrast outlines + light shadows, no heavy drop shadows
- **Spacing**: 8dp baseline, 16dp gutter, 24dp margins

---

## Subsystem A: Photobook List Page

### Files
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookListScreen.kt`
- Modify: `app/src/main/java/com/yingjian/core/data/database/YingJianDatabase.kt` (add `coverImageUri` column)
- Modify: `app/src/main/java/com/yingjian/core/data/database/PhotobookEntity` (if in separate file)

### TopAppBar

```
┌──────────────────────────────┐
│  画册                   🔍   │
└──────────────────────────────┘
```

- Title "画册" left-aligned, matching MemoriesScreen style
- No menu icon on left
- Search button on right (MVP: placeholder, non-functional)

### Section Header

- "所有画册" headline
- "共 N 册，记录 M 个瞬间" subtitle
- Ink divider below (1px, outline color at 40% opacity, 80% width centered)

### Grid Layout

- 2-column `LazyVerticalGrid`, `GridCells.Fixed(2)`
- Gap: 16dp horizontal, 32dp vertical
- Side margins: 24dp (margin-mobile)
- Content padding: `PaddingValues(24.dp)`

### Photobook Cover Card

```
┌─────────────────┐
│ ▎               │  ← spine-line (1px vertical, left 8dp)
│ ▎  ┌─────────┐  │
│ ▎  │         │  │  ← inner cover image area
│ ▎  │  image  │  │     padding: 12dp (left extra 8dp)
│ ▎  │         │  │     aspect: 2:1 landscape
│ ▎  └─────────┘  │
│ ▎               │
└─────────────────┘  ← aspect-ratio: 285/210
                       background: #a49a8e (paper texture)
                       corner: rounded-sm (4dp equivalent)
                       shadow: book-shadow (0 4px 16px rgba(0,0,0,0.1))
─────────────────
画册名称             ← 16sp, Noto Sans SC, light, tracking-widest
42页                ← 10sp, label-lg, opacity 0.5
```

- Paper texture background: solid color `#a49a8e` (warm taupe)
- Spine line: 1px vertical line at 8dp from left edge, `rgba(0,0,0,0.15)` with subtle highlight
- Cover image: inner box padding 12dp, aspect 2:1, `object-fit: cover`
- Book shadow: subtle shadow with large blur
- Title below: centered relative to card width, font-light, tracking-widest
- Page count below title: smaller, lower opacity
- Hover/tap: subtle lift (`-translate-y-1` equivalent = slight upward offset)

### "New Photobook" Card

Same 285x210 proportions, but:
- Paper texture background, no cover image
- Centered circle (40dp) with border outline + "add" icon inside
- "新建画册" text below (same styling as other titles)

### Selection Mode (Long Press)

Entry: long press on any photobook card → enter selection mode.

```
┌──────────────────────────────┐
│  ✕ 取消    已选择 N 册  🗑 删除│  ← TopAppBar changes
├──────────────────────────────┤
│  ┌──────┐  ┌──────┐         │
│  │ ☑️   │  │  ☑️  │         │  ← Selected: checkmark + semi-transparent primary overlay
│  │cover │  │cover │         │
│  └──────┘  └──────┘         │
│                              │
│  ┌──────┐  ┌──────┐         │
│  │  ☑️  │  │      │         │  ← Unselected: no overlay
│  │cover │  │cover │         │
│  └──────┘  └──────┘         │
└──────────────────────────────┘
```

- TopAppBar changes: ✕ cancel button (left) + count text + 🗑 delete button (right)
- Selected cards: checkmark icon (top-left), semi-transparent primary color overlay
- Tap unselected card → select it
- Tap selected card → deselect it
- Tap "新建画册" card in selection mode → ignored
- Tap delete → confirmation dialog → batch delete → exit selection mode
- Tap ✕ or back button → exit selection mode
- PhotobookViewModel needs `deletePhotobooks(ids: List<Long>)` batch method

### Cover Image Data

- `PhotobookEntity` adds `coverImageUri: String?` — null = default paper texture
- Cover set in editor page (Subsystem B)
- Cover image loaded via Coil with `Uri.parse(coverImageUri)`

### Database Changes

```kotlin
data class PhotobookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val paperSize: String,
    val coverImageUri: String? = null,  // NEW
    val createdAt: Long,
    val updatedAt: Long
)
```

Room auto-migration: add `coverImageUri` column (nullable TEXT, default null).

### Interaction Flow

```
Photobook List
  ├─ Tap "新建画册" → BottomSheet(name + size) → PhotoPicker → create
  ├─ Tap photobook cover → navigate to Editor (Subsystem B)
  ├─ Long press cover → enter selection mode
  │    ├─ Tap 🗑 → confirmation dialog → batch delete → exit
  │    └─ Tap ✕ → exit selection mode
  └─ Search button → placeholder (non-functional MVP)
```

---

## Subsystem B: Photobook Editor Page

### Files
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookEditorScreen.kt` (full rewrite)
- Modify: `app/src/main/java/com/yingjian/feature/photobook/model/PageState.kt` (add `memoryId`)
- Modify: `app/src/main/java/com/yingjian/feature/photobook/model/BookState.kt` (no changes)
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookCanvasPage.kt` (full rewrite)
- Modify: `app/src/main/java/com/yingjian/feature/photobook/export/PdfExportUtil.kt`
- Modify: `app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt` (add preview route, update editor route)

### TopAppBar

```
┌──────────────────────────────┐
│  ← 编辑    👁️预览 📤导出 💾保存│
└──────────────────────────────┘
```

- Left: back arrow + "编辑" text
- Right: three icon buttons (no text labels)
  - 👁️ Preview — navigate to preview page
  - 📤 Export — generate and share PDF
  - 💾 Save — persist to database

### Single Page Canvas

```
     ┌──────────────────────┐
     │                      │
     │    (whitespace)      │
     │                      │
     │   ┌──────────────┐   │  ← Image centered, aspect 3:4
     │   │              │   │
     │   │    photo     │   │
     │   │              │   │
     │   └──────────────┘   │
     │                      │
     │   心情描述文字         │  ← Noto Serif SC, 11sp, extralight, letter-spacing 0.2em
     │   2024.11.15         │  ← Date below mood text
     │                      │
     │                 03   │  ← Page number (bottom-right)
     └──────────────────────┘
```

- Aspect ratio: `285/210` (landscape)
- Width: fills screen, max 600dp
- Paper texture: `#faf9f6` background with subtle SVG noise overlay (via Canvas)
- Shadow: `0 4px 12px rgba(0,0,0,0.05)`
- Image: centered, default aspect 3:4, white margins ~30-40dp on all sides
- Mood text + date: below image, centered, Noto Serif SC (printer-style), small size
- Page number: bottom-right corner, small, outline-variant color
- Each page contains exactly one photo

### Gesture-Based Image Editing (Hybrid Mode)

**Default state:**
- Single-finger drag: move image position
- Two-finger pinch: scale image
- Double-tap: enter selected state

**Selected state:**
- Selection border: primary color, 1px, around image
- Four corner handles: 8x8dp white squares
  - Top-left: cursor-nwse-resize
  - Top-right: cursor-nesw-resize
  - Bottom-left: cursor-nesw-resize
  - Bottom-right: cursor-nwse-resize
- Drag corner handle: resize image (maintain aspect ratio)
- Bottom toolbar appears: [🗑 Delete] [↺ Reset]

### Page Navigation

```
   ◀  ────●────  ▶
```

- HorizontalPager for page switching
- Page indicator: dots, current page dot is elongated (24dp wide vs 8dp)
- Left/right arrow buttons on sides of indicator
- Additional tap zones: tap left half of screen → previous page, tap right half → next page
- Slide gesture: main navigation method
- Transition: slide animation (MVP), curl animation (future)

### Add Photo to Book

Tap [+] button (visible at bottom when no image selected):
- Opens PhotoPicker showing memories from 影记 (same as create flow)
- Selected photos → each photo becomes a new page appended to end
- Auto-navigate to the first new page

### Bottom Toolbar (Contextual)

| State | Toolbar Content |
|-------|----------------|
| No page / empty | None |
| Has image, not selected | [+] Add Photo button (bottom center) |
| Image selected | [🗑 Delete] [↺ Reset] (bottom center) |

### Cover Photo Setting

- Accessible via long-press on the first page (cover page) or a settings action
- Opens a bottom sheet: "设为封面" option → saves current page's image URI to `PhotobookEntity.coverImageUri`
- Or: in TopAppBar add a cover icon button when on the first page

### PDF Export

- Icon button in TopAppBar: 📤 Export
- Uses `PdfExportUtil` to render all pages at 285x210mm
- Pages rendered with paper texture background, images at their positioned coordinates, mood text + dates
- Output: PDF file, shared via Android share sheet
- Cover page (if set) rendered as first page

### Save

- Persists all `PageState` → `PageLayoutEntity` records
- Updates `PhotobookEntity.updatedAt` and `coverImageUri`
- Runs on IO dispatcher, shows brief success feedback

### Data Model

```kotlin
// PageState adds memoryId for mood/date display
data class PageState(
    val pageNumber: Int,
    val elements: List<PageElement>,
    val trimWidthMm: Float,
    val trimHeightMm: Float,
    val memoryId: Long? = null,  // NEW: linked memory for mood text + date
    val bleedMm: Float = 3.0f
)
```

### Interaction Flow

```
Editor
  ├─ Slide left/right → flip pages
  ├─ Tap left/right half → flip pages
  ├─ ◀/▶ arrows → flip pages
  ├─ Single-finger drag on image → move
  ├─ Two-finger pinch → scale
  ├─ Double-tap image → select (show handles + bottom toolbar)
  │    ├─ Drag corner handle → resize
  │    ├─ 🗑 Delete → remove image from page
  │    └─ ↺ Reset → image back to default position
  ├─ [+] button → PhotoPicker → append new pages
  ├─ 👁️ Preview → navigate to Preview
  ├─ 📤 Export → generate PDF → share sheet
  └─ 💾 Save → persist all pages → back to list
```

---

## Subsystem C: Photobook Preview

### Files
- Create: `app/src/main/java/com/yingjian/feature/photobook/PhotobookPreviewScreen.kt`
- Modify: `app/src/main/java/com/yingjian/core/ui/navigation/NavDestinations.kt` (add Preview route)
- Modify: `app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt` (add Preview composable)

### TopBar

```
┌──────────────────────────────┐
│  ← 返回               📤分享 │
└──────────────────────────────┘
```

- Semi-transparent, floating over content
- No title text
- Back arrow + share/export icon
- Blur backdrop effect

### Portrait Mode (Single Page)

```
┌──────────────────────────────┐
│                              │
│   ┌──────────────────┐      │
│   │                  │      │
│   │     Page N       │      │  ← Single page 285x210
│   │                  │      │     Fills screen width
│   └──────────────────┘      │
│                              │
│        ◀  ●●●●●  ▶          │
│                              │
│  [tap left zone] [tap right] │
└──────────────────────────────┘
```

- Single page fills screen width
- Page rendered with paper texture, image, mood text, page number (same as editor)
- Tap left/right zones + slide to flip pages

### Landscape Mode (Spread View)

```
┌─────────────────────────────────────────────┐
│                                             │
│  ┌──────────┐  ┃  ┌──────────┐             │
│  │          │  ┃  │          │             │
│  │  Page N  │  ┃  │ Page N+1  │             │
│  │          │  ┃  │          │             │
│  └──────────┘  ┃  └──────────┘             │
│           spine crease                      │
│                                             │
│              ◀  ●●●  ▶                     │
└─────────────────────────────────────────────┘
```

- Spread width: `90vw`, max 1200dp, aspect `2.7:1` (two 285x210 pages side by side)
- Spine effect at center:
  - 40dp gradient shadow from transparent → `rgba(0,0,0,0.15)` → transparent
  - 1px inner highlight white line
  - 2px subtle center crease line
- Each half-page: paper texture background, rendered with image and text from editor
- Flipping a spread advances +2 pages or reverses -2 pages

### Page Flip Interaction

| Action | Portrait | Landscape |
|--------|----------|-----------|
| Swipe left | Next page (+1) | Next spread (+2 pages) |
| Swipe right | Previous page (-1) | Previous spread (-2 pages) |
| Tap left half | Previous page | Previous spread |
| Tap right half | Next page | Next spread |
| ◀ / ▶ arrows | Previous / next | Previous / next spread |

### Page Indicator

- Bottom centered
- Dots with current page elongated
- Arrow buttons on sides

### Transition Animation

- Page change: horizontal slide with slight scale (current: 1.0, outgoing: 0.95)
- Orientation change: animate between single-page and spread layouts

### Orientation Handling

- Listen to device orientation via `LocalConfiguration.current.orientation`
- Portrait: `GridCells.Fixed(1)` effectively, single page view
- Landscape: two pages side-by-side in spread view
- Transition animated when rotation detected

### Interaction Flow

```
Preview
  ├─ Swipe left / tap right → next page (portrait) or spread (landscape)
  ├─ Swipe right / tap left → previous
  ├─ ◀/▶ buttons → flip pages
  ├─ ← Back → return to editor
  └─ 📤 Share → share PDF via system share sheet
```

---

## Implementation Order

1. **Subsystem A** — List page redesign + DB migration
2. **Subsystem B** — Editor full rewrite + PDF export
3. **Subsystem C** — Preview page (depends on B's page rendering)

Each subsystem is independently testable after completion.

---

## PaperSize Addition

New PaperSize enum value for 方12寸横版:

```kotlin
enum class PaperSize(val widthMm: Float, val heightMm: Float) {
    // ... existing values
    TWELVE_INCH_LANDSCAPE(285f, 210f),  // NEW: 方12寸横版
    // ... existing values continue
}
```

This is the default paper size for all photobooks going forward. The editor canvas renders at this size.

---

## Database Migration Summary

| Version | Change |
|---------|--------|
| Current | (check existing version) |
| +1 | Add `coverImageUri TEXT` to `photobook` table (nullable, default null) |
