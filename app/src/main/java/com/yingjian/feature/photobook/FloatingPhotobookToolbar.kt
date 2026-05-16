package com.yingjian.feature.photobook

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class ToolbarAction { Layout, Text, Move, Delete, AddPage }

@Composable
fun FloatingPhotobookToolbar(
    onLayoutClicked: () -> Unit,
    onTextClicked: () -> Unit,
    onMoveClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onAddPageClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f), RoundedCornerShape(24.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconButton(onClick = onLayoutClicked, modifier = Modifier.size(44.dp)) { LayoutTemplateIcon() }
        IconButton(onClick = onTextClicked, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.TextFields, contentDescription = "文本") }
        IconButton(onClick = onMoveClicked, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.DriveFileMove, contentDescription = "移动") }
        IconButton(onClick = onDeleteClicked, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.Delete, contentDescription = "删除") }
        IconButton(onClick = onAddPageClicked, modifier = Modifier.size(44.dp)) { Icon(Icons.Default.Add, contentDescription = "添加页面") }
    }
}

@Composable
private fun LayoutTemplateIcon() {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = Modifier.size(24.dp)) {
        drawRoundRect(color, Offset(3.dp.toPx(), 3.dp.toPx()), Size(10.dp.toPx(), 18.dp.toPx()), CornerRadius(2.dp.toPx()))
        drawRoundRect(color, Offset(15.dp.toPx(), 3.dp.toPx()), Size(6.dp.toPx(), 8.dp.toPx()), CornerRadius(1.5.dp.toPx()))
        drawRoundRect(color, Offset(15.dp.toPx(), 13.dp.toPx()), Size(6.dp.toPx(), 8.dp.toPx()), CornerRadius(1.5.dp.toPx()))
    }
}
