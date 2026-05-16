package com.yingjian.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.yingjian.core.data.repository.StorageProvider

private enum class SettingsSection {
    Main,
    Storage
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    localStorageProvider: StorageProvider,
    onNavigateToAbout: () -> Unit = {}
) {
    var currentSection by remember { mutableStateOf(SettingsSection.Main) }

    BackHandler(enabled = currentSection != SettingsSection.Main) {
        currentSection = SettingsSection.Main
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentSection) {
                            SettingsSection.Main -> "设置"
                            SettingsSection.Storage -> "存储设置"
                        }
                    )
                },
                navigationIcon = {
                    if (currentSection != SettingsSection.Main) {
                        IconButton(onClick = { currentSection = SettingsSection.Main }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when (currentSection) {
            SettingsSection.Main -> SettingsHomeScreen(
                modifier = Modifier.padding(paddingValues),
                onStorageClicked = { currentSection = SettingsSection.Storage },
                onAboutClicked = onNavigateToAbout
            )
            SettingsSection.Storage -> StorageListScreen(
                modifier = Modifier.padding(paddingValues),
                localStorageProvider = localStorageProvider
            )
        }
    }
}

@Composable
private fun SettingsHomeScreen(
    modifier: Modifier = Modifier,
    onStorageClicked: () -> Unit,
    onAboutClicked: () -> Unit
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            ListItem(
                headlineContent = { Text("存储设置") },
                supportingContent = { Text("配置本地、S3、SMB、FTP 存储") },
                leadingContent = { Icon(Icons.Default.Storage, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStorageClicked() }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("关于") },
                leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAboutClicked() }
            )
        }
    }
}
