package com.yingjian.feature.photobook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yingjian.feature.photobook.model.BookState
import kotlinx.coroutines.launch

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
                PhotobookCanvasPage(
                    pageState = bookState.pages[page],
                    containerWidthDp = 200.dp,
                    isSelected = false,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .shadow(4.dp, RoundedCornerShape(2.dp))
                        .clip(RoundedCornerShape(2.dp))
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
private fun LandscapeSpreadView(bookState: BookState) {
    val pagerState = rememberPagerState(
        initialPage = bookState.currentPage / 2,
        pageCount = { (bookState.pages.size + 1) / 2 }
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
                        PhotobookCanvasPage(
                            pageState = bookState.pages[leftPageIndex],
                            containerWidthDp = 150.dp,
                            isSelected = false,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight())
                    }

                    // Spine
                    Box(
                        modifier = Modifier.width(40.dp).fillMaxHeight()
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
                        PhotobookCanvasPage(
                            pageState = bookState.pages[rightPageIndex],
                            containerWidthDp = 150.dp,
                            isSelected = false,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight())
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
            val spreadCount = (bookState.pages.size + 1) / 2
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
