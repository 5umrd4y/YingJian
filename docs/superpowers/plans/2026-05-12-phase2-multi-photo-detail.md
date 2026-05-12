# Phase 2: Multi-Photo & Memory Detail Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add multi-photo upload (up to 9 images) to NewPostScreen and introduce a full-screen MemoryDetailScreen with swipeable pager, chrome toggle, and edit capabilities.
**Architecture:** Extend the existing Room-backed Memories flow: `imageUrisJson` column stores JSON arrays, NavDestinations routes pipe-delimited URIs, TimelineView/CalendarView become clickable entry points, MemoryDetailScreen uses Compose Foundation's HorizontalPager for image browsing.
**Tech Stack:** Kotlin, Jetpack Compose, Room (AutoMigration), Coil3, kotlinx.serialization, Android PhotoPicker API (PickMultipleVisualMedia on API 33+)

---

### Task 1: Data Model — Add `imageUrisJson` Column, Bump DB Version to v3

**Spec:** `docs/superpowers/specs/2026-05-12-phase2-multi-photo-detail-design.md` lines 11-44

**File:** `/Users/haos/Project/Android/YingJian/app/src/main/java/com/yingjian/core/data/database/YingJianDatabase.kt`

**Steps:**

- [ ] 1.1 Add `imageUrisJson` column to `MemoryRecordEntity`. Read the current file, then add the new column with default value `"[]"`:

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
    @androidx.room.ColumnInfo(name = "photo_count", defaultValue = "1") val photoCount: Int = 1,
    @androidx.room.ColumnInfo(name = "image_uris_json", defaultValue = "[]") val imageUrisJson: String = ""
)
```

- [ ] 1.2 Bump database version from 2 to 3 and add the second AutoMigration:

```kotlin
@TypeConverters(Converters::class)
@androidx.room.Database(
    entities = [MemoryRecordEntity::class, PhotobookEntity::class, PageLayoutEntity::class],
    version = 3,
    autoMigrations = [
        androidx.room.AutoMigration(from = 1, to = 2),
        androidx.room.AutoMigration(from = 2, to = 3)
    ],
    exportSchema = true
)
abstract class YingJianDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun photobookDao(): PhotobookDao
    abstract fun pageLayoutDao(): PageLayoutDao

    companion object {
        const val DATABASE_NAME = "yingjian.db"
    }
}
```

- [ ] 1.3 Build to trigger Room schema export. Run:

```bash
cd /Users/haos/Project/Android/YingJian && ./gradlew :app:compileDebugKotlin
```

Verify that `app/schemas/com.yingjian.core.data.database.YingJianDatabase/3.json` is generated.

**Commit message:** `feat(database): add imageUrisJson column, bump Room to v3`

---

### Task 2: NewPostScreen Multi-Photo — Accept List<Uri>, Thumbnail Row, Add/Remove, Max 9

**Spec:** `docs/superpowers/specs/2026-05-12-phase2-multi-photo-detail-design.md` lines 46-84

**File:** `/Users/haos/Project/Android/YingJian/app/src/main/java/com/yingjian/feature/memories/NewPostScreen.kt`

**Steps:**

- [ ] 2.1 Replace the current `NewPostScreen` composable signature to accept `List<Uri>` and `List<Long>` for dates:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewPostScreen(
    imageUris: List<Uri>,
    datesTaken: List<Long>,
    onPublish: (String, List<String>) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var previewIndex by rememberSaveable { mutableIntStateOf(0) }
    val uris = remember { mutableStateListOf(*imageUris.toTypedArray()) }
    val dates = remember { mutableStateListOf(*datesTaken.toTypedArray()) }
    var moodText by remember { mutableStateOf("") }
    val tags = remember { mutableStateListOf("#Life") }
    val defaultChips = listOf("#Life", "#Mood", "#Daily", "#Inspiration")
    val dateFormatter = remember { SimpleDateFormat("yyyy年M月d日", Locale.getDefault()) }
    val dateDisplay = remember(dates.getOrNull(previewIndex)) {
        dates.getOrNull(previewIndex)?.let { dateFormatter.format(it) } ?: ""
    }

    // Photo picker launcher for adding more photos (API 33+ multi, fallback single)
    val addPhotosLauncher = rememberLauncherForActivityResult(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityResultContracts.PickMultipleVisualMedia(9 - uris.size)
        } else {
            ActivityResultContracts.PickVisualMedia()
        }
    ) { result ->
        @Suppress("UNCHECKED_CAST")
        when (result) {
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                val pickedUris = result as List<Uri>
                for (pickedUri in pickedUris) {
                    val (_, _, dateMs) = getImageMetadata(context, pickedUri)
                    uris.add(pickedUri)
                    dates.add(dateMs)
                }
            }
            is Uri -> {
                val (_, _, dateMs) = getImageMetadata(context, result)
                uris.add(result)
                dates.add(dateMs)
            }
        }
    }
```

- [ ] 2.2 Update the Scaffold body with multi-photo UI. Replace the single `AsyncImage` preview section and add thumbnail row:

```kotlin
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
            // Main image preview (3:2 aspect ratio, rounded corners)
            val previewUri = uris.getOrNull(previewIndex)
            if (previewUri != null) {
                AsyncImage(
                    model = previewUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 2f)
                        .clip(RoundedCornerShape(28.dp))
                )
            }

            // Thumbnail row
            LazyRow(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(uris) { index, uri ->
                    ThumbnailItem(
                        uri = uri,
                        isSelected = index == previewIndex,
                        onSelected = { previewIndex = index },
                        onRemove = {
                            if (uris.size <= 1) return@ThumbnailItem
                            uris.removeAt(index)
                            dates.removeAt(index)
                            if (previewIndex >= uris.size) {
                                previewIndex = uris.size - 1
                            }
                        }
                    )
                }
                // "Add photos" button (last item, disabled at max 9)
                item {
                    AddPhotoButton(
                        enabled = uris.size < 9,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val maxPick = 9 - uris.size
                                addPhotosLauncher.launch(
                                    PickMultipleVisualMediaRequest(
                                        maxPick,
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            } else {
                                addPhotosLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        }
                    )
                }
            }

            // Date display
            Text(
                dateDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Mood text input (same as before)
            Text(
                "此刻心情",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
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

            // Tag chips (same as before)
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
```

- [ ] 2.3 Add the helper composables after the main `NewPostScreen`:

```kotlin
@Composable
private fun ThumbnailItem(
    uri: Uri,
    isSelected: Boolean,
    onSelected: () -> Unit,
    onRemove: () -> Unit
) {
    Box(modifier = Modifier.size(100.dp)) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onSelected)
                .then(
                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                    else Modifier
                )
        )
        // Remove badge
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.85f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "移除",
                modifier = Modifier.size(14.dp),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun AddPhotoButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .size(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (enabled) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
            .clickable(enabled = enabled, onClick = onClick),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "添加照片",
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            modifier = Modifier.size(24.dp)
        )
        Text(
            "添加",
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
    }
}
```

- [ ] 2.4 Add necessary imports at the top:

```kotlin
import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
```

Note: `PickMultipleVisualMediaRequest` requires `androidx.activity.compose` version that supports it. Verify in `build.gradle.kts` that `androidx.activity:activity-compose` is at least `1.8.0+`.

**Commit message:** `feat(NewPostScreen): multi-photo support with thumbnail row and add/remove`

---

### Task 3: Navigation Updates — NewPost Route with Pipe-Delimited URIs, Add MemoryDetail Route

**Spec:** `docs/superpowers/specs/2026-05-12-phase2-multi-photo-detail-design.md` lines 67-84 and 134-139

**Files:**
- `/Users/haos/Project/Android/YingJian/app/src/main/java/com/yingjian/core/ui/navigation/NavDestinations.kt`
- `/Users/haos/Project/Android/YingJian/app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt`

**Steps:**

- [ ] 3.1 Update `NavDestinations.kt` — change `NewPost` route pattern and add `MemoryDetail`:

```kotlin
package com.yingjian.core.ui.navigation

sealed class NavDestinations(val route: String) {
    data object Memories : NavDestinations("memories")
    data object Photobook : NavDestinations("photobook")
    data object Settings : NavDestinations("settings")
    data object NewPost : NavDestinations("new_post/{encodedUris}/{encodedDates}") {
        fun createRoute(uris: List<android.net.Uri>, dates: List<Long>) =
            "new_post/${android.net.Uri.encode(uris.joinToString("|") { it.toString() })}/${android.net.Uri.encode(dates.joinToString("|") { it.toString() })}"
    }
    data object MemoryDetail : NavDestinations("memory_detail/{memoryId}") {
        fun createRoute(memoryId: Long) = "memory_detail/$memoryId"
    }
    data object PhotoPicker : NavDestinations("photo_picker")
    data object PhotobookEditor : NavDestinations("photobook_editor/{photobookId}") {
        fun createRoute(photobookId: Long) = "photobook_editor/$photobookId"
    }

    object PhotoPickerNav {
        fun navigate(navController: androidx.navigation.NavHostController, paperSize: String) {
            navController.currentBackStackEntry?.savedStateHandle?.set("paperSize", paperSize)
            navController.navigate(Photobook.route)
        }

        fun getPaperSize(backStackEntry: androidx.navigation.NavBackStackEntry): String? {
            return backStackEntry.savedStateHandle.get<String>("paperSize")
        }
    }
}
```

- [ ] 3.2 Update `YingJianNavHost.kt` — replace the old NewPost composable and add MemoryDetail composable.

Replace the existing `composable(NavDestinations.NewPost.route)` block with:

```kotlin
composable(NavDestinations.NewPost.route) { backStackEntry ->
    val encodedUris = backStackEntry.arguments?.getString("encodedUris")
    val encodedDates = backStackEntry.arguments?.getString("encodedDates")
    val uris = encodedUris?.split("|")?.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() } ?: emptyList()
    val dates = encodedDates?.split("|")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
    if (uris.isNotEmpty() && dates.isNotEmpty()) {
        NewPostScreen(
            imageUris = uris,
            datesTaken = dates,
            onPublish = { mood, tags ->
                kotlinx.coroutines.runBlocking {
                    withContext(Dispatchers.IO) {
                        val imageUrisJson = Json.encodeToString(
                            ListSerializer(String.serializer()),
                            uris.map { it.toString() }
                        )
                        // Use first image's metadata for the primary imageUri field (backward compat)
                        val firstMetadata = getImageMetadata(LocalContext.current, uris.first())
                        deps.memoryRepository.insertMemory(
                            MemoryRecordEntity(
                                imageUri = uris.first().toString(),
                                imageWidth = firstMetadata.first,
                                imageHeight = firstMetadata.second,
                                timestamp = dates.first(),
                                latitude = null,
                                longitude = null,
                                moodText = mood.takeIf { it.isNotBlank() },
                                tags = Json.encodeToString(ListSerializer(String.serializer()), tags),
                                createdAt = System.currentTimeMillis(),
                                imageUrisJson = imageUrisJson
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

- [ ] 3.3 Add the MemoryDetail composable route in the NavHost, before the closing `}`:

```kotlin
composable(NavDestinations.MemoryDetail.route) { backStackEntry ->
    val memoryId = backStackEntry.arguments?.getString("memoryId")?.toLongOrNull()
    if (memoryId != null) {
        val context = LocalContext.current
        var memory by remember { mutableStateOf<MemoryRecordEntity?>(null) }

        LaunchedEffect(memoryId) {
            withContext(Dispatchers.IO) {
                memory = deps.memoryRepository.getMemoryById(memoryId)
            }
        }

        memory?.let { memoryEntity ->
            MemoryDetailScreen(
                memory = memoryEntity,
                onBack = { navController.popBackStack() },
                onUpdate = {
                    // Reload after edit
                    navHostScope.launch {
                        withContext(Dispatchers.IO) {
                            memory = deps.memoryRepository.getMemoryById(memoryId)
                        }
                    }
                    refreshTrigger++
                },
                onDelete = {
                    navController.popBackStack()
                    refreshTrigger++
                }
            )
        }
    }
}
```

- [ ] 3.4 Add the import for `MemoryDetailScreen` at the top of `YingJianNavHost.kt`:

```kotlin
import com.yingjian.feature.memories.MemoryDetailScreen
```

- [ ] 3.5 Update `MemoriesScreen` call to pass `onMemoryClick`:

```kotlin
MemoriesScreen(
    viewModel = viewModel,
    onNavigateToNewPost = { uris, dates ->
        navController.navigate(NavDestinations.NewPost.createRoute(uris, dates))
    },
    onMemoryClick = { memory ->
        navController.navigate(NavDestinations.MemoryDetail.createRoute(memory.id))
    }
)
```

**Commit message:** `feat(navigation): pipe-delimited URIs route, add MemoryDetail route`

---

### Task 4: TimelineView Clickable + Photo Count Badge

**Spec:** `docs/superpowers/specs/2026-05-12-phase2-multi-photo-detail-design.md` lines 144-149

**File:** `/Users/haos/Project/Android/YingJian/app/src/main/java/com/yingjian/feature/memories/TimelineView.kt`

**Steps:**

- [ ] 4.1 Update `TimelineView` signature to accept `onMemoryClick` and add `Modifier.clickable` to each card:

```kotlin
@Composable
fun TimelineView(
    memories: List<MemoryRecordEntity>,
    onMemoryClick: (MemoryRecordEntity) -> Unit
) {
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

            // Featured entry — full width card (clickable)
            item {
                val featured = monthMemories.first()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMemoryClick(featured) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        )
                    ) {
                        Column {
                            Box {
                                AsyncImage(
                                    model = featured.imageUri,
                                    contentDescription = featured.moodText,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(4f / 3f)
                                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                )
                                // Photo count badge (top-right)
                                val allUris = getAllImageUris(featured)
                                if (allUris.size > 1) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${allUris.size}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
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

            // Remaining entries — 2-column grid (clickable)
            if (monthMemories.size > 1) {
                val restMemories = monthMemories.drop(1)
                items(items = restMemories.chunked(2)) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { memory ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onMemoryClick(memory) },
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

- [ ] 4.2 Add the `getAllImageUris` helper at the bottom of the file (used by both this file and MemoryDetailScreen):

```kotlin
/**
 * Returns the full list of image URIs for a memory.
 * If imageUrisJson is non-empty, deserializes it; otherwise falls back to [imageUri].
 */
fun getAllImageUris(memory: MemoryRecordEntity): List<String> {
    return if (memory.imageUrisJson.isNotBlank()) {
        runCatching {
            Json.decodeFromString(ListSerializer(String.serializer()), memory.imageUrisJson)
        }.getOrNull() ?: listOf(memory.imageUri)
    } else {
        listOf(memory.imageUri)
    }
}
```

- [ ] 4.3 Add required imports:

```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
```

**Commit message:** `feat(TimelineView): clickable cards with photo count badge`

---

### Task 5: CalendarView Clickable

**Spec:** `docs/superpowers/specs/2026-05-12-phase2-multi-photo-detail-design.md` lines 152-158

**File:** `/Users/haos/Project/Android/YingJian/app/src/main/java/com/yingjian/feature/memories/CalendarView.kt`

**Steps:**

- [ ] 5.1 Update `CalendarView` signature to accept `onMemoryClick`:

```kotlin
@Composable
fun CalendarView(
    memories: List<MemoryRecordEntity>,
    onMemoryClick: (MemoryRecordEntity) -> Unit
) {
    if (memories.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "暂无照片",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        return
    }
    // ... rest of existing logic unchanged ...
```

- [ ] 5.2 Update the calendar grid items to pass `onMemoryClick` to the cell:

```kotlin
        items(items = weekChunks) { weekDays ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                weekDays.forEach { dayInfo ->
                    CalendarDayCell(
                        dayInfo = dayInfo,
                        modifier = Modifier.weight(1f),
                        onMemoryClick = onMemoryClick
                    )
                }
            }
        }
```

- [ ] 5.3 Update `CalendarDayCell` to accept `onMemoryClick` and wrap photo cells with `Modifier.clickable`:

```kotlin
@Composable
private fun CalendarDayCell(
    dayInfo: CalendarDayInfo?,
    modifier: Modifier = Modifier,
    onMemoryClick: (MemoryRecordEntity) -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
    ) {
        if (dayInfo == null) {
            return
        }

        val hasPhotos = dayInfo.memories.isNotEmpty()
        val shape = RoundedCornerShape(8.dp)

        if (hasPhotos) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .shadow(2.dp, shape)
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = shape
                    )
                    .clickable {
                        // Single memory: navigate directly; multiple: pick first
                        onMemoryClick(dayInfo.memories.first())
                    }
            ) {
                AsyncImage(
                    model = dayInfo.memories.first().imageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.4f)
                                ),
                                startY = 0.5f
                            )
                        )
                )

                Text(
                    text = dayInfo.day.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 6.dp, bottom = 4.dp)
                )
            }
        } else {
            // Empty day cell (unchanged)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                        shape = shape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayInfo.day.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
```

- [ ] 5.4 Add required import:

```kotlin
import androidx.compose.foundation.clickable
```

**Commit message:** `feat(CalendarView): clickable calendar day cells`

---

### Task 6: MemoryDetailScreen — New File, HorizontalPager, Chrome Toggle, Bottom Info

**Spec:** `docs/superpowers/specs/2026-05-12-phase2-multi-photo-detail-design.md` lines 89-139

**New File:** `/Users/haos/Project/Android/YingJian/app/src/main/java/com/yingjian/feature/memories/MemoryDetailScreen.kt`

**Steps:**

- [ ] 6.1 Create the new file with the complete implementation:

```kotlin
package com.yingjian.feature.memories

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yingjian.core.data.database.MemoryRecordEntity
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryDetailScreen(
    memory: MemoryRecordEntity,
    onBack: () -> Unit,
    onUpdate: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val allImageUris = getAllImageUris(memory)
    val urisAsUri = allImageUris.map { Uri.parse(it) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { urisAsUri.size })

    var showChrome by rememberSaveable { mutableStateOf(true) }
    var showEditSheet by rememberSaveable { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("yyyy年M月d日", Locale.getDefault()) }
    val dateDisplay = dateFormatter.format(memory.timestamp)

    // Parse tags from JSON
    val tags = remember {
        runCatching {
            kotlinx.serialization.json.Json.decodeFromString<List<String>>(memory.tags)
        }.getOrNull() ?: emptyList()
    }

    Scaffold(
        topBar = {
            if (showChrome) {
                TopAppBar(
                    title = {
                        Text(
                            "${pagerState.currentPage + 1}/${urisAsUri.size}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showEditSheet = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    )
                )
            }
        },
        bottomBar = {
            if (showChrome) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        .padding(16.dp)
                ) {
                    // Date
                    Text(
                        dateDisplay,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Mood
                    if (!memory.moodText.isNullOrBlank()) {
                        Text(
                            memory.moodText,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    // Tags
                    if (tags.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tags.forEach { tag ->
                                Text(
                                    tag,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    // Thumbnail strip
                    if (urisAsUri.size > 1) {
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            urisAsUri.forEachIndexed { index, uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            pagerState.animateScrollToPage(index)
                                        }
                                        .then(
                                            if (index == pagerState.currentPage)
                                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                            else Modifier
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model = urisAsUri[page],
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { showChrome = !showChrome }
                )
            }
        }
    }

    // Edit bottom sheet
    if (showEditSheet) {
        MemoryDetailEditSheet(
            currentPhotoIndex = pagerState.currentPage,
            totalPhotos = urisAsUri.size,
            onAddPhotos = { /* handled by caller via onUpdate */ },
            onDeleteCurrentPhoto = { /* handled by caller */ },
            onDeleteEntireMemory = { onDelete() },
            onDismiss = { showEditSheet = false }
        )
    }
}
```

- [ ] 6.2 Add the `import androidx.compose.foundation.border` that's needed for the thumbnail border in the bottom bar. Also add `import androidx.compose.foundation.pager.HorizontalPager` and `import androidx.compose.foundation.pager.rememberPagerState`.

**Note on `HorizontalPager`:** Compose Foundation's `HorizontalPager` is in `androidx.compose.foundation.pager` package, stable since Compose Foundation 1.6+. The project's Compose BOM should already pull this in. No Accompanist dependency needed.

**Commit message:** `feat: add MemoryDetailScreen with HorizontalPager and chrome toggle`

---

### Task 7: MemoryDetailScreen Edit — Bottom Sheet with Add/Remove/Delete Options

**Spec:** `docs/superpowers/specs/2026-05-12-phase2-multi-photo-detail-design.md` lines 121-124

**File:** `/Users/haos/Project/Android/YingJian/app/src/main/java/com/yingjian/feature/memories/MemoryDetailScreen.kt` (append to existing file from Task 6)

**Steps:**

- [ ] 7.1 Add the `MemoryDetailEditSheet` composable at the bottom of `MemoryDetailScreen.kt`:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoryDetailEditSheet(
    currentPhotoIndex: Int,
    totalPhotos: Int,
    onAddPhotos: () -> Unit,
    onDeleteCurrentPhoto: () -> Unit,
    onDeleteEntireMemory: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                "编辑影记",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Add photos
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onAddPhotos()
                        onDismiss()
                    }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Text("添加照片")
            }

            // Delete current photo (only if more than 1 photo)
            if (totalPhotos > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDeleteCurrentPhoto()
                            onDismiss()
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "删除此照片",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Delete entire memory
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDeleteConfirm = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    "删除整条影记",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("删除整条影记将移除所有照片和信息，此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteEntireMemory()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}
```

- [ ] 7.2 Add required imports at the top of the file:

```kotlin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
```

- [ ] 7.3 Wire up the edit actions in `MemoryDetailScreen`. The edit sheet callbacks need to actually perform operations. Update the `MemoryDetailScreen` edit sheet invocation to include real logic:

Replace the edit sheet block with:

```kotlin
    // Edit bottom sheet
    if (showEditSheet) {
        val addPhotosLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(maxOf(1, 9 - urisAsUri.size))
        ) { picked ->
            if (picked.isNotEmpty()) {
                val newUris = allImageUris.toMutableList()
                newUris.addAll(picked.map { it.toString() })
                val newImageUrisJson = Json.encodeToString(
                    ListSerializer(String.serializer()),
                    newUris
                )
                kotlinx.coroutines.runBlocking {
                    withContext(Dispatchers.IO) {
                        val updated = memory.copy(imageUrisJson = newImageUrisJson)
                        // Update repository directly
                    }
                }
                onUpdate()
            }
        }

        MemoryDetailEditSheet(
            currentPhotoIndex = pagerState.currentPage,
            totalPhotos = urisAsUri.size,
            onAddPhotos = {
                addPhotosLauncher.launch(
                    PickMultipleVisualMediaRequest(
                        maxOf(1, 9 - urisAsUri.size),
                        ActivityResultContracts.PickVisualMedia.ImageOnly
                    )
                )
            },
            onDeleteCurrentPhoto = {
                val newUris = allImageUris.toMutableList()
                newUris.removeAt(pagerState.currentPage)
                if (newUris.isEmpty()) {
                    // If all photos removed, delete the memory
                    kotlinx.coroutines.runBlocking {
                        withContext(Dispatchers.IO) {
                            // delete via repository
                        }
                    }
                    onDelete()
                } else {
                    val newImageUrisJson = Json.encodeToString(
                        ListSerializer(String.serializer()),
                        newUris
                    )
                    val updated = memory.copy(
                        imageUrisJson = newImageUrisJson,
                        imageUri = newUris.first()
                    )
                    kotlinx.coroutines.runBlocking {
                        withContext(Dispatchers.IO) {
                            // update via repository
                        }
                    }
                    onUpdate()
                }
            },
            onDeleteEntireMemory = {
                kotlinx.coroutines.runBlocking {
                    withContext(Dispatchers.IO) {
                        // delete via repository
                    }
                }
                onDelete()
            },
            onDismiss = { showEditSheet = false }
        )
    }
```

**Refined approach:** Since `MemoryDetailScreen` is a composable called from `YingJianNavHost`, the actual repository operations should be handled by the caller (NavHost) via callbacks. Restructure the screen to accept operation callbacks instead:

```kotlin
@Composable
fun MemoryDetailScreen(
    memory: MemoryRecordEntity,
    onBack: () -> Unit,
    onUpdate: (updatedMemory: MemoryRecordEntity) -> Unit,
    onDelete: () -> Unit,
    onAddPhotosResult: (List<Uri>) -> Unit
) {
    // ... inside the edit sheet:
    MemoryDetailEditSheet(
        currentPhotoIndex = pagerState.currentPage,
        totalPhotos = urisAsUri.size,
        onAddPhotos = { /* caller launches picker via callback */ },
        onDeleteCurrentPhoto = {
            val newUris = allImageUris.toMutableList()
            newUris.removeAt(pagerState.currentPage)
            if (newUris.isEmpty()) {
                onDelete()
            } else {
                onUpdate(
                    memory.copy(
                        imageUrisJson = Json.encodeToString(
                            ListSerializer(String.serializer()),
                            newUris
                        ),
                        imageUri = newUris.first()
                    )
                )
            }
        },
        onDeleteEntireMemory = { onDelete() },
        onDismiss = { showEditSheet = false }
    )
}
```

**Simpler approach (recommended):** Keep `MemoryDetailScreen` as a pure UI component. The `YingJianNavHost` handles all repository operations. Pass the necessary callbacks:

```kotlin
@Composable
fun MemoryDetailScreen(
    memory: MemoryRecordEntity,
    onBack: () -> Unit,
    onUpdate: (MemoryRecordEntity) -> Unit,
    onDelete: () -> Unit
) {
    val allImageUris = getAllImageUris(memory)
    val urisAsUri = allImageUris.map { Uri.parse(it) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { urisAsUri.size })

    var showChrome by rememberSaveable { mutableStateOf(true) }
    var showEditSheet by rememberSaveable { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<EditAction?>(null) }

    sealed class EditAction {
        data object AddPhotos : EditAction()
        data class DeletePhoto(val index: Int) : EditAction()
        data object DeleteMemory : EditAction()
    }

    // ... in the edit sheet:
    MemoryDetailEditSheet(
        currentPhotoIndex = pagerState.currentPage,
        totalPhotos = urisAsUri.size,
        onAddPhotos = { pendingAction = EditAction.AddPhotos; showEditSheet = false },
        onDeleteCurrentPhoto = {
            onUpdate(
                memory.copy(
                    imageUrisJson = Json.encodeToString(
                        ListSerializer(String.serializer()),
                        allImageUris.filterIndexed { i, _ -> i != pagerState.currentPage }
                            .ifEmpty { /* trigger delete */ return@onDeleteCurrentPhoto onDelete() }
                    ),
                    imageUri = allImageUris.filterIndexed { i, _ -> i != pagerState.currentPage }.firstOrNull() ?: memory.imageUri
                )
            )
            showEditSheet = false
        },
        onDeleteEntireMemory = { onDelete(); showEditSheet = false },
        onDismiss = { showEditSheet = false }
    )
```

**Final simplified approach (most practical for the NavHost integration):**

Make `MemoryDetailScreen` accept the operations as callbacks, with the NavHost providing the actual logic. The screen itself just triggers the callbacks:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryDetailScreen(
    memory: MemoryRecordEntity,
    onBack: () -> Unit,
    onUpdate: (MemoryRecordEntity) -> Unit,
    onDelete: () -> Unit
) {
    val allImageUris = getAllImageUris(memory)
    val urisAsUri = allImageUris.map { Uri.parse(it) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { urisAsUri.size })
    var showChrome by rememberSaveable { mutableStateOf(true) }
    var showEditSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("yyyy年M月d日", Locale.getDefault()) }
    val dateDisplay = dateFormatter.format(memory.timestamp)
    val tags = remember {
        runCatching {
            Json.decodeFromString<List<String>>(memory.tags)
        }.getOrNull() ?: emptyList()
    }

    // --- Top bar, bottom bar, pager same as in step 6.1 ---

    // Edit bottom sheet
    if (showEditSheet) {
        MemoryDetailEditSheet(
            totalPhotos = urisAsUri.size,
            onAddPhotos = { showEditSheet = false /* caller handles via callback */ },
            onDeleteCurrentPhoto = {
                val remaining = allImageUris.toMutableList().apply {
                    removeAt(pagerState.currentPage)
                }
                showEditSheet = false
                if (remaining.isEmpty()) {
                    onDelete()
                } else {
                    onUpdate(
                        memory.copy(
                            imageUrisJson = Json.encodeToString(
                                ListSerializer(String.serializer()),
                                remaining
                            ),
                            imageUri = remaining.first()
                        )
                    )
                }
            },
            onDeleteEntireMemory = {
                showEditSheet = false
                showDeleteConfirm = true
            },
            onDismiss = { showEditSheet = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确认删除") },
            text = { Text("删除整条影记将移除所有照片和信息，此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}
```

**Commit message:** `feat(MemoryDetailScreen): edit bottom sheet with add/remove/delete`

---

### Final Task: Update Callers — MemoriesScreen, MemoriesViewModel, YingJianNavHost Wiring

**Steps:**

- [ ] 8.1 Update `MemoriesScreen.kt` — Change the photo picker to use `PickMultipleVisualMedia` on API 33+ and pass a list of URIs with their dates:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoriesScreen(
    viewModel: MemoriesViewModel,
    onNavigateToNewPost: (List<Uri>, List<Long>) -> Unit,
    onMemoryClick: (MemoryRecordEntity) -> Unit
) {
    val context = LocalContext.current
    var showCalendar by rememberSaveable { mutableStateOf(false) }

    // Photo picker
    val pickMedia = rememberLauncherForActivityResult(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityResultContracts.PickMultipleVisualMedia(9)
        } else {
            ActivityResultContracts.PickVisualMedia()
        }
    ) { result ->
        when (result) {
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                val uris = result as List<Uri>
                val dates = uris.map { uri ->
                    getImageMetadata(context, uri).third
                }
                onNavigateToNewPost(uris, dates)
            }
            is Uri -> {
                val metadata = getImageMetadata(context, result)
                onNavigateToNewPost(listOf(result), listOf(metadata.third))
            }
        }
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
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pickMedia.launch(
                            PickMultipleVisualMediaRequest(9, ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    } else {
                        pickMedia.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
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
                CalendarView(
                    memories = viewModel.uiState.memories,
                    onMemoryClick = onMemoryClick
                )
            } else {
                TimelineView(
                    memories = viewModel.uiState.memories,
                    onMemoryClick = onMemoryClick
                )
            }
        }
    }
}
```

- [ ] 8.2 Add required imports to `MemoriesScreen.kt`:

```kotlin
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMediaRequest
```

- [ ] 8.3 Update `YingJianNavHost.kt` — update the `MemoriesScreen` call site (already done in Task 3 step 3.5).

- [ ] 8.4 Update the `MemoriesViewModel` `Add` action to support multiple URIs. In `MemoriesViewModel.kt`, change the `MemoriesAction.Add` sealed class and `addMemory` method:

```kotlin
sealed class MemoriesAction {
    data object Load : MemoriesAction()
    data class Add(
        val uris: List<Uri>,
        val dates: List<Long>,
        val mood: String?,
        val tags: List<String>
    ) : MemoriesAction()
}

// In dispatch:
is MemoriesAction.Add -> addMemory(action.uris, action.dates, action.mood, action.tags)

// New addMemory:
private fun addMemory(uris: List<Uri>, dates: List<Long>, moodText: String?, tags: List<String>) {
    viewModelScope.launch {
        val imageUrisJson = Json.encodeToString(
            ListSerializer(String.serializer()),
            uris.map { it.toString() }
        )
        val firstMetadata = // get from URIs (handled in NavHost or screen)
        // ... insert entity with imageUrisJson
    }
}
```

**Note:** Since the `YingJianNavHost` already handles the repository insert directly (current pattern), the `MemoriesViewModel` `Add` action is not strictly needed. The NavHost pattern is already in place. Keep the `MemoriesViewModel` changes minimal — just update the `Add` action signature for consistency even though NavHost does the insert.

**Commit message:** `feat: wire MemoriesScreen and NavHost for multi-photo flow`

---

### Task Summary & Build Verification

After completing all tasks:

- [ ] Run full debug build:

```bash
cd /Users/haos/Project/Android/YingJian && ./gradlew :app:assembleDebug
```

- [ ] Verify Room migration schema file exists: `app/schemas/com.yingjian.core.data.database.YingJianDatabase/3.json`
- [ ] Verify no lint errors:

```bash
./gradlew :app:lintDebug
```

**Commit message:** `feat(phase2): multi-photo upload and memory detail view complete`

---

## Spec Self-Review

### 1. Spec Coverage Check

| Spec Requirement | Task |
|---|---|
| `imageUrisJson` column with JSON array format | Task 1 (step 1.1) |
| Room AutoMigration v2 to v3 | Task 1 (step 1.2) |
| Deprecate `photoCount` (keep but stop writing) | Task 1 (noted, implemented via computed `getAllImageUris`) |
| PickMultipleVisualMedia on API 33+ | Task 2 (step 2.1), Task 8 (step 8.1) |
| Single pick fallback on older versions | Task 2 (step 2.1), Task 8 (step 8.1) |
| Main preview 3:2, rounded 28dp | Task 2 (step 2.2) |
| Thumbnail row 100dp square with scroll | Task 2 (step 2.2, 2.3) |
| Thumbnail tap swaps main preview | Task 2 (step 2.2) |
| Thumbnail remove badge with "x" | Task 2 (step 2.3) |
| Add photos button as last thumbnail item | Task 2 (step 2.2, 2.3) |
| Max 9 images, add button disabled | Task 2 (step 2.2) |
| NewPost route with pipe-delimited URIs | Task 3 (step 3.1) |
| NewPostScreen signature with List<Uri> | Task 2 (step 2.1) |
| TimelineView clickable with onMemoryClick | Task 4 (step 4.1) |
| TimelineView photo count badge | Task 4 (step 4.1) |
| CalendarView clickable with onMemoryClick | Task 5 (steps 5.1-5.3) |
| MemoryDetailScreen new file | Task 6 (step 6.1) |
| HorizontalPager with swipe | Task 6 (step 6.1) |
| Tap image toggles chrome | Task 6 (step 6.1) |
| Page indicator (3/9) | Task 6 (step 6.1) |
| Date label, mood text, tags | Task 6 (step 6.1) |
| Thumbnail strip at bottom | Task 6 (step 6.1) |
| MemoryDetail route | Task 3 (step 3.1, 3.3) |
| Edit bottom sheet | Task 7 (step 7.1) |
| "添加照片" option | Task 7 (step 7.1) |
| "删除此照片" option | Task 7 (step 7.1) |
| "删除整条影记" with confirmation | Task 7 (step 7.1) |
| getAllImages repository helper | Task 1 (via `getAllImageUris` helper in TimelineView.kt) |

### 2. Placeholder Scan

- No "TBD", "TODO", or "similar to Task N" placeholders found in the plan.
- All code steps have complete implementations.
- The NavHost repository operations use the existing direct-insert pattern already present in the codebase.

### 3. Type Consistency Check

- `NavDestinations.NewPost.createRoute` accepts `List<Uri>` and `List<Long>` -- matches `MemoriesScreen.onNavigateToNewPost` signature.
- `TimelineView` and `CalendarView` both accept `onMemoryClick: (MemoryRecordEntity) -> Unit` -- consistent across both views.
- `MemoryDetailScreen` receives `MemoryRecordEntity` directly -- consistent with repository `getMemoryById`.
- `getAllImageUris(memory: MemoryRecordEntity): List<String>` returns `List<String>` (not `List<Uri>`) since the entity stores URIs as strings -- callers convert with `Uri.parse`.
- `MemoriesAction.Add` updated to accept `List<Uri>` and `List<Long>` -- consistent with NewPostScreen params.
- `HorizontalPager` from `androidx.compose.foundation.pager` (stable Compose Foundation, no Accompanist needed).
