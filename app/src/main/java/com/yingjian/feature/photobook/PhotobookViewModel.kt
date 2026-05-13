package com.yingjian.feature.photobook

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.yingjian.core.data.database.PageLayoutEntity
import com.yingjian.core.data.database.PhotobookEntity
import com.yingjian.core.data.repository.MemoryRepository
import com.yingjian.core.data.repository.PhotobookRepository
import com.yingjian.core.util.ElementSerializer
import com.yingjian.feature.photobook.layout.AutoLayoutAlgorithm
import com.yingjian.feature.photobook.model.BookState
import com.yingjian.feature.photobook.model.LayoutMode
import com.yingjian.feature.photobook.model.PaperSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PhotobookUiState(
    val photobooks: List<PhotobookEntity> = emptyList(),
    val currentBookState: BookState? = null,
    val isLoading: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
)

class PhotobookViewModel(
    private val photobookRepository: PhotobookRepository,
    private val memoryRepository: MemoryRepository
) : ViewModel() {

    var uiState by mutableStateOf(PhotobookUiState())
        private set

    init {
        loadPhotobooks()
    }

    fun loadPhotobooks() {
        viewModelScope.launch {
            val books = withContext(Dispatchers.IO) {
                photobookRepository.getAllPhotobooks()
            }
            uiState = uiState.copy(photobooks = books)
        }
    }

    fun createPhotobook(name: String, selectedMemoryIds: List<Long>) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)

            val paperSize = PaperSize.TWELVE_INCH_LANDSCAPE
            val photobook = PhotobookEntity(
                name = name,
                paperSize = paperSize.name,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val bookId = withContext(Dispatchers.IO) {
                photobookRepository.createPhotobook(photobook)
            }

            val memories = withContext(Dispatchers.IO) {
                memoryRepository.getMemoriesByIds(selectedMemoryIds)
            }

            val bookState = AutoLayoutAlgorithm.layout(
                memories = memories,
                paperSize = paperSize,
                photobook = photobook.copy(id = bookId)
            )

            withContext(Dispatchers.IO) {
                bookState.pages.forEach { page ->
                    photobookRepository.savePageLayout(
                        PageLayoutEntity(
                            photobookId = bookId,
                            pageNumber = page.pageNumber,
                            elementsJson = ElementSerializer.serialize(page.elements),
                            mode = bookState.mode.name
                        )
                    )
                }
            }

            uiState = uiState.copy(
                currentBookState = bookState,
                isLoading = false
            )
        }
    }

    fun loadPhotobook(photobookId: Long) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true)
            val photobook = withContext(Dispatchers.IO) {
                photobookRepository.getPhotobookById(photobookId)
            }
            val pages = withContext(Dispatchers.IO) {
                photobookRepository.getPageLayouts(photobookId)
            }
            // MVP: simplified reconstruction
            uiState = uiState.copy(isLoading = false)
        }
    }

    fun updateBookState(newState: BookState) {
        uiState = uiState.copy(currentBookState = newState)
    }

    fun switchToManualMode() {
        val currentState = uiState.currentBookState ?: return
        uiState = uiState.copy(
            currentBookState = currentState.copy(
                previousManualState = null,
                mode = LayoutMode.MANUAL
            )
        )
    }

    fun switchToAutoMode() {
        val currentState = uiState.currentBookState ?: return
        val stateWithUndo = currentState.copy(
            previousManualState = currentState,
            mode = LayoutMode.AUTO
        )
        uiState = uiState.copy(currentBookState = stateWithUndo)
    }

    fun undo() {
        val currentState = uiState.currentBookState ?: return
        currentState.previousManualState?.let { manualState ->
            uiState = uiState.copy(
                currentBookState = manualState.copy(previousManualState = null)
            )
        }
    }

    fun deletePhotobook(photobook: PhotobookEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                photobookRepository.deletePageLayouts(photobook.id)
                photobookRepository.deletePhotobook(photobook)
            }
            loadPhotobooks()
        }
    }

    fun enterSelectionMode(id: Long) {
        uiState = uiState.copy(
            isSelectionMode = true,
            selectedIds = setOf(id)
        )
    }

    fun exitSelectionMode() {
        uiState = uiState.copy(
            isSelectionMode = false,
            selectedIds = emptySet()
        )
    }

    fun toggleSelection(id: Long) {
        val current = uiState.selectedIds
        val updated = if (id in current) current - id else current + id
        if (updated.isEmpty()) {
            exitSelectionMode()
        } else {
            uiState = uiState.copy(selectedIds = updated)
        }
    }

    fun deletePhotobooks(ids: List<Long>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                photobookRepository.deletePhotobooks(ids)
            }
            exitSelectionMode()
            loadPhotobooks()
        }
    }

    fun appendPhotosToBook(memoryIds: List<Long>) {
        viewModelScope.launch {
            val currentState = uiState.currentBookState ?: return@launch
            uiState = uiState.copy(isLoading = true)

            val memories = withContext(Dispatchers.IO) {
                memoryRepository.getMemoriesByIds(memoryIds)
            }

            val paperSize = runCatching {
                PaperSize.valueOf(currentState.photobook.paperSize)
            }.getOrDefault(PaperSize.TWELVE_INCH_LANDSCAPE)

            val startPageNumber = currentState.pages.size + 1
            val newPages = memories.mapIndexed { index, memory ->
                AutoLayoutAlgorithm.createSinglePhotoPage(
                    memory = memory,
                    paperSize = paperSize,
                    pageNumber = startPageNumber + index
                )
            }

            val updatedState = currentState.copy(
                pages = currentState.pages + newPages,
                currentPage = currentState.pages.size
            )
            uiState = uiState.copy(
                currentBookState = updatedState,
                isLoading = false
            )
        }
    }

    fun setCoverImage(uri: String) {
        val currentState = uiState.currentBookState ?: return
        val updatedPhotobook = currentState.photobook.copy(
            coverImageUri = uri,
            updatedAt = System.currentTimeMillis()
        )
        uiState = uiState.copy(
            currentBookState = currentState.copy(photobook = updatedPhotobook)
        )
    }

    companion object {
        fun factory(
            photobookRepository: PhotobookRepository,
            memoryRepository: MemoryRepository
        ) = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return PhotobookViewModel(photobookRepository, memoryRepository) as T
            }
        }
    }
}
