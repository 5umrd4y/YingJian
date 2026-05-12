# Photobook Module Adjustments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix empty photo picker, replace create photobook AlertDialog with ModalBottomSheet, and redesign photobook list to 2-column grid matching the design reference.

**Architecture:** Three independent UI changes. Task 1 fixes PhotoPickerScreen's data loading and grid. Task 2 replaces AlertDialog with ModalBottomSheet in PhotobookListScreen. Task 3 is a full rewrite of PhotobookListScreen's list view to a 2-column grid with frame-style album cards per the HTML design reference.

**Tech Stack:** Android Jetpack Compose, Material 3, Coil 3, ViewModel, Room Database.

---

### Task 1: Fix Empty Photo Picker

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotoPickerScreen.kt`

**Root cause**: PhotoPickerScreen receives `memories` from NavHost's `rememberLoadedMemories`, which loads from DB asynchronously. The current code has no empty/loading state handling, and the two tabs are non-functional (no actual filtering logic).

- [ ] **Step 1: Simplify PhotoPickerScreen — remove unused tabs and add empty state**

Replace the entire `LazyVerticalGrid` section. Remove the non-functional TabRow ("全部照片" / "按月份浏览"). Show a 3-column grid when data exists, or an empty state message when no memories.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPickerScreen(
    memories: List<MemoryRecordEntity>,
    onBack: () -> Unit,
    onComplete: (List<Long>) -> Unit
) {
    val selectedIds = remember { mutableStateListOf<Long>() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择照片 (${selectedIds.size})") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (selectedIds.isNotEmpty()) {
                        TextButton(onClick = { onComplete(selectedIds.toList()) }) {
                            Text("完成")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (memories.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "暂无照片",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "请先在影记中添加照片",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(1.dp)
            ) {
                items(memories) { memory ->
                    val isSelected = selectedIds.contains(memory.id)
                    PhotoGridItem(
                        memory = memory,
                        isSelected = isSelected,
                        onToggle = {
                            if (isSelected) selectedIds.remove(memory.id)
                            else selectedIds.add(memory.id)
                        }
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 2: Update imports — remove unused**

Remove `Tab`, `TabRow`, `mutableIntStateOf`, `Box` (keep only the one needed for FAB if keeping it, but we're removing FAB too), `mutableStateOf`, `saveable`, `Icons.Default.CheckCircle` (keep), `Icons.Default.Visibility`, `FloatingActionButton`.

- [ ] **Step 3: Remove unused FAB code**

Delete the entire FAB section (the "预览选中" floating button at the bottom).

- [ ] **Step 4: Add TextButton import**

Add `import androidx.compose.material3.TextButton` to imports.

- [ ] **Step 5: Compile and verify**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/PhotoPickerScreen.kt
git commit -m "fix: simplify photo picker, remove non-functional tabs, add empty state"
```

---

### Task 2: Replace Create Photobook AlertDialog with ModalBottomSheet

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookListScreen.kt`

- [ ] **Step 1: Add imports for ModalBottomSheet**

```kotlin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
```

- [ ] **Step 2: Replace CreatePhotobookDialog with CreatePhotobookBottomSheet**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePhotobookBottomSheet(
    onDismiss: () -> Unit,
    onCreate: (String, PaperSize) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedSize by rememberSaveable { mutableStateOf(PaperSize.A4) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                "创建画册",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("画册名称") },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "纸张大小",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaperSize.entries.forEach { size ->
                    FilterChip(
                        selected = size == selectedSize,
                        onClick = { selectedSize = size },
                        label = { Text(size.name.replace("_", " ")) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                TextButton(
                    onClick = { onCreate(name.takeIf { it.isNotBlank() } ?: "未命名画册", selectedSize) },
                    enabled = name.isNotBlank()
                ) {
                    Text("下一步")
                }
            }
        }
    }
}
```

- [ ] **Step 3: Update the call site in PhotobookListScreen**

Change `CreatePhotobookDialog` to `CreatePhotobookBottomSheet`:

```kotlin
if (showCreateDialog) {
    CreatePhotobookBottomSheet(
        onDismiss = { showCreateDialog = false },
        onCreate = { name, paperSize ->
            showCreateDialog = false
            onNavigateToPhotoPicker(paperSize)
        }
    )
}
```

- [ ] **Step 4: Remove AlertDialog import**

Remove `import androidx.compose.material3.AlertDialog`.

- [ ] **Step 5: Compile and verify**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/PhotobookListScreen.kt
git commit -m "feat: replace create photobook dialog with ModalBottomSheet"
```

---

### Task 3: Redesign Photobook List to 2-Column Grid

**Files:**
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookListScreen.kt`
- Modify: `app/src/main/java/com/yingjian/feature/photobook/PhotobookScreen.kt` (update viewModel reference)

**Design reference**: `/Users/haos/Project/Android/YingJian/stitch_yingjian_huace/code.html`
- 2-column grid (`GridCells.Fixed(2)`)
- First item: "新建画册" card — dashed border, centered + icon, text below
- Album cards: aspect ratio 3:4, frame-style image (inset 4px), rounded corners, title centered below image, subtitle with page count

- [ ] **Step 1: Rewrite PhotobookListScreen content area**

Replace the entire `Scaffold` content body. Change from `LazyColumn` of cards to a `LazyVerticalGrid` with 2 columns.

```kotlin
@Composable
fun PhotobookListScreen(
    viewModel: PhotobookViewModel,
    onNavigateToPhotoPicker: (PaperSize) -> Unit = {},
    onNavigateToEditor: (Long) -> Unit = {}
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // "新建画册" card — first item
            item {
                CreateNewAlbumCard(onClick = { showCreateDialog = true })
            }

            // Album cards
            items(viewModel.uiState.photobooks) { book ->
                AlbumCard(
                    photobook = book,
                    onClick = { onNavigateToEditor(book.id) }
                )
            }

            // "到底了" end marker when list has items
            if (viewModel.uiState.photobooks.isNotEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        "到底了",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Empty state — show when only the "新建画册" card is visible
        if (viewModel.uiState.photobooks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "点击 + 创建你的第一本画册",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showCreateDialog) {
        CreatePhotobookBottomSheet(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, paperSize ->
                showCreateDialog = false
                onNavigateToPhotoPicker(paperSize)
            }
        )
    }
}
```

- [ ] **Step 2: Add CreateNewAlbumCard composable**

```kotlin
@Composable
private fun CreateNewAlbumCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                "新建画册",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
```

- [ ] **Step 3: Replace PhotobookCard with AlbumCard**

```kotlin
@Composable
private fun AlbumCard(
    photobook: PhotobookEntity,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        // Frame-style cover image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner frame with inset
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                // Cover image from first memory in this photobook
                // MVP: use placeholder color since we don't have a cover image yet
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                )
            }
        }

        // Title and subtitle
        Text(
            text = photobook.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = photobook.paperSize.replace("_", " "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
```

- [ ] **Step 4: Update imports**

Remove unused imports: `ElevatedCard`, `CardDefaults`, `IconButton`, `Icons.Default.Delete`, `Row`, `Column` (keep Column for CreateNewAlbumCard and AlbumCard), `Box` (keep), `Modifier` (keep). Add: `LazyVerticalGrid`, `GridCells`, `GridItemSpan`, `CircleShape`, `BorderStroke`, `Arrangement`, `PaddingValues`, `TextAlign`, `clip`.

Required new imports:
```kotlin
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
```

- [ ] **Step 5: Compile and verify**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/PhotobookListScreen.kt
git commit -m "feat: redesign photobook list to 2-column grid with frame-style cards"
```

---

### Task 4: Build and Test

- [ ] **Step 1: Full debug build**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:assembleDebug 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Copy APK to temp**

```bash
cp app/build/outputs/apk/debug/app-debug.apk /private/tmp/yingjian-debug-photobook.apk
ls -lh /private/tmp/yingjian-debug-photobook.apk
```
