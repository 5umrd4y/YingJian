package com.yingjian.feature.photobook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yingjian.core.data.database.PhotobookEntity
import com.yingjian.core.ui.theme.PaperTexture

@Composable
fun CoverEditorPage(
    photobook: PhotobookEntity,
    onUpdatePhotobook: (PhotobookEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .fillMaxWidth()
                .aspectRatio(285f / 210f)
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFFAF9F6)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cover image preview (if set)
                photobook.coverImageUri?.let { uri ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(285f / 160f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PaperTexture.copy(alpha = 0.3f))
                    ) {
                        coil3.compose.AsyncImage(
                            model = android.net.Uri.parse(uri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Cover title
                OutlinedTextField(
                    value = photobook.displayCoverTitle(),
                    onValueChange = { newTitle ->
                        onUpdatePhotobook(photobook.copy(coverTitle = newTitle))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.Center
                    ),
                    label = { Text("封面标题") },
                    singleLine = false,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Cover subtitle
                OutlinedTextField(
                    value = photobook.displayCoverSubtitle(),
                    onValueChange = { newSubtitle ->
                        onUpdatePhotobook(photobook.copy(coverSubtitle = newSubtitle))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraLight,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    label = { Text("副标题") },
                    singleLine = true
                )
            }
        }
    }
}

@Composable
fun BackCoverEditorPage(
    photobook: PhotobookEntity,
    onUpdatePhotobook: (PhotobookEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .fillMaxWidth()
                .aspectRatio(285f / 210f)
                .shadow(4.dp, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFFAF9F6)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Back title
                OutlinedTextField(
                    value = photobook.displayBackTitle(),
                    onValueChange = { newTitle ->
                        onUpdatePhotobook(photobook.copy(backTitle = newTitle))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.Center
                    ),
                    label = { Text("封底标题") },
                    singleLine = false,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Back subtitle
                OutlinedTextField(
                    value = photobook.displayBackSubtitle(),
                    onValueChange = { newSubtitle ->
                        onUpdatePhotobook(photobook.copy(backSubtitle = newSubtitle))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraLight,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    label = { Text("封底副标题") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Back date text
                OutlinedTextField(
                    value = photobook.displayBackDateText(),
                    onValueChange = { newDateText ->
                        onUpdatePhotobook(photobook.copy(backDateText = newDateText))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraLight,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    ),
                    label = { Text("日期文字（可选）") },
                    singleLine = true
                )
            }
        }
    }
}
