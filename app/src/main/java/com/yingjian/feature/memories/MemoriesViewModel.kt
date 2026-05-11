package com.yingjian.feature.memories

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.yingjian.core.data.database.MemoryRecordEntity
import com.yingjian.core.data.repository.MemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

data class MemoriesUiState(
    val memories: List<MemoryRecordEntity> = emptyList(),
    val isLoading: Boolean = false
)

sealed class MemoriesAction {
    data object Load : MemoriesAction()
    data class Add(
        val uri: Uri, val mood: String?, val tags: List<String>,
        val imageWidth: Int, val imageHeight: Int
    ) : MemoriesAction()
}

class MemoriesViewModel(
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    var uiState by mutableStateOf(MemoriesUiState())
        private set

    init {
        dispatch(MemoriesAction.Load)
    }

    fun dispatch(action: MemoriesAction) {
        when (action) {
            is MemoriesAction.Load -> loadMemories()
            is MemoriesAction.Add -> addMemory(action.uri, action.mood, action.tags, action.imageWidth, action.imageHeight)
        }
    }

    private fun loadMemories() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            val memories = withContext(Dispatchers.IO) {
                memoryRepository.getAllMemories()
            }
            uiState = uiState.copy(memories = memories, isLoading = false)
        }
    }

    private fun addMemory(imageUri: Uri, moodText: String?, tags: List<String>, imageWidth: Int, imageHeight: Int) {
        viewModelScope.launch {
            val memory = MemoryRecordEntity(
                imageUri = imageUri.toString(),
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                timestamp = System.currentTimeMillis(),
                latitude = null,
                longitude = null,
                moodText = moodText?.takeIf { it.isNotBlank() },
                tags = Json.encodeToString(ListSerializer(String.serializer()), tags),
                createdAt = System.currentTimeMillis()
            )
            withContext(Dispatchers.IO) {
                memoryRepository.insertMemory(memory)
            }
            loadMemories()
        }
    }

    companion object {
        fun factory(repository: MemoryRepository) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return MemoriesViewModel(repository) as T
            }
        }
    }
}
