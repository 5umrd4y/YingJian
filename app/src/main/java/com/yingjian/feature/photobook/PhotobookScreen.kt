package com.yingjian.feature.photobook

import androidx.compose.runtime.Composable
import com.yingjian.feature.photobook.model.PaperSize

@Composable
fun PhotobookScreen(
    viewModel: PhotobookViewModel,
    onNavigateToPhotoPicker: (PaperSize) -> Unit = {},
    onNavigateToEditor: (Long) -> Unit = {}
) {
    PhotobookListScreen(
        viewModel = viewModel,
        onNavigateToPhotoPicker = onNavigateToPhotoPicker,
        onNavigateToEditor = onNavigateToEditor
    )
}
