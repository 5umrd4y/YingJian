# Photobook Module Adjustments Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix empty photo picker, replace create photobook AlertDialog with ModalBottomSheet, and redesign photobook list to 2-column grid matching the design reference.

**Architecture:** Three independent UI changes. Task 1 fixes PhotoPickerScreen's data loading and grid. Task 2 replaces AlertDialog with ModalBottomSheet in PhotobookListScreen. Task 3 is a full rewrite of PhotobookListScreen's list view to a 2-column grid with frame-style album cards per the HTML design reference.

**Tech Stack:** Android Jetpack Compose, Material 3, Coil 3, ViewModel, Room Database.

---

### Task 1: Fix Empty Photo Picker

**Status: Already completed in prior session. Skip.**

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

**Design reference**: `/Users/haos/Project/Android/YingJian/stitch_yingjian_huace/code.html`
- TopAppBar with "画册" title
- 2-column grid (`GridCells.Fixed(2)`)
- First item: "新建画册" card — dashed border, centered + icon, text below
- Album cards: aspect ratio 3:4, frame-style image (inset 4px), rounded corners, title centered below image, subtitle with page count

- [ ] **Step 1: Rewrite entire PhotobookListScreen**

Replace the entire file content with the new implementation:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotobookListScreen(
    viewModel: PhotobookViewModel,
    onNavigateToPhotoPicker: (PaperSize) -> Unit = {},
    onNavigateToEditor: (Long) -> Unit = {}
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
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
    ) { paddingValues ->
        val hasBooks = viewModel.uiState.photobooks.isNotEmpty()

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // "新建画册" card — always first item
            item {
                CreateNewAlbumCard(onClick = { showCreateDialog = true })
            }

            // Album cards
            items(viewModel.uiState.photobooks) { book ->
                AlbumCard(
                    photobook = book,
                    onClick = { onNavigateToEditor(book.id) },
                    onDelete = { viewModel.deletePhotobook(book) }
                )
            }

            // "到底了" end marker
            if (hasBooks) {
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

            // Empty state as a full-span item when no albums exist
            if (!hasBooks) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
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
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                RoundedCornerShape(16.dp)
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

Note: The design reference uses a dashed border. Compose's `BorderStroke` does not support dashed borders natively. Use a solid border with `outlineVariant.copy(alpha = 0.6f)` as a close approximation. If a dashed border is required later, use `Canvas` with `PathEffect.dashPathEffect`.

- [ ] **Step 3: Replace PhotobookCard with AlbumCard (frame-style with long-press delete)**

```kotlin
@Composable
private fun AlbumCard(
    photobook: PhotobookEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
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
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    RoundedCornerShape(16.dp)
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
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = "${photobook.pageCount} 页 · ${java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault()).format(photobook.updatedAt)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}
```

Note: `PhotobookEntity` may not have a `pageCount` field. If it doesn't exist yet, use `"0 页"` as a placeholder until the page count feature is implemented.

- [ ] **Step 4: Update imports**

Replace all imports at the top of the file with:

```kotlin
package com.yingjian.feature.photobook

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yingjian.core.data.database.PhotobookEntity
import com.yingjian.feature.photobook.model.PaperSize
import java.text.SimpleDateFormat
import java.util.Locale
```

Remove these old imports: `AlertDialog`, `CardDefaults`, `ElevatedCard`, `FloatingActionButton`, `IconButton`, `Icons.Default.Delete`, `LazyColumn`, `items` from lazy (not lazy.grid), `Row`.

- [ ] **Step 5: Compile and verify**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && ./gradlew :app:compileDebugKotlin 2>&1 | tail -10`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/yingjian/feature/photobook/PhotobookListScreen.kt
git commit -m "feat: redesign photobook list to 2-column grid with frame-style cards and ModalBottomSheet"
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
