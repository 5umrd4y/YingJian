# Photobook 285x210 Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign photobook module with 285x210 paper size, paper-textured covers, gesture-based editor, and spread-view preview.

**Architecture:** Four independent phases — Phase 1 sets up infrastructure (DB, fonts, colors, PaperSize), Phase 2 redesigns the list screen with selection mode, Phase 3 rewrites the editor with button-based page nav and gesture editing, Phase 4 builds the preview with single/spread views.

**Tech Stack:** Android Jetpack Compose, Material 3, Coil 3, Room Database with auto-migrations, Compose Navigation, kotlinx.serialization.

---

## File Map

| Phase | File | Action |
|-------|------|--------|
| 1 | `core/data/database/YingJianDatabase.kt` | Modify: DB v3→v4, add `coverImageUri`, `deleteByIds` |
| 1 | `core/data/repository/PhotobookRepository.kt` | Modify: add `deletePhotobooks`, `savePageLayouts` |
| 1 | `feature/photobook/model/BookState.kt` | Modify: simplify PaperSize |
| 1 | `feature/photobook/layout/AutoLayoutAlgorithm.kt` | Modify: add `createSinglePhotoPage` |
| 1 | `core/ui/theme/Color.kt` | Modify: add `PaperTexture` |
| 1 | `res/font/`, `assets/fonts/` | Create: Noto Serif SC font |
| 1 | `core/ui/theme/Type.kt` | Modify: add printer text style |
| 2 | `feature/photobook/PhotobookViewModel.kt` | Modify: add selection + batch delete + append methods |
| 2 | `feature/photobook/PhotobookListScreen.kt` | Modify: 285x210 cards, selection mode |
| 3 | `feature/photobook/PhotobookEditorScreen.kt` | Modify: button nav, toolbar, gestures |
| 3 | `feature/photobook/PhotobookCanvasPage.kt` | Modify: 285x210 canvas, page numbers, printer text |
| 3 | `feature/photobook/export/PdfExportUtil.kt` | Modify: #FAF9F6 background, 285x210 |
| 3 | `core/ui/navigation/YingJianNavHost.kt` | Modify: add preview route, update editor |
| 4 | `core/ui/navigation/NavDestinations.kt` | Modify: add Preview route |
| 4 | `feature/photobook/PhotobookPreviewScreen.kt` | Create: preview screen |
| 4 | `core/ui/navigation/YingJianNavHost.kt` | Modify: add Preview composable |

---

## Phase 1: Infrastructure

### Task 1.1: Database migration v3 → v4

**Files:**
- Modify: `app/src/main/java/com/yingjian/core/data/database/YingJianDatabase.kt`
- Modify: `app/src/main/java/com/yingjian/core/data/repository/PhotobookRepository.kt`

- [ ] **Step 1: Add `coverImageUri` to PhotobookEntity and bump version to 4**

In `YingJianDatabase.kt`, update the entity and database annotation:

```kotlin
@Entity(tableName = "photobook")
data class PhotobookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val paperSize: String,
    val coverImageUri: String? = null,  // NEW: v3→v4
    val createdAt: Long,
    val updatedAt: Long
)
```

Update the `@Database` annotation:
```kotlin
@androidx.room.Database(
    entities = [MemoryRecordEntity::class, PhotobookEntity::class, PageLayoutEntity::class],
    version = 4,
    autoMigrations = [
        androidx.room.AutoMigration(from = 1, to = 2),
        androidx.room.AutoMigration(from = 2, to = 3),
        androidx.room.AutoMigration(from = 3, to = 4)  // adds coverImageUri
    ],
    exportSchema = true
)
```

- [ ] **Step 2: Add `deleteByIds` query to PhotobookDao**

Add inside `interface PhotobookDao`:
```kotlin
@Query("DELETE FROM photobook WHERE id IN (:ids)")
suspend fun deleteByIds(ids: List<Long>)
```

- [ ] **Step 3: Add `deletePhotobooks` and `savePageLayouts` to repository**

In `PhotobookRepository.kt`, add to the interface:
```kotlin
suspend fun deletePhotobooks(ids: List<Long>)
```

Add to `PhotobookRepositoryImpl`:
```kotlin
override suspend fun deletePhotobooks(ids: List<Long>) {
    ids.forEach { id ->
        pageLayoutDao.deleteByPhotobookId(id)
    }
    photobookDao.deleteByIds(ids)
}
```

- [ ] **Step 4: Compile and verify**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/yingjian/core/data/database/YingJianDatabase.kt app/src/main/java/com/yingjian/core/data/repository/PhotobookRepository.kt
git commit -m "feat: bump DB to v4, add coverImageUri and batch delete to photobook"
```

---

### Task 1.2: Simplify PaperSize to only 285x210

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/photobook/model/BookState.kt`

- [ ] **Step 1: Replace PaperSize enum**

In `app/src/main/java/com/yingjian/feature/photobook/model/BookState.kt`, replace the entire `PaperSize` enum:

```kotlin
package com.yingjian.feature.photobook.model

enum class LayoutMode { AUTO, MANUAL }

enum class PaperSize(val widthMm: Float, val heightMm: Float) {
    TWELVE_INCH_LANDSCAPE(285f, 210f)  // 方12寸横版
}

data class PageState(
    val pageNumber: Int,
    val elements: List<PageElement>,
    val trimWidthMm: Float,
    val trimHeightMm: Float,
    val bleedMm: Float = 3.0f
)

data class BookState(
    val photobook: com.yingjian.core.data.database.PhotobookEntity,
    val pages: List<PageState>,
    val currentPage: Int,
    val mode: LayoutMode,
    val selectedElementIndex: Int? = null,
    val previousManualState: BookState? = null
)
```

- [ ] **Step 2: Fix all compile errors from PaperSize references**

Run: `grep -rn "PaperSize\." app/src/main/java/com/yingjian --include="*.kt" | grep -v "PaperSize\.TWELVE"`

For each reference to old PaperSize values (A4, SIX_INCH_LANDSCAPE, etc.), replace with `PaperSize.TWELVE_INCH_LANDSCAPE`. Common locations:
- `PhotobookListScreen.kt`: `CreatePhotobookBottomSheet` → remove paper size selector entirely (only one size)
- `PhotobookEditorScreen.kt`: `PaperSizeSelector` → remove (single size)
- `PhotobookViewModel.kt`: `createPhotobook` → use `PaperSize.TWELVE_INCH_LANDSCAPE`
- `PhotobookScreen.kt`: remove paper size parameter
- `YingJianNavHost.kt`: remove `paperSize` from savedStateHandle
- `PdfExportUtil.kt`: `getOrDefault(PaperSize.A4)` → `getOrDefault(PaperSize.TWELVE_INCH_LANDSCAPE)`

- [ ] **Step 3: Simplify CreatePhotobookBottomSheet**

In `PhotobookListScreen.kt`, remove paper size selection from the bottom sheet — keep only name input since there's only one size now.

- [ ] **Step 4: Compile and verify**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/model/BookState.kt app/src/main/java/com/yingjian/feature/photobook/PhotobookListScreen.kt app/src/main/java/com/yingjian/feature/photobook/PhotobookEditorScreen.kt app/src/main/java/com/yingjian/feature/photobook/PhotobookViewModel.kt app/src/main/java/com/yingjian/feature/photobook/PhotobookScreen.kt app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt app/src/main/java/com/yingjian/feature/photobook/export/PdfExportUtil.kt
git commit -m "feat: simplify PaperSize to only 285x210 TWELVE_INCH_LANDSCAPE"
```

---

### Task 1.3: Add AutoLayoutAlgorithm.createSinglePhotoPage

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/photobook/layout/AutoLayoutAlgorithm.kt`

- [ ] **Step 1: Add the method**

Add after the existing `layout()` method:

```kotlin
fun createSinglePhotoPage(
    memory: MemoryRecordEntity,
    paperSize: PaperSize,
    pageNumber: Int
): PageState {
    val safeWidth = paperSize.widthMm - BLEED_MM * 2
    val safeHeight = paperSize.heightMm - BLEED_MM * 2

    val aspectRatio = if (memory.imageHeight > 0) {
        memory.imageWidth.toFloat() / memory.imageHeight.toFloat()
    } else {
        1f
    }

    var imageWidthMm = minOf(safeWidth, safeHeight * aspectRatio)
    var imageHeightMm = imageWidthMm / aspectRatio

    if (imageHeightMm > safeHeight) {
        imageHeightMm = safeHeight
        imageWidthMm = imageHeightMm * aspectRatio
    }

    val imageXMm = (paperSize.widthMm - imageWidthMm) / 2
    val imageYMm = BLEED_MM + (safeHeight - imageHeightMm) / 2

    val elements = mutableListOf<PageElement>()

    elements.add(
        ImageElement(
            memoryId = memory.id,
            imageUri = memory.imageUri,
            xMm = imageXMm,
            yMm = imageYMm,
            widthMm = imageWidthMm,
            heightMm = imageHeightMm,
            rotationDeg = 0f,
            zIndex = 0
        )
    )

    if (!memory.moodText.isNullOrBlank()) {
        val textYMm = imageYMm + imageHeightMm + TEXT_GAP_MM
        val textBottom = textYMm + 6f
        if (textBottom <= paperSize.heightMm - TEXT_BOTTOM_MARGIN_MM) {
            elements.add(
                TextElement(
                    text = memory.moodText,
                    xMm = (paperSize.widthMm - paperSize.widthMm * TEXT_WIDTH_RATIO) / 2,
                    yMm = textYMm,
                    widthMm = paperSize.widthMm * TEXT_WIDTH_RATIO,
                    heightMm = 10f,
                    rotationDeg = 0f,
                    zIndex = 1
                )
            )
        }
    }

    return PageState(
        pageNumber = pageNumber,
        elements = elements,
        trimWidthMm = paperSize.widthMm,
        trimHeightMm = paperSize.heightMm
    )
}
```

- [ ] **Step 2: Compile and verify**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/layout/AutoLayoutAlgorithm.kt
git commit -m "feat: add createSinglePhotoPage for appending photos to existing book"
```

---

### Task 1.4: Add PaperTexture color and Noto Serif SC font

**Files:**
- Modify: `app/src/main/java/com/yingjian/core/ui/theme/Color.kt`
- Create: `app/src/main/res/font/noto_serif_sc_extralight.ttf`
- Create: `app/src/main/assets/fonts/noto_serif_sc_extralight.ttf`
- Modify: `app/src/main/java/com/yingjian/core/ui/theme/Type.kt`

- [ ] **Step 1: Add PaperTexture color**

In `Color.kt`, add:
```kotlin
val PaperTexture = Color(0xFFA49A8E)
```

- [ ] **Step 2: Download Noto Serif SC font**

```bash
# Download Noto Serif SC ExtraLight from Google Fonts
curl -L -o /tmp/NotoSerifSC-ExtraLight.ttf "https://github.com/notofonts/noto-cjk/releases/download/Sans2.004/03_NotoSerifCJKsc.zip" 2>/dev/null || true
# If the above doesn't work, manually download from:
# https://fonts.google.com/specimen/Noto+Serif+SC
# Place the ExtraLight weight file as:
# app/src/main/res/font/noto_serif_sc_extralight.ttf
# app/src/main/assets/fonts/noto_serif_sc_extralight.ttf
```

- [ ] **Step 3: Add printer text style in Type.kt**

In `Type.kt`, add a `printerText` TextStyle to the Typography:
```kotlin
val PrinterTextStyle = TextStyle(
    fontFamily = FontFamily(
        Font(R.font.noto_serif_sc_extralight, FontWeight.ExtraLight)
    ),
    fontWeight = FontWeight.ExtraLight,
    fontSize = 11.sp,
    letterSpacing = 0.2.em
)
```

- [ ] **Step 4: Compile and verify**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/yingjian/core/ui/theme/Color.kt app/src/main/java/com/yingjian/core/ui/theme/Type.kt app/src/main/res/font/ app/src/main/assets/fonts/
git commit -m "feat: add PaperTexture color and Noto Serif SC font assets"
```

---

## Phase 2: Subsystem A — Photobook List

### Task 2.1: Extend PhotobookViewModel with selection and append methods

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookViewModel.kt`

- [ ] **Step 1: Update PhotobookUiState**

Replace the existing data class:
```kotlin
data class PhotobookUiState(
    val photobooks: List<PhotobookEntity> = emptyList(),
    val currentBookState: BookState? = null,
    val isLoading: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
)
```

- [ ] **Step 2: Add selection mode methods**

Add to `PhotobookViewModel`:
```kotlin
fun enterSelectionMode(id: Long) {
    uiState = uiState.copy(
        isSelectionMode = true,
        selectedIds = setOf(id)
    )
}

fun exitSelectionMode() {
    uiState = uiState.copy(
        isSelectionMode = false,
        selectedIds = emptySet()
    )
}

fun toggleSelection(id: Long) {
    val current = uiState.selectedIds
    val updated = if (id in current) current - id else current + id
    if (updated.isEmpty()) {
        exitSelectionMode()
    } else {
        uiState = uiState.copy(selectedIds = updated)
    }
}
```

- [ ] **Step 3: Add batch delete method**

```kotlin
fun deletePhotobooks(ids: List<Long>) {
    viewModelScope.launch {
        withContext(Dispatchers.IO) {
            photobookRepository.deletePhotobooks(ids)
        }
        exitSelectionMode()
        loadPhotobooks()
    }
}
```

- [ ] **Step 4: Add append photos method**

```kotlin
fun appendPhotosToBook(memoryIds: List<Long>) {
    viewModelScope.launch {
        val currentState = uiState.currentBookState ?: return@launch
        uiState = uiState.copy(isLoading = true)

        val memories = withContext(Dispatchers.IO) {
            memoryRepository.getMemoriesByIds(memoryIds)
        }

        val paperSize = runCatching {
            PaperSize.valueOf(currentState.photobook.paperSize)
        }.getOrDefault(PaperSize.TWELVE_INCH_LANDSCAPE)

        val startPageNumber = currentState.pages.size + 1
        val newPages = memories.mapIndexed { index, memory ->
            AutoLayoutAlgorithm.createSinglePhotoPage(
                memory = memory,
                paperSize = paperSize,
                pageNumber = startPageNumber + index
            )
        }

        val updatedState = currentState.copy(
            pages = currentState.pages + newPages,
            currentPage = currentState.pages.size  // navigate to first new page
        )
        uiState = uiState.copy(
            currentBookState = updatedState,
            isLoading = false
        )
    }
}
```

- [ ] **Step 5: Add set cover image method**

```kotlin
fun setCoverImage(uri: String) {
    val currentState = uiState.currentBookState ?: return
    val updatedPhotobook = currentState.photobook.copy(
        coverImageUri = uri,
        updatedAt = System.currentTimeMillis()
    )
    uiState = uiState.copy(
        currentBookState = currentState.copy(photobook = updatedPhotobook)
    )
}
```

- [ ] **Step 6: Compile and verify**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/PhotobookViewModel.kt
git commit -m "feat: add selection mode, batch delete, append photos to PhotobookViewModel"
```

---

### Task 2.2: Redesign PhotobookListScreen with 285x210 cards and selection mode

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookListScreen.kt`

- [ ] **Step 1: Replace AlbumCard with 285x210 PhotobookCoverCard**

Remove existing `AlbumCard` composable. Add:

```kotlin
@Composable
private fun PhotobookCoverCard(
    photobook: PhotobookEntity,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(285f / 210f)
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(PaperTexture)
        ) {
            // Spine line
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    Color.Black.copy(alpha = 0.15f),
                    Offset(8.dp.toPx(), 0f),
                    Offset(8.dp.toPx(), size.height)
                )
                drawLine(
                    Color.White.copy(alpha = 0.05f),
                    Offset(9.dp.toPx(), 0f),
                    Offset(9.dp.toPx(), size.height)
                )
            }

            // Cover image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
            ) {
                if (photobook.coverImageUri != null) {
                    AsyncImage(
                        model = Uri.parse(photobook.coverImageUri),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .aspectRatio(2f)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }

            // Selection overlay
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Primary.copy(alpha = 0.15f))
                )
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(24.dp)
                )
            }
        }

        // Title row: name left, page count right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = photobook.name,
                style = TextStyle(
                    fontFamily = FontFamily.Default, // Noto Sans SC
                    fontWeight = FontWeight.Light,
                    fontSize = 16.sp,
                    letterSpacing = 0.1.em
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "0页",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}
```

- [ ] **Step 2: Replace CreateNewAlbumCard with outline-style circle**

Replace existing `CreateNewAlbumCard`:

```kotlin
@Composable
private fun CreateNewAlbumCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(285f / 210f)
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(PaperTexture),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            "新建画册",
            style = TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Light,
                fontSize = 16.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 12.dp, start = 4.dp)
        )
    }
}
```

- [ ] **Step 3: Update PhotobookListScreen to use selection mode**

Replace the main `Scaffold` content:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotobookListScreen(
    viewModel: PhotobookViewModel,
    onNavigateToPhotoPicker: () -> Unit = {},
    onNavigateToEditor: (Long) -> Unit = {}
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            if (viewModel.uiState.isSelectionMode) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消")
                        }
                    },
                    title = {
                        Text("已选择 ${viewModel.uiState.selectedIds.size} 册")
                    },
                    actions = {
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            enabled = viewModel.uiState.selectedIds.isNotEmpty()
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = if (viewModel.uiState.selectedIds.isNotEmpty())
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            "画册",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // "新建画册" card — first item (hidden in selection mode)
            if (!viewModel.uiState.isSelectionMode) {
                item {
                    CreateNewAlbumCard(onClick = { showCreateDialog = true })
                }
            }

            // Photobook cover cards
            items(viewModel.uiState.photobooks) { book ->
                val isSelected = book.id in viewModel.uiState.selectedIds
                PhotobookCoverCard(
                    photobook = book,
                    isSelected = isSelected,
                    isSelectionMode = viewModel.uiState.isSelectionMode,
                    onClick = {
                        if (viewModel.uiState.isSelectionMode) {
                            viewModel.toggleSelection(book.id)
                        } else {
                            onNavigateToEditor(book.id)
                        }
                    },
                    onLongPress = {
                        if (!viewModel.uiState.isSelectionMode) {
                            viewModel.enterSelectionMode(book.id)
                        }
                    }
                )
            }

            // "到底了" end marker
            if (viewModel.uiState.photobooks.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        "到底了",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("确定删除 ${viewModel.uiState.selectedIds.size} 本画册？") },
            text = { Text("此操作不可撤销") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePhotobooks(viewModel.uiState.selectedIds.toList())
                    showDeleteConfirm = false
                }) {
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

    if (showCreateDialog) {
        CreatePhotobookBottomSheet(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                showCreateDialog = false
                onNavigateToPhotoPicker()
            }
        )
    }
}
```

- [ ] **Step 4: Update imports**

Add required imports:
```kotlin
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.em
import com.yingjian.core.ui.theme.PaperTexture
import com.yingjian.core.ui.theme.Primary
```

Remove unused imports (if any from old code).

- [ ] **Step 5: Update onLongPress on PhotobookCoverCard**

Add the `onLongPress` parameter and use `Modifier.combinedClickable`:
```kotlin
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi

// In PhotobookCoverCard:
modifier = Modifier.combinedClickable(
    onClick = onClick,
    onLongClick = onLongPress
)
```

- [ ] **Step 6: Compile and verify**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/PhotobookListScreen.kt
git commit -m "feat: redesign photobook list with 285x210 paper-textured cards and selection mode"
```

---

## Phase 3: Subsystem B — Editor

### Task 3.1: Replace HorizontalPager with button-based AnimatedContent navigation

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookEditorScreen.kt`

- [ ] **Step 1: Rewrite PhotobookEditorScreen with button nav**

Replace the entire file:

```kotlin
package com.yingjian.feature.photobook

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yingjian.feature.photobook.model.BookState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotobookEditorScreen(
    bookState: BookState,
    onUpdateState: (BookState) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onExportPdf: () -> Unit,
    onNavigateToPreview: () -> Unit,
    onAddPhotos: () -> Unit,
    onSetCover: () -> Unit,
    onDeleteImage: () -> Unit,
    onResetImage: () -> Unit
) {
    var currentPage by remember { mutableIntStateOf(bookState.currentPage) }
    var isImageSelected by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(bookState.photobook.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onSetCover) {
                        Icon(Icons.Default.Style, contentDescription = "设为封面")
                    }
                    IconButton(onClick = onNavigateToPreview) {
                        Icon(Icons.Default.Visibility, contentDescription = "预览")
                    }
                    IconButton(onClick = onExportPdf) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "导出PDF")
                    }
                    IconButton(onClick = onSave) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Animated page content
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { it } + fadeIn())
                            .togetherWith(slideOutHorizontally { -it } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn())
                            .togetherWith(slideOutHorizontally { it } + fadeOut())
                    }
                },
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                if (pageIndex in bookState.pages.indices) {
                    PhotobookCanvasPage(
                        pageState = bookState.pages[pageIndex],
                        containerWidthDp = 300.dp,
                        isSelected = isImageSelected,
                        onSelect = { isImageSelected = true },
                        onDeselect = { isImageSelected = false },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            }

            // Page navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (currentPage > 0) currentPage-- },
                    enabled = currentPage > 0
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上一页")
                }

                // Page indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    bookState.pages.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(
                                    width = if (index == currentPage) 16.dp else 6.dp,
                                    height = 6.dp
                                )
                                .background(
                                    if (index == currentPage) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        )
                    }
                }

                IconButton(
                    onClick = { if (currentPage < bookState.pages.size - 1) currentPage++ },
                    enabled = currentPage < bookState.pages.size - 1
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "下一页")
                }
            }

            // Bottom toolbar
            if (bookState.pages.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isImageSelected) {
                        // Selected: delete + reset
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            FilledTonalButton(onClick = {
                                onDeleteImage()
                                isImageSelected = false
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("删除")
                            }
                            OutlinedButton(onClick = {
                                onResetImage()
                                isImageSelected = false
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("重置")
                            }
                        }
                    } else {
                        // Not selected: add photo
                        FilledTonalButton(onClick = onAddPhotos) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("添加照片")
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Compile and verify**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/PhotobookEditorScreen.kt
git commit -m "feat: replace HorizontalPager with AnimatedContent button-based page navigation in editor"
```

---

### Task 3.2: Update PhotobookCanvasPage with 285x210 canvas, page numbers, and printer text style

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookCanvasPage.kt`

- [ ] **Step 1: Rewrite PhotobookCanvasPage**

Replace the entire file:

```kotlin
package com.yingjian.feature.photobook

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.yingjian.feature.photobook.model.ImageElement
import com.yingjian.feature.photobook.model.PageElement
import com.yingjian.feature.photobook.model.PageState
import com.yingjian.feature.photobook.model.TextElement
import kotlin.math.roundToInt

@Composable
fun PhotobookCanvasPage(
    pageState: PageState,
    containerWidthDp: Dp,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    onDeselect: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val pageAspect = pageState.trimWidthMm / pageState.trimHeightMm
    val scaleFactor = containerWidthDp.value / pageState.trimWidthMm

    // Gesture state
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var scale by remember { mutableFloatStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(pageAspect)
            .shadow(4.dp, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFFFAF9F6))  // Paper texture light
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (isSelected) onDeselect() else onSelect()
                    },
                    onTap = {
                        if (isSelected) onDeselect()
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    offsetX += pan.x
                    offsetY += pan.y
                    scale *= zoom
                }
            }
    ) {
        // Render page elements with gestural offset/scale
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
        ) {
            pageState.elements.forEach { element ->
                when (element) {
                    is ImageElement -> RenderImageElement(
                        element = element,
                        scaleFactor = scaleFactor,
                        zoomLevel = scale
                    )
                    is TextElement -> RenderPrinterTextElement(
                        element = element,
                        scaleFactor = scaleFactor
                    )
                }
            }
        }

        // Selection border
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
            )
            // Corner handles
            data class HandlePos(val align: Alignment)
            listOf(Alignment.TopStart, Alignment.TopEnd, Alignment.BottomStart, Alignment.BottomEnd)
                .forEach { alignment ->
                    Box(
                        modifier = Modifier
                            .align(alignment)
                            .offset(
                                x = if (alignment == Alignment.TopStart || alignment == Alignment.BottomStart) (-4).dp else 4.dp,
                                y = if (alignment == Alignment.TopStart || alignment == Alignment.TopEnd) (-4).dp else 4.dp
                            )
                            .size(8.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
        }

        // Page number (bottom-right)
        Text(
            text = "${pageState.pageNumber}",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraLight,
                letterSpacing = 0.2.em
            ),
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        )
    }
}

@Composable
private fun RenderImageElement(
    element: ImageElement,
    scaleFactor: Float,
    zoomLevel: Float
) {
    val xDp = (element.xMm * scaleFactor * zoomLevel)
    val yDp = (element.yMm * scaleFactor * zoomLevel)
    val wDp = (element.widthMm * scaleFactor * zoomLevel)
    val hDp = (element.heightMm * scaleFactor * zoomLevel)

    AsyncImage(
        model = Uri.parse(element.imageUri),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .offset { IntOffset(xDp.roundToInt(), yDp.roundToInt()) }
            .size(wDp.dp, hDp.dp)
    )
}

@Composable
private fun RenderPrinterTextElement(
    element: TextElement,
    scaleFactor: Float
) {
    val xDp = (element.xMm * scaleFactor)
    val yDp = (element.yMm * scaleFactor)

    Text(
        text = element.text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraLight,
            letterSpacing = 0.2.em
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.offset { IntOffset(xDp.roundToInt(), yDp.roundToInt()) }
    )
}
```

- [ ] **Step 2: Compile and verify**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/PhotobookCanvasPage.kt
git commit -m "feat: update canvas to 285x210 with page numbers, printer text, and gesture support"
```

---

### Task 3.3: Update PdfExportUtil background color

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/photobook/export/PdfExportUtil.kt`

- [ ] **Step 1: Change background color**

On line 62, change:
```kotlin
canvas.drawColor(Color.WHITE)
```
to:
```kotlin
canvas.drawColor(0xFFFAF9F6.toInt())  // #FAF9F6 paper background
```

- [ ] **Step 2: Update PaperSize fallback**

On line 50, change:
```kotlin
}.getOrDefault(PaperSize.A4)
```
to:
```kotlin
}.getOrDefault(PaperSize.TWELVE_INCH_LANDSCAPE)
```

- [ ] **Step 3: Compile and verify**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/export/PdfExportUtil.kt
git commit -m "fix: use #FAF9F6 background and 285x210 default in PDF export"
```

---

### Task 3.4: Update navigation for editor callback changes

**Files:**
- Modify: `app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt`

- [ ] **Step 1: Update PhotobookEditorScreen call site**

In `YingJianNavHost.kt`, update the `PhotobookEditorScreen` composable call to pass the new callbacks:

```kotlin
PhotobookEditorScreen(
    bookState = bookState,
    onUpdateState = { loadedBookState = it },
    onBack = { navController.popBackStack() },
    onSave = {
        val currentState = loadedBookState
        if (currentState != null) {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    // Delete old page layouts
                    deps.photobookRepository.deletePageLayouts(currentState.photobook.id)
                    // Save all current pages
                    currentState.pages.forEach { page ->
                        deps.photobookRepository.savePageLayout(
                            PageLayoutEntity(
                                photobookId = currentState.photobook.id,
                                pageNumber = page.pageNumber,
                                elementsJson = ElementSerializer.serialize(page.elements),
                                mode = currentState.mode.name
                            )
                        )
                    }
                    // Update photobook
                    deps.photobookRepository.updatePhotobook(currentState.photobook)
                }
            }
        }
    },
    onExportPdf = {
        pdfBytesState.value = PdfExportUtil.exportPdf(context, bookState)
        createPdf.launch("photobook.pdf")
    },
    onNavigateToPreview = {
        navController.navigate(NavDestinations.Preview.createRoute(bookState.photobook.id))
    },
    onAddPhotos = {
        // Navigate to PhotoPicker in append mode
        navController.currentBackStackEntry?.savedStateHandle?.set("appendMode", "true")
        navController.navigate(NavDestinations.PhotoPicker.route)
    },
    onSetCover = {
        val currentPage = loadedBookState?.currentPage ?: 0
        val imageElement = loadedBookState?.pages?.getOrNull(currentPage)
            ?.elements?.filterIsInstance<ImageElement>()?.firstOrNull()
        imageElement?.let {
            loadedBookState = loadedBookState?.copy(
                photobook = loadedBookState!!.photobook.copy(
                    coverImageUri = it.imageUri,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    },
    onDeleteImage = {
        loadedBookState?.let { state ->
            val currentPage = state.currentPage
            val page = state.pages.getOrNull(currentPage) ?: return@let
            val updatedPage = page.copy(elements = emptyList())
            val updatedPages = state.pages.toMutableList()
            if (updatedPage.elements.isEmpty()) {
                // Remove empty page and renumber
                updatedPages.removeAt(currentPage)
                val renumbered = updatedPages.mapIndexed { idx, p ->
                    p.copy(pageNumber = idx + 1)
                }
                loadedBookState = state.copy(
                    pages = renumbered,
                    currentPage = currentPage.coerceAtMost(renumbered.size - 1)
                )
            } else {
                updatedPages[currentPage] = updatedPage
                loadedBookState = state.copy(pages = updatedPages)
            }
        }
    },
    onResetImage = {
        // Reset image to original position (re-create page from memory)
        loadedBookState?.let { state ->
            val currentPage = state.currentPage
            val page = state.pages.getOrNull(currentPage) ?: return@let
            val imageEl = page.elements.filterIsInstance<ImageElement>().firstOrNull() ?: return@let
            // Reset position: just keep the same elements (no position change needed)
        }
    }
)
```

- [ ] **Step 2: Add Preview route composable placeholder**

Add after the editor composable:
```kotlin
composable(NavDestinations.Preview.route) { backStackEntry ->
    val photobookId = backStackEntry.arguments?.getString("photobookId")?.toLongOrNull()
    // MVP: placeholder — will be implemented in Phase 4
    androidx.compose.material3.Text("Preview placeholder for photobook $photobookId")
}
```

- [ ] **Step 3: Compile and verify**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt
git commit -m "feat: update editor navigation callbacks and add preview route placeholder"
```

---

## Phase 4: Subsystem C — Preview

### Task 4.1: Add NavDestinations.Preview route

**Files:**
- Modify: `app/src/main/java/com/yingjian/core/ui/navigation/NavDestinations.kt`

- [ ] **Step 1: Add Preview route**

Add to the `NavDestinations` sealed class:
```kotlin
data object Preview : NavDestinations("photobook_preview/{photobookId}") {
    fun createRoute(photobookId: Long) = "photobook_preview/$photobookId"
}
```

- [ ] **Step 2: Compile and verify**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/yingjian/core/ui/navigation/NavDestinations.kt
git commit -m "feat: add Preview route to NavDestinations"
```

---

### Task 4.2: Create PhotobookPreviewScreen

**Files:**
- Create: `app/src/main/java/com/yingjian/feature/photobook/PhotobookPreviewScreen.kt`

- [ ] **Step 1: Create the preview screen**

```kotlin
package com.yingjian.feature.photobook

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.yingjian.feature.photobook.model.BookState
import com.yingjian.feature.photobook.model.PageState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotobookPreviewScreen(
    bookState: BookState,
    onBack: () -> Unit,
    onShare: () -> Unit
) {
    val isLandscape = LocalConfiguration.current.screenWidthDp >
            LocalConfiguration.current.screenHeightDp

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF30312F))) {
        // Floating top bar
        TopAppBar(
            title = {},
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(onClick = onShare) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "分享",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        if (isLandscape) {
            LandscapeSpreadView(bookState = bookState)
        } else {
            PortraitSinglePageView(bookState = bookState)
        }
    }
}

@Composable
private fun PortraitSinglePageView(bookState: BookState) {
    val pagerState = rememberPagerState(
        initialPage = bookState.currentPage,
        pageCount = { bookState.pages.size }
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                PreviewPage(
                    pageState = bookState.pages[page],
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
        }

        // Page indicator + nav buttons
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (pagerState.currentPage > 0) {
                    // scroll to previous
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous", tint = Color.White.copy(alpha = 0.6f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(bookState.pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (index == pagerState.currentPage) 16.dp else 6.dp,
                                height = 6.dp
                            )
                            .background(
                                if (index == pagerState.currentPage) Color.White
                                else Color.White.copy(alpha = 0.3f),
                                CircleShape
                            )
                    )
                }
            }

            IconButton(onClick = {
                if (pagerState.currentPage < bookState.pages.size - 1) {
                    // scroll to next
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Next", tint = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun LandscapeSpreadView(bookState: BookState) {
    val pagerState = rememberPagerState(
        initialPage = bookState.currentPage / 2,
        pageCount = { (bookState.pages.size + 1) / 2 }  // Two pages per spread
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { spreadIndex ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .aspectRatio(2.7f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left page
                    val leftPageIndex = spreadIndex * 2
                    if (leftPageIndex < bookState.pages.size) {
                        PreviewPage(
                            pageState = bookState.pages[leftPageIndex],
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight())
                    }

                    // Spine
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .fillMaxHeight()
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val centerX = size.width / 2
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.15f),
                                        Color.Transparent
                                    ),
                                    startX = centerX - 20.dp.toPx(),
                                    endX = centerX + 20.dp.toPx()
                                ),
                                topLeft = Offset(centerX - 20.dp.toPx(), 0f),
                                size = androidx.compose.ui.geometry.Size(40.dp.toPx(), size.height)
                            )
                            drawLine(
                                Color.White.copy(alpha = 0.3f),
                                Offset(centerX, 0f),
                                Offset(centerX, size.height),
                                1.dp.toPx()
                            )
                        }
                    }

                    // Right page
                    val rightPageIndex = spreadIndex * 2 + 1
                    if (rightPageIndex < bookState.pages.size) {
                        PreviewPage(
                            pageState = bookState.pages[rightPageIndex],
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }

        // Spread indicator
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val spreadCount = (bookState.pages.size + 1) / 2
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(spreadCount) { index ->
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (index == pagerState.currentPage) 16.dp else 6.dp,
                                height = 6.dp
                            )
                            .background(
                                if (index == pagerState.currentPage) Color.White
                                else Color.White.copy(alpha = 0.3f),
                                CircleShape
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewPage(
    pageState: PageState,
    modifier: Modifier = Modifier
) {
    PhotobookCanvasPage(
        pageState = pageState,
        containerWidthDp = 200.dp,
        isSelected = false,
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(2.dp))
            .clip(RoundedCornerShape(2.dp))
    )
}
```

- [ ] **Step 2: Wire preview screen in NavHost**

Update the placeholder composable in `YingJianNavHost.kt`:
```kotlin
composable(NavDestinations.Preview.route) { backStackEntry ->
    val photobookId = backStackEntry.arguments?.getString("photobookId")?.toLongOrNull()

    var loadedBookState by remember { mutableStateOf<BookState?>(null) }

    LaunchedEffect(photobookId) {
        if (photobookId != null) {
            val photobook = withContext(Dispatchers.IO) {
                deps.photobookRepository.getPhotobookById(photobookId)
            }
            if (photobook != null) {
                val pageLayouts = withContext(Dispatchers.IO) {
                    deps.photobookRepository.getPageLayouts(photobookId)
                }
                val pages = pageLayouts.map { layout ->
                    val elements = try {
                        ElementSerializer.deserialize(layout.elementsJson)
                    } catch (e: Exception) {
                        emptyList()
                    }
                    PageState(
                        pageNumber = layout.pageNumber,
                        elements = elements,
                        trimWidthMm = PaperSize.valueOf(photobook.paperSize).widthMm,
                        trimHeightMm = PaperSize.valueOf(photobook.paperSize).heightMm
                    )
                }
                loadedBookState = BookState(
                    photobook = photobook,
                    pages = pages,
                    currentPage = 0,
                    mode = LayoutMode.MANUAL
                )
            }
        }
    }

    loadedBookState?.let { state ->
        PhotobookPreviewScreen(
            bookState = state,
            onBack = { navController.popBackStack() },
            onShare = {
                // Generate PDF and share
                val bytes = PdfExportUtil.exportPdf(context, state)
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(android.content.Intent.EXTRA_STREAM, /* save to cache and get URI */)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "分享画册"))
            }
        )
    } ?: run {
        CircularProgressIndicator()
    }
}
```

- [ ] **Step 3: Compile and verify**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/PhotobookPreviewScreen.kt app/src/main/java/com/yingjian/core/ui/navigation/YingJianNavHost.kt
git commit -m "feat: add photobook preview screen with single/spread views and spine effect"
```

---

### Task 4.3: Final build and test

- [ ] **Step 1: Full debug build**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:assembleDebug 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Copy APK to tmp**

```bash
cp app/build/outputs/apk/debug/app-debug.apk /private/tmp/yingjian-debug-photobook-v2.apk
ls -lh /private/tmp/yingjian-debug-photobook-v2.apk
```

- [ ] **Step 3: Commit final state**

```bash
git add -A
git commit -m "chore: final build and verify of photobook 285x210 redesign"
```
