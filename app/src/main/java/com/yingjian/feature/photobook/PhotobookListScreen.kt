package com.yingjian.feature.photobook

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yingjian.core.data.database.PhotobookEntity
import com.yingjian.feature.photobook.model.PaperSize
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PhotobookListScreen(
    viewModel: PhotobookViewModel,
    onNavigateToPhotoPicker: (PaperSize) -> Unit = {},
    onNavigateToEditor: (Long) -> Unit = {}
) {
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "创建画册")
            }
        }
    ) { paddingValues ->
        if (viewModel.uiState.photobooks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "暂无画册",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "点击 + 创建你的第一本画册",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.uiState.photobooks) { book ->
                    PhotobookCard(
                        photobook = book,
                        onClick = { onNavigateToEditor(book.id) },
                        onDelete = { viewModel.deletePhotobook(book) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePhotobookDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, paperSize ->
                showCreateDialog = false
                onNavigateToPhotoPicker(paperSize)
            }
        )
    }
}

@Composable
fun PhotobookCard(
    photobook: PhotobookEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    photobook.name,
                    style = MaterialTheme.typography.titleLarge
                )
                val paperSize = runCatching {
                    PaperSize.valueOf(photobook.paperSize)
                }.getOrDefault(PaperSize.A4)
                Text(
                    "${paperSize.widthMm.toInt()}x${paperSize.heightMm.toInt()}mm",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(photobook.updatedAt)
                Text(
                    "更新于 $date",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun CreatePhotobookDialog(
    onDismiss: () -> Unit,
    onCreate: (String, PaperSize) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedSize by rememberSaveable { mutableStateOf(PaperSize.A4) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建画册") },
        text = {
            Column {
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
                        TextButton(
                            onClick = { selectedSize = size },
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                "${size.name.replace("_", " ")} (${size.widthMm.toInt()}x${size.heightMm.toInt()})",
                                color = if (size == selectedSize)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(name.takeIf { it.isNotBlank() } ?: "未命名画册", selectedSize) },
                enabled = name.isNotBlank()
            ) {
                Text("下一步")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
