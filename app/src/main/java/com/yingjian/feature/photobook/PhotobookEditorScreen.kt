package com.yingjian.feature.photobook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yingjian.feature.photobook.model.BookState
import com.yingjian.feature.photobook.model.LayoutMode
import com.yingjian.feature.photobook.model.PaperSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotobookEditorScreen(
    bookState: BookState,
    onUpdateState: (BookState) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onExportPdf: () -> Unit,
    onUndo: () -> Unit
) {
    var zoomLevel by remember { mutableFloatStateOf(1f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(bookState.photobook.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onSave) {
                        Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.padding(end = 4.dp))
                        Text("Save")
                    }
                    IconButton(onClick = onExportPdf) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            HorizontalPager(
                state = rememberPagerState(
                    initialPage = bookState.currentPage,
                    pageCount = { bookState.pages.size }
                ),
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageState = bookState.pages[page]
                PhotobookCanvasPage(
                    pageState = pageState,
                    containerWidthDp = 300.dp,
                    zoomLevel = zoomLevel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }

            // Bottom controls: paper size selector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                PaperSizeSelector(
                    currentSize = runCatching { PaperSize.valueOf(bookState.photobook.paperSize) }.getOrDefault(PaperSize.A4),
                    onSizeChange = { /* Handle size change */ }
                )
            }

            // Zoom controls (bottom right)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Column {
                    IconButton(onClick = { zoomLevel = (zoomLevel + 0.2f).coerceAtMost(3f) }) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom in")
                    }
                    IconButton(onClick = { zoomLevel = (zoomLevel - 0.2f).coerceAtLeast(0.5f) }) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom out")
                    }
                }
            }
        }
    }
}

@Composable
fun PaperSizeSelector(
    currentSize: PaperSize,
    onSizeChange: (PaperSize) -> Unit
) {
    Row {
        Text(
            "Canvas size",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.CenterVertically).padding(end = 8.dp)
        )
        listOf(PaperSize.A4, PaperSize.SIX_INCH_LANDSCAPE, PaperSize.SQUARE).forEach { size ->
            FilterChip(
                selected = size == currentSize,
                onClick = { onSizeChange(size) },
                label = { Text(size.name.replace("_", " ")) },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
