package com.yingjian.feature.photobook

import androidx.compose.runtime.Composable

@Composable
fun PhotobookScreen(
    viewModel: PhotobookViewModel,
    onNavigateToPhotoPicker: (String) -> Unit = {},
    onNavigateToEditor: (Long) -> Unit = {}
) {
    PhotobookListScreen(
        viewModel = viewModel,
        onNavigateToPhotoPicker = onNavigateToPhotoPicker,
        onNavigateToEditor = onNavigateToEditor
    )
}
