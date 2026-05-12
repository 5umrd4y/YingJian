# Phase 2: Multi-Photo Upload & Memory Detail View

## Overview

Two interconnected features extending the Memories flow: multi-photo support in NewPostScreen (up to 9 images) and a new MemoryDetailScreen for full-screen image browsing with swipe navigation and edit capability. Both Timeline and Calendar views become clickable entry points to the detail view.

---

## Data Model Change

### Add `imageUrisJson` to `MemoryRecordEntity`

Add a new column `@ColumnInfo(name = "image_uris_json") val imageUrisJson: String` to store a JSON array of image URIs using kotlinx.serialization.

- **Format**: JSON array of URI strings, e.g. `["content://...", "content://..."]`
- **Backward compatibility**: Existing records have a single `imageUri`. When reading, if `imageUrisJson` is empty, use `[imageUri]` as the image list.
- **Room migration**: AutoMigration v2→v3. New column needs a default value. Use `defaultValue = "[]"`.

### Deprecate `photoCount`

The existing `photoCount` column becomes derived data (computed from `imageUrisJson` length) rather than a stored field. Keep the column for now but stop writing to it.

### AutoMigration v2→v3

```kotlin
@Database(
    entities = [MemoryRecordEntity::class, PhotobookEntity::class, PageLayoutEntity::class],
    version = 3,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3)
    ],
    exportSchema = true
)
```

### Repository helper

Add `getAllImages(memoryId: Long): List<String>` to `MemoryRepository` that:
1. Reads `imageUrisJson` from the entity
2. If non-empty, deserializes the JSON array
3. If empty, returns `[entity.imageUri]` (backward compat)

---

## Multi-Photo Upload Flow

### Entry Point 1: System Photo Picker

**Android 13+ (API 33+)**: Use `PickMultipleVisualMedia` contract. Select up to 9 images, then navigate to NewPostScreen with all URIs.
**Android 12 and below**: Use existing single-photo `PickVisualMedia` contract. NewPostScreen supports in-app adding more photos.

### Entry Point 2: In-App Adding from NewPostScreen

NewPostScreen has a "Add more photos" button. Tapping it opens the system photo picker (single or multiple) and appends the selected URIs to the current list.

### NewPostScreen — Multi-Photo UI

**Image section**:
- Main preview: first image fills width, aspect ratio 3:2, rounded corners (28dp)
- Thumbnail row: scrollable horizontal `LazyRow` of 100dp×100dp square thumbnails below the main preview
- Tapping a thumbnail swaps it as the main preview
- Each thumbnail has a small "×" badge (top-right corner) to remove that image
- "Add photos" button (icon + label) appears as the last item in the thumbnail row
- Maximum 9 images — "Add" button is disabled when at limit

**Navigation change**:
```kotlin
// NavDestinations.kt
data object NewPost : NavDestinations("new_post/{encodedUris}") {
    fun createRoute(uris: List<Uri>) =
        "new_post/${android.net.Uri.encode(uris.joinToString("|") { it.toString() })}"
}
```

**NewPostScreen signature**:
```kotlin
fun NewPostScreen(
    imageUris: List<Uri>,  // was imageUri: Uri
    datesTaken: List<Long>, // EXIF dates, one per URI
    onPublish: (String, List<String>) -> Unit,
    onBack: () -> Unit
)
```

---

## MemoryDetailScreen

### Entry Points
- **TimelineView**: Tap any memory card → MemoryDetailScreen with that memory's ID
- **CalendarView**: Tap any calendar cell with photos → MemoryDetailScreen with that memory's ID

### Layout
```
┌─────────────────────────────────┐
│ ←        3/9         ♢           │  TopBar: back, page indicator, edit
├─────────────────────────────────┤
│                                 │
│     ┌───────────────────┐       │
│     │                   │       │
│     │   Full image      │       │  HorizontalPager with swipe
│     │   (tap to toggle  │       │
│     │    chrome)        │       │
│     │                   │       │
│     └───────────────────┘       │
│                                 │
├─────────────────────────────────┤
│ 2024年3月15日                    │  Date label
│ Morning clarity...               │  Mood text
│ #Life  #Mood                     │  Tags
├─────────────────────────────────┤
│  [○] [○] [●] [○] [○]            │  Thumbnail strip at bottom
└─────────────────────────────────┘
```

### Interactions
- **Swipe left/right**: Navigate between images within the same memory
- **Tap image**: Toggle chrome visibility (hide/show top bar + bottom info)
- **Tap thumbnail**: Jump to that image
- **Edit button (♢)**: Opens an edit bottom sheet with options:
  - "添加照片" — open system picker to add images to this memory
  - "删除此照片" — remove the currently viewed image
  - "删除整条影记" — delete the entire memory (with confirmation dialog)

### Implementation

**New file**: `app/src/main/java/com/yingjian/feature/memories/MemoryDetailScreen.kt`

Uses `HorizontalPager` from `com.google.accompanist:accompanist-pager` or Compose Foundation's built-in `HorizontalPager`.

Check if Foundation's `HorizontalPager` is available — it's in `compose-foundation` which should already be pulled in by Compose BOM.

**Route**:
```kotlin
data object MemoryDetail : NavDestinations("memory_detail/{memoryId}") {
    fun createRoute(memoryId: Long) = "memory_detail/$memoryId"
}
```

---

## TimelineView Updates

Each memory card becomes clickable:
- Wrap each card in `Modifier.clickable { onMemoryClick(memory) }`
- `TimelineView` signature changes to accept an `onMemoryClick: (MemoryRecordEntity) -> Unit` parameter
- For multi-photo memories, show a small photo count badge (e.g. "3") in the top-right corner of the featured card

---

## CalendarView Updates

Each calendar cell with photos becomes clickable:
- Wrap each cell in `Modifier.clickable { if (memories.size == 1) onMemoryClick(memories.first()) }`
- If a cell has multiple memories (unlikely in current design), show a bottom sheet list
- `CalendarView` signature changes to accept an `onMemoryClick: (MemoryRecordEntity) -> Unit` parameter

---

## File Inventory

### New files
- `app/src/main/java/com/yingjian/feature/memories/MemoryDetailScreen.kt`

### Modified files
- `app/src/main/java/com/yingjian/core/data/database/YingJianDatabase.kt` — Add `imageUrisJson` column, bump version to 3
- `app/src/main/java/com/yingjian/core/ui/navigation/NavDestinations.kt` — Change NewPost route, add MemoryDetail route
- `app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt` — Update NewPost nav, wire MemoryDetail
- `app/src/main/java/com/yingjian/feature/memories/MemoriesScreen.kt` — Add `onMemoryClick` callback to views, update FAB to support PickMultipleVisualMedia
- `app/src/main/java/com/yingjian/feature/memories/NewPostScreen.kt` — Accept List<Uri>, thumbnail grid, add/remove photos
- `app/src/main/java/com/yingjian/feature/memories/TimelineView.kt` — Add click support, photo count badge
- `app/src/main/java/com/yingjian/feature/memories/CalendarView.kt` — Add click support
- `app/src/main/java/com/yingjian/feature/memories/MemoriesViewModel.kt` — Update Add action to accept multiple URIs
- `app/src/main/java/com/yingjian/core/data/repository/MemoryRepository.kt` — Add getAllImages helper

---

## Implementation Order

1. **Data model + migration** — Add `imageUrisJson`, bump DB version, repository helper
2. **NewPostScreen multi-photo** — Update route, accept List<Uri>, thumbnail row, add/remove
3. **Navigation updates** — Update YingJianNavHost for multi-URI route
4. **TimelineView clickable** — Add onMemoryClick, photo count badge
5. **CalendarView clickable** — Add onMemoryClick
6. **MemoryDetailScreen** — New screen, HorizontalPager, chrome toggle
7. **MemoryDetailScreen edit** — Bottom sheet with add/remove/delete options
