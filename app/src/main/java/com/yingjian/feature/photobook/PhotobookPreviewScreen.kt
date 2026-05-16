package com.yingjian.feature.photobook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yingjian.feature.photobook.model.BookState
import com.yingjian.feature.photobook.model.CoverLayout
import com.yingjian.feature.photobook.model.PageState
import com.yingjian.feature.photobook.model.PhotobookLayoutDefaults
import kotlinx.coroutines.launch

sealed interface PhotobookLeaf {
    data class Cover(val layout: CoverLayout) : PhotobookLeaf
    data class Content(val page: PageState) : PhotobookLeaf
    data class BackCover(val layout: CoverLayout) : PhotobookLeaf
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotobookPreviewScreen(
    bookState: BookState,
    onBack: () -> Unit,
    onShare: () -> Unit
) {
    val leaves = remember(bookState.photobook.id, bookState.pages.size) {
        buildList {
            add(PhotobookLeaf.Cover(bookState.coverLayout))
            bookState.pages.forEach { page ->
                add(PhotobookLeaf.Content(page))
            }
            add(PhotobookLeaf.BackCover(bookState.backCoverLayout))
        }
    }

    val isLandscape = LocalConfiguration.current.screenWidthDp >
            LocalConfiguration.current.screenHeightDp

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF30312F))) {
        if (isLandscape) {
            LandscapeSpreadView(leaves = leaves)
        } else {
            PortraitSinglePageView(leaves = leaves)
        }

        TopAppBar(
            title = { },
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
    }
}

@Composable
private fun PortraitSinglePageView(leaves: List<PhotobookLeaf>) {
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { leaves.size }
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
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 56.dp),
                contentAlignment = Alignment.Center
            ) {
                val pageSize = PhotobookPreviewLayout.fitSinglePage(maxWidth.value, maxHeight.value)
                PreviewLeaf(
                    leaf = leaves[page],
                    pageWidthDp = pageSize.pageWidthDp.dp,
                    modifier = Modifier
                        .size(pageSize.pageWidthDp.dp, pageSize.pageHeightDp.dp)
                        .shadow(10.dp)
                )
            }
        }

        // Page indicator + nav buttons
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val coroutineScope = rememberCoroutineScope()
            IconButton(onClick = {
                if (pagerState.currentPage > 0) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous", tint = Color.White.copy(alpha = 0.6f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(leaves.size) { index ->
                    val dotType = when (leaves[index]) {
                        is PhotobookLeaf.Cover -> LeafPreviewType.Cover
                        is PhotobookLeaf.BackCover -> LeafPreviewType.BackCover
                        is PhotobookLeaf.Content -> LeafPreviewType.Content
                    }
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (index == pagerState.currentPage) 16.dp else 6.dp,
                                height = 6.dp
                            )
                            .background(
                                if (index == pagerState.currentPage) Color.White
                                else when (dotType) {
                                    LeafPreviewType.Cover, LeafPreviewType.BackCover -> Color.White.copy(alpha = 0.6f)
                                    LeafPreviewType.Content -> Color.White.copy(alpha = 0.3f)
                                },
                                CircleShape
                            )
                    )
                }
            }

            IconButton(onClick = {
                if (pagerState.currentPage < leaves.size - 1) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next", tint = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun LandscapeSpreadView(leaves: List<PhotobookLeaf>) {
    val spreadCount = (leaves.size + 1) / 2
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { spreadCount }
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
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 44.dp),
                contentAlignment = Alignment.Center
            ) {
                val spreadSize = PhotobookPreviewLayout.fitSpread(maxWidth.value, maxHeight.value)
                Box(
                    modifier = Modifier
                        .size(spreadSize.spreadWidthDp.dp, spreadSize.spreadHeightDp.dp)
                        .shadow(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val leftIndex = spreadIndex * 2
                        if (leftIndex < leaves.size) {
                            PreviewLeaf(
                                leaf = leaves[leftIndex],
                                pageWidthDp = spreadSize.pageWidthDp.dp,
                                modifier = Modifier
                                    .width(spreadSize.pageWidthDp.dp)
                                    .fillMaxHeight()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .width(spreadSize.pageWidthDp.dp)
                                    .fillMaxHeight()
                            )
                        }

                        val rightIndex = spreadIndex * 2 + 1
                        if (rightIndex < leaves.size) {
                            PreviewLeaf(
                                leaf = leaves[rightIndex],
                                pageWidthDp = spreadSize.pageWidthDp.dp,
                                modifier = Modifier
                                    .width(spreadSize.pageWidthDp.dp)
                                    .fillMaxHeight()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .width(spreadSize.pageWidthDp.dp)
                                    .fillMaxHeight()
                            )
                        }
                    }

                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.18f),
                                    Color.Transparent
                                ),
                                startX = centerX - 24.dp.toPx(),
                                endX = centerX + 24.dp.toPx()
                            ),
                            topLeft = Offset(centerX - 24.dp.toPx(), 0f),
                            size = androidx.compose.ui.geometry.Size(48.dp.toPx(), size.height)
                        )
                        drawLine(
                            Color.White.copy(alpha = 0.28f),
                            Offset(centerX, 0f),
                            Offset(centerX, size.height),
                            1.dp.toPx()
                        )
                    }
                }
            }
        }

        // Spread indicator with nav buttons
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val coroutineScope = rememberCoroutineScope()
            IconButton(onClick = {
                if (pagerState.currentPage > 0) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous", tint = Color.White.copy(alpha = 0.6f))
            }

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

            IconButton(onClick = {
                if (pagerState.currentPage < spreadCount - 1) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next", tint = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun PreviewLeaf(
    leaf: PhotobookLeaf,
    pageWidthDp: Dp,
    modifier: Modifier = Modifier
) {
    when (leaf) {
        is PhotobookLeaf.Cover -> PhotobookStage(
            modifier = modifier,
            maxWidth = pageWidthDp,
            backgroundColor = Color(PhotobookLayoutDefaults.COVER_PAGE_COLOR),
            shadowElevation = 0.dp,
            cornerRadius = 0.dp
        ) { stageScale ->
            CoverPageRenderer(
                layout = leaf.layout,
                scaleFactor = stageScale,
                selectedTextId = null,
                onTextSelected = { },
                onTextMoved = { _, _, _ -> }
            )
        }
        is PhotobookLeaf.Content -> PhotobookStage(
            modifier = modifier,
            maxWidth = pageWidthDp,
            backgroundColor = Color(PhotobookLayoutDefaults.CONTENT_PAGE_COLOR),
            shadowElevation = 0.dp,
            cornerRadius = 0.dp
        ) { stageScale ->
            ContentPageRenderer(
                pageState = leaf.page,
                scaleFactor = stageScale,
                selectedSlotId = null,
                onSlotSelected = { },
                onEmptySlotAddClicked = { },
                onSlotImageAdjusted = { _, _, _, _ -> }
            )
        }
        is PhotobookLeaf.BackCover -> PhotobookStage(
            modifier = modifier,
            maxWidth = pageWidthDp,
            backgroundColor = Color(PhotobookLayoutDefaults.COVER_PAGE_COLOR),
            shadowElevation = 0.dp,
            cornerRadius = 0.dp
        ) { stageScale ->
            CoverPageRenderer(
                layout = leaf.layout,
                scaleFactor = stageScale,
                selectedTextId = null,
                onTextSelected = { },
                onTextMoved = { _, _, _ -> }
            )
        }
    }
}

private enum class LeafPreviewType { Cover, Content, BackCover }
