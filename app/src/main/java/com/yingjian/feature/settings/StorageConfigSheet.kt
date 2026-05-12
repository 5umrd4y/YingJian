package com.yingjian.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class StorageConfig(
    var server: String = "",
    var port: String = "",
    var account: String = "",
    var password: String = "",
    var path: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageConfigSheet(
    storageEntry: StorageEntry,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var config by remember { mutableStateOf(StorageConfig()) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Result<Unit>?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                "配置 ${storageEntry.displayName}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = config.server,
                onValueChange = { config = config.copy(server = it) },
                label = { Text("服务器") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = config.port,
                onValueChange = { config = config.copy(port = it) },
                label = { Text("端口") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = config.account,
                onValueChange = { config = config.copy(account = it) },
                label = { Text("账号") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = config.password,
                onValueChange = { config = config.copy(password = it) },
                label = { Text("密码") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = config.path,
                onValueChange = { config = config.copy(path = it) },
                label = { Text("路径") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            isTesting = true
                            testResult = storageEntry.provider.testConnection()
                            isTesting = false
                        }
                    },
                    enabled = !isTesting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text("测试连接")
                }

                testResult?.let { result ->
                    Text(
                        text = if (result.isSuccess) "连接成功" else "连接失败: ${result.exceptionOrNull()?.message}",
                        color = if (result.isSuccess)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
                Button(
                    onClick = {
                        // Save configuration (MVP: not persisted)
                        onSave()
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("保存")
                }
            }
        }
    }
}
