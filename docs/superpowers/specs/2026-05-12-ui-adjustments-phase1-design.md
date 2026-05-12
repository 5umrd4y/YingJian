# Phase 1: UI Adjustments Design Spec

## Overview

Five independent UI adjustments to existing screens. No data model changes, no new screens. Each item can be implemented and tested separately.

---

## 1. Different Nav Bar Icons (影记 vs 画册)

**Current**: Both use visually similar icons (`PhotoLibrary` and `Collections`).

**Design**: Change 画册 to `AutoStories` icon (book/library icon), keeping 影记 as `PhotoLibrary`.

**File**: `BottomNavigationBar.kt`

**Change**:
```kotlin
Icons.Default.PhotoLibrary  // 影记 — unchanged
Icons.Default.AutoStories   // 画册 — changed from Collections
```

Material3 `AutoStories` is available in the `androidx.compose.material:material-icons-extended` dependency already present in the version catalog.

---

## 2. Memories Default to Timeline, Calendar Toggle in TopBar

**Current**: `TabRow` with "时光轴" and "日历" tabs at the top of MemoriesScreen.

**Design**: Default to Timeline. Add a calendar icon button in the TopBar (right side) that toggles between Timeline and Calendar views.

**Reference**: Design HTML shows a TopBar with `calendar_month` icon button and `search` icon button on the right.

**Files**:
- `MemoriesScreen.kt` — Add `TopAppBar` with calendar toggle button, remove `TabRow`
- `NavDestinations.kt` — No change needed

**Change outline**:
```kotlin
// TopAppBar instead of nothing
TopAppBar(
    title = { Text("影记") },
    actions = {
        IconButton(onClick = { showCalendar = !showCalendar }) {
            Icon(if (showCalendar) Icons.Default.ViewAgenda else Icons.Default.CalendarMonth, ...)
        }
    }
)
```

The calendar icon toggles: `CalendarMonth` (default, tap → Calendar view) / `ViewAgenda` (calendar active, tap → Timeline view).

---

## 3. Use Image Date Instead of Upload Time, Show Photo + Date + Mood

**Current**: Uses `memory.timestamp` (upload time) for display. NewPostScreen shows only the image preview, mood input, and tags.

**Design**:
1. When picking an image, extract the **EXIF "date taken"** from the content URI as the primary date. Fall back to `contentResolver.query(uri, MediaStore.Images.Media.DATE_MODIFIED)` → fallback to `System.currentTimeMillis()`.
2. Store this in `MemoryRecordEntity.timestamp` (renamed logically to "image date").
3. NewPostScreen shows: image preview + date display (formatted from EXIF) + mood input.
4. TimelineView shows: date + moodText under each image (matching design HTML).
5. `MemoryRecordEntity` gets a new `photoCount` field (default 1) to support future multi-photo.

**Files**:
- `YingJianDatabase.kt` — Add `@ColumnInfo(name = "photo_count") val photoCount: Int = 1` to `MemoryRecordEntity`. Room auto-migration will handle the new column (use `fallbackToDestructiveMigration()` or add `@AutoMigration`).
- `getImageDimensions()` in `MemoriesScreen.kt` → Rename to `getImageMetadata()` and return `(width, height, dateTaken)` — extracts EXIF date.
- `MemoriesViewModel.kt` — `Add` action takes `imageDate: Long` instead of relying on `System.currentTimeMillis()` for `timestamp`.
- `NewPostScreen.kt` — Add date display below image preview, remove tags (design doesn't show tags on create).
- `TimelineView.kt` — Display date and moodText under each image, matching design HTML layout.
- `YingJianNavHost.kt` — Update `NewPost` navigation to pass image date.

**Image date extraction**:
```kotlin
// Use ExifInterface to read EXIF DateTimeOriginal
val inputStream = context.contentResolver.openInputStream(uri)
val exif = ExifInterface(inputStream!!)
val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME)
// Parse "yyyy:MM:dd HH:mm:ss" → Long
```

For images without EXIF (screenshots, etc.), fall back to `DATE_MODIFIED` from `MediaStore`.

---

## 4. (Skipped — Phase 2)

Multi-photo upload and MemoryDetailScreen deferred to Phase 2.

---

## 5. (Skipped — Phase 2)

Photo preview/edit deferred to Phase 2.

---

## 6. Calendar View: Date Overlay on Thumbnail

**Current**: Date number appears at top, thumbnail below with `padding(top = 16.dp)`. Thumbnail and date are separate visual layers. Click on thumbnail does nothing.

**Design**: Thumbnail fills the entire cell background. Date number overlays on top, centered, with a subtle background for readability.

**Reference**: Design HTML shows `rounded-xl overflow-hidden` image container, date displayed below as separate element in desktop layout. For mobile calendar, we overlay the date.

**File**: `CalendarView.kt`

**Change**:
```kotlin
Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(1.dp)) {
    // Background: thumbnail
    AsyncImage(
        model = memory.imageUri,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
    // Foreground: date overlay
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            dayInfo.day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
```

For days with multiple photos, show the first one as background. For days with no photos, show empty cell with just the day number (centered, light gray).

**Click interaction**: Tapping a calendar cell navigates to that day's photos in Timeline view (scrolls Timeline to the matching month). For MVP, just highlight the cell — navigation can be Phase 2.

---

## 7. Local Storage: Non-Clickable in Settings

**Current**: `ListItem` with `clickable` modifier opens `StorageConfigSheet` for all entries including local storage.

**Design**: Local storage entry is not clickable — it's always configured and can't be edited via config sheet.

**File**: `StorageListScreen.kt`

**Change**:
```kotlin
modifier = Modifier
    .fillMaxWidth()
    .then(if (entry.key == "local") Modifier else Modifier.clickable { showConfigSheet = entry.key })
```

Local storage shows "已连接" without click interaction. Other entries remain clickable.

---

## Room Migration Note

Adding `photoCount` field to `MemoryRecordEntity` requires a Room migration. The simplest MVP approach:

```kotlin
// In YingJianDatabase builder (YingJianApplication or AppDependencies)
.fallbackToDestructiveMigration()
```

This wipes data on schema change. For production, a proper `@AutoMigration` should be added. Since we're in MVP stage, destructive migration is acceptable. Alternatively, use `@Database(exportSchema = true, version = 2)` with `@AutoMigration(from = 1, to = 2)`.

---

## Implementation Order

1. **Icons** (item 1) — trivial, single-line change
2. **Local storage** (item 7) — trivial, modifier change
3. **Calendar overlay** (item 6) — UI-only, CalendarView.kt
4. **Image date + Timeline redesign** (item 3) — touches multiple files, core data flow
5. **Timeline default + TopBar** (item 2) — depends on 3's timeline changes
