package com.yingjian.feature.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.yingjian.core.data.repository.StorageProvider

@androidx.compose.runtime.Composable
fun SettingsScreen(
    localStorageProvider: StorageProvider
) {
    Scaffold { paddingValues ->
        StorageListScreen(
            modifier = Modifier.padding(paddingValues),
            localStorageProvider = localStorageProvider
        )
    }
}
