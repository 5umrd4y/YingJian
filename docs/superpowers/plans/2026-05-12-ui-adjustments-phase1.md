# Phase 1 UI Adjustments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Apply 5 independent UI adjustments to existing screens — nav icon change, Memories default-to-timeline with TopBar toggle, EXIF date extraction + bento Timeline layout, Calendar overlay, local storage non-clickable.

**Architecture:** Each item is a focused change to existing Compose screens. No new screens. Data flow changes only in `MemoryRecordEntity` (one new column) and EXIF metadata extraction path. Room auto-migration handles schema change.

**Tech Stack:** Kotlin, Jetpack Compose, Room, ExifInterface, Coil 3

---

### Task 1: Local storage non-clickable + Nav icon change

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/settings/StorageListScreen.kt:84-93`
- Modify: `app/src/main/java/com/yingjian/core/ui/common/BottomNavigationBar.kt:7,37`

These two changes are trivial and isolated — combine into one task for efficiency.

- [ ] **Step 1: Make local storage non-clickable**

Modify `StorageListScreen.kt` line 90-92. Change the modifier from always clickable to conditionally clickable:

```kotlin
// Before:
modifier = Modifier
    .fillMaxWidth()
    .clickable { showConfigSheet = entry.key }

// After:
modifier = Modifier
    .fillMaxWidth()
    .then(
        if (entry.key == "local") Modifier
        else Modifier.clickable { showConfigSheet = entry.key }
    )
```

- [ ] **Step 2: Change 画册 nav icon to AutoStories**

In `BottomNavigationBar.kt`:

1. Change import line 7:
```kotlin
// Before:
import androidx.compose.material.icons.filled.Collections
// After:
import androidx.compose.material.icons.filled.AutoStories
```

2. Change line 37:
```kotlin
// Before:
BottomNavItem(NavDestinations.Photobook, "画册", Icons.Default.Collections),
// After:
BottomNavItem(NavDestinations.Photobook, "画册", Icons.Default.AutoStories),
```

- [ ] **Step 3: Build to verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/settings/StorageListScreen.kt
git add app/src/main/java/com/yingjian/core/ui/common/BottomNavigationBar.kt
git commit -m "fix: local storage non-clickable, 画册 nav icon to AutoStories"
```

---

### Task 2: Add ExifInterface dependency + getImageMetadata utility

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/yingjian/feature/memories/MemoriesScreen.kt:3-97`

- [ ] **Step 1: Add ExifInterface to libs.versions.toml**

Add version reference in the `[versions]` section:

```toml
exifinterface = "1.3.7"
```

Add library reference in the `[libraries]` section:

```toml
androidx-exifinterface = { group = "androidx.exifinterface", name = "exifinterface", version.ref = "exifinterface" }
```

- [ ] **Step 2: Add ExifInterface to app/build.gradle.kts**

In the `dependencies` block, add:

```kotlin
implementation(libs.androidx.exifinterface)
```

- [ ] **Step 3: Replace getImageDimensions with getImageMetadata**

In `MemoriesScreen.kt`, replace the `getImageDimensions` function (lines 90-97) with `getImageMetadata`:

```kotlin
import android.media.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Returns (width, height, dateTakenMs) from image URI.
 * Date priority: EXIF DATETIME_ORIGINAL > EXIF DATETIME > MediaStore DATE_MODIFIED > currentTime
 */
fun getImageMetadata(context: Context, uri: Uri): Triple<Int, Int, Long> {
    var width = 0
    var height = 0
    var dateTaken = System.currentTimeMillis()

    // Extract dimensions
    context.contentResolver.openInputStream(uri)?.use { stream ->
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(stream, null, options)
        width = options.outWidth
        height = options.outHeight
    }

    // Extract EXIF date
    context.contentResolver.openInputStream(uri)?.use { stream ->
        try {
            val exif = ExifInterface(stream)
            val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            if (dateTime != null) {
                SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).parse(dateTime)?.time?.let {
                    dateTaken = it
                }
            }
        } catch (_: Exception) {
            // Fall through to DATE_MODIFIED or current time
        }
    }

    // Fallback: MediaStore DATE_MODIFIED
    if (dateTaken == System.currentTimeMillis()) {
        context.contentResolver.query(
            uri,
            arrayOf(MediaStore.Images.Media.DATE_MODIFIED),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val modifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val modifiedSec = cursor.getLong(modifiedIndex)
                if (modifiedSec > 0) dateTaken = modifiedSec * 1000
            }
        }
    }

    return Triple(width, height, dateTaken)
}
```

- [ ] **Step 4: Build to verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/java/com/yingjian/feature/memories/MemoriesScreen.kt
git commit -m "feat: add ExifInterface dependency, replace getImageDimensions with getImageMetadata"
```

---

### Task 3: Add photoCount to MemoryRecordEntity + Room auto-migration

**Files:**
- Modify: `app/src/main/java/com/yingjian/core/data/database/YingJianDatabase.kt:16-27,111-115`

- [ ] **Step 1: Add photoCount column to MemoryRecordEntity**

In `YingJianDatabase.kt`, add the new field with default value:

```kotlin
@Entity(tableName = "memory_record")
data class MemoryRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imageUri: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val moodText: String?,
    val tags: String,
    val createdAt: Long,
    @androidx.room.ColumnInfo(name = "photo_count") val photoCount: Int = 1
)
```

- [ ] **Step 2: Bump database version with AutoMigration**

Change the `@Database` annotation (lines 111-115):

```kotlin
@TypeConverters(Converters::class)
@androidx.room.Database(
    entities = [MemoryRecordEntity::class, PhotobookEntity::class, PageLayoutEntity::class],
    version = 2,
    autoMigrations = [
        androidx.room.AutoMigration(from = 1, to = 2)
    ],
    exportSchema = true
)
```

- [ ] **Step 3: Update all MemoryRecordEntity construction sites**

Search for all places that construct `MemoryRecordEntity` and add `photoCount = 1` (or omit since it has a default):

1. `YingJianNavHost.kt` line 132-142 — the existing construction already has all named params, no change needed since `photoCount` has default `1`.
2. `MemoriesViewModel.kt` line 65-75 — same, default value covers it.

No code changes needed at construction sites because `photoCount` has a default value.

- [ ] **Step 4: Build to verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/yingjian/core/data/database/YingJianDatabase.kt
git commit -m "feat: add photoCount column with AutoMigration v1->v2"
```

---

### Task 4: Timeline bento layout — show image + date + mood

**Files:**
- Rewrite: `app/src/main/java/com/yingjian/feature/memories/TimelineView.kt`

- [ ] **Step 1: Rewrite TimelineView with bento layout**

Replace the entire `TimelineView.kt` content:

```kotlin
package com.yingjian.feature.memories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yingjian.core.data.database.MemoryRecordEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun TimelineView(memories: List<MemoryRecordEntity>) {
    if (memories.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "暂无照片",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Group by year-month
    val grouped = memories.groupBy { memory ->
        val cal = Calendar.getInstance().apply { timeInMillis = memory.timestamp }
        "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月"
    }

    val dateFormatter = SimpleDateFormat("yyyy年M月d日", Locale.getDefault())

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        grouped.forEach { (monthLabel, monthMemories) ->
            item {
                Text(
                    monthLabel,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 8.dp)
                )
            }

            // Featured entry (first in month) — full width card
            item {
                val featured = monthMemories.first()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    ) {
                        Column {
                            AsyncImage(
                                model = featured.imageUri,
                                contentDescription = featured.moodText,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(4f / 3f)
                                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                            )
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    dateFormatter.format(featured.timestamp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!featured.moodText.isNullOrBlank()) {
                                    Text(
                                        featured.moodText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Remaining entries — 2-column grid
            if (monthMemories.size > 1) {
                val restMemories = monthMemories.drop(1)
                items(items = restMemories.chunked(2)) { rowItems ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { memory ->
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                                )
                            ) {
                                Column {
                                    AsyncImage(
                                        model = memory.imageUri,
                                        contentDescription = memory.moodText,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                    )
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(
                                            dateFormatter.format(memory.timestamp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (!memory.moodText.isNullOrBlank()) {
                                            Text(
                                                memory.moodText,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(top = 2.dp),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // Fill remaining slot if odd count
                        if (rowItems.size == 1) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}
```

- [ ] **Step 2: Build to verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/memories/TimelineView.kt
git commit -m "feat: TimelineView bento layout with date + mood on each card"
```

---

### Task 5: NewPostScreen — show extracted date below image

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/memories/NewPostScreen.kt`
- Modify: `app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt:119-151`

- [ ] **Step 1: Update NewPostScreen to accept and display dateTaken**

Modify `NewPostScreen.kt` to accept a `dateTaken` parameter and display it:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPostScreen(
    imageUri: Uri,
    dateTaken: Long,
    onPublish: (String, List<String>) -> Unit,
    onBack: () -> Unit
) {
    var moodText by remember { mutableStateOf("") }
    val tags = remember { mutableStateListOf("#Life") }
    val defaultChips = listOf("#Life", "#Mood", "#Daily", "#Inspiration")

    val dateFormatter = java.text.SimpleDateFormat("yyyy年M月d日", java.util.Locale.getDefault())
    val dateDisplay = remember(dateTaken) { dateFormatter.format(dateTaken) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加影记") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { onPublish(moodText, tags.toList()) }) {
                        Icon(Icons.Default.Check, contentDescription = "发布")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Image preview
            AsyncImage(
                model = imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(28.dp))
            )

            // Date display
            Text(
                dateDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            // Mood text input (unchanged)
            Text(
                "此刻心情",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
            )
            BasicTextField(
                value = moodText,
                onValueChange = { moodText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .padding(12.dp),
                decorationBox = { innerTextField ->
                    if (moodText.isEmpty()) {
                        Text(
                            "记录此刻的心情...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                }
            )

            // Tag chips (unchanged)
            Text(
                "标签",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            LazyRow {
                items(defaultChips) { chip ->
                    FilterChip(
                        selected = tags.contains(chip),
                        onClick = {
                            if (tags.contains(chip)) tags.remove(chip) else tags.add(chip)
                        },
                        label = { Text(chip) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Update YingJianNavHost.kt to pass dateTaken**

In `YingJianNavHost.kt`, update the NewPost composable (lines 119-151):

```kotlin
composable(NavDestinations.NewPost.route) { backStackEntry ->
    val encodedUri = backStackEntry.arguments?.getString("encodedUri")
    val uri = encodedUri?.let { Uri.decode(it) }?.let { Uri.parse(it) }
    if (uri != null) {
        val context = LocalContext.current
        val (width, height, dateTaken) = getImageMetadata(context, uri)

        NewPostScreen(
            imageUri = uri,
            dateTaken = dateTaken,
            onPublish = { mood, tags ->
                // Insert directly via repository
                kotlinx.coroutines.runBlocking {
                    withContext(Dispatchers.IO) {
                        deps.memoryRepository.insertMemory(
                            com.yingjian.core.data.database.MemoryRecordEntity(
                                imageUri = uri.toString(),
                                imageWidth = width,
                                imageHeight = height,
                                timestamp = dateTaken,
                                latitude = null,
                                longitude = null,
                                moodText = mood.takeIf { it.isNotBlank() },
                                tags = Json.encodeToString(ListSerializer(String.serializer()), tags),
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
                refreshTrigger++
                navController.popBackStack()
            },
            onBack = { navController.popBackStack() }
        )
    }
}
```

Key change: `timestamp = dateTaken` (from EXIF) instead of `System.currentTimeMillis()`.

- [ ] **Step 3: Build to verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/memories/NewPostScreen.kt app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt
git commit -m "feat: NewPostScreen shows EXIF date, uses it as timestamp"
```

---

### Task 6: Memories default to Timeline with TopBar calendar toggle

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/memories/MemoriesScreen.kt`

- [ ] **Step 1: Replace TabRow with TopAppBar + calendar toggle**

Rewrite `MemoriesScreen.kt`:

```kotlin
package com.yingjian.feature.memories

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaStore
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.exifinterface.media.ExifInterface
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesScreen(
    viewModel: MemoriesViewModel,
    onNavigateToNewPost: (Uri) -> Unit
) {
    val context = LocalContext.current
    var showCalendar by rememberSaveable { mutableStateOf(false) }

    // Photo picker: after picking, navigate to NewPostScreen
    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { onNavigateToNewPost(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("影记") },
                actions = {
                    IconButton(onClick = { showCalendar = !showCalendar }) {
                        Icon(
                            imageVector = if (showCalendar) Icons.Default.ViewAgenda else Icons.Default.CalendarMonth,
                            contentDescription = if (showCalendar) "切换时光轴" else "切换日历"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "添加照片")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showCalendar) {
                CalendarView(memories = viewModel.uiState.memories)
            } else {
                TimelineView(memories = viewModel.uiState.memories)
            }
        }
    }
}
```

Note: The `getImageMetadata` function from Task 2 remains in this file (it was added in Task 2). Keep it below `MemoriesScreen`.

- [ ] **Step 2: Build to verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/memories/MemoriesScreen.kt
git commit -m "feat: Memories defaults to Timeline, TopBar calendar toggle replaces TabRow"
```

---

### Task 7: Calendar overlay — thumbnail background with date on top

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/memories/CalendarView.kt:71-93`

- [ ] **Step 1: Update CalendarView cell rendering**

Replace the Box composable inside the week loop (lines 73-92) with the overlay pattern:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment

// Inside the forEach loop for weekDays, replace the existing Box:
Box(
    modifier = Modifier.weight(1f).aspectRatio(1f).padding(1.dp)
) {
    if (dayInfo != null) {
        // Background: thumbnail
        dayInfo.memories.firstOrNull()?.let { memory ->
            AsyncImage(
                model = memory.imageUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } ?: Box(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )

        // Foreground: date overlay
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                dayInfo.day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    } else {
        // Empty day cell
        Box(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
            contentAlignment = Alignment.Center
        )
    }
}
```

- [ ] **Step 2: Verify CalendarView imports**

Ensure the following imports are present at the top of `CalendarView.kt`:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
```

- [ ] **Step 3: Build to verify**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/memories/CalendarView.kt
git commit -m "feat: Calendar overlay with thumbnail background and date on top"
```

---

## Implementation Order

1. Task 1: Local storage + Nav icon (isolated, trivial)
2. Task 2: ExifInterface + getImageMetadata (enables Tasks 4, 5)
3. Task 3: photoCount + AutoMigration (data model change)
4. Task 4: Timeline bento layout (core visual change)
5. Task 5: NewPostScreen date display + NavHost EXIF timestamp
6. Task 6: Memories TopBar toggle (depends on Task 4's Timeline)
7. Task 7: Calendar overlay (standalone visual change)
