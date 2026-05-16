package com.yingjian.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yingjian.core.data.repository.StorageProvider

data class StorageEntry(
    val key: String,
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val configured: Boolean,
    val provider: StorageProvider
)

@Composable
fun StorageListScreen(
    modifier: Modifier = Modifier,
    localStorageProvider: StorageProvider
) {
    var showConfigSheet by remember { mutableStateOf<String?>(null) }

    val storageEntries = listOf(
        StorageEntry(
            key = "local",
            displayName = "本地存储",
            icon = Icons.Default.Computer,
            configured = true,
            provider = localStorageProvider
        ),
        StorageEntry(
            key = "s3",
            displayName = "Amazon S3",
            icon = Icons.Default.Cloud,
            configured = false,
            provider = com.yingjian.core.data.repository.S3StorageProvider()
        ),
        StorageEntry(
            key = "smb",
            displayName = "SMB",
            icon = Icons.Default.Folder,
            configured = false,
            provider = com.yingjian.core.data.repository.SMBStorageProvider()
        ),
        StorageEntry(
            key = "ftp",
            displayName = "FTP",
            icon = Icons.Default.CloudDone,
            configured = false,
            provider = com.yingjian.core.data.repository.FTPStorageProvider()
        )
    )

    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        items(storageEntries) { entry ->
            ListItem(
                headlineContent = { Text(entry.displayName) },
                supportingContent = {
                    Text(if (entry.configured) "已连接" else "未配置")
                },
                leadingContent = { Icon(entry.icon, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (entry.key == "local") Modifier else Modifier.clickable { showConfigSheet = entry.key })
            )
        }
    }

    showConfigSheet?.let { key ->
        val entry = storageEntries.find { it.key == key }
        if (entry != null) {
            StorageConfigSheet(
                storageEntry = entry,
                onDismiss = { showConfigSheet = null },
                onSave = { showConfigSheet = null }
            )
        }
    }
}
