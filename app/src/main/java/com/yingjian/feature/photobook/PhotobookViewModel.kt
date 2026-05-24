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
import com.yingjian.core.util.PageLayoutDocumentSerializer
import com.yingjian.feature.photobook.layout.AutoLayoutAlgorithm
import com.yingjian.feature.photobook.model.BookState
import com.yingjian.feature.photobook.model.ImageRef
import com.yingjian.feature.photobook.model.LayoutMode
import com.yingjian.feature.photobook.model.PaperSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PhotobookUiState(
    val photobooks: List<PhotobookEntity> = emptyList(),
    val pageCounts: Map<Long, Int> = emptyMap(),
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
            val counts = mutableMapOf<Long, Int>()
            books.forEach { book ->
                withContext(Dispatchers.IO) {
                    counts[book.id] = photobookRepository.getPageCount(book.id)
                }
            }
            uiState = uiState.copy(photobooks = books, pageCounts = counts)
        }
    }

    fun createPhotobookFromPhotos(name: String, selectedPhotos: List<SelectedMemoryPhoto>) {
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
            val memoryById = withContext(Dispatchers.IO) {
                selectedPhotos.mapNotNull { photo ->
                    memoryRepository.getMemoryById(photo.memoryId)?.let { photo.memoryId to it }
                }.toMap()
            }
            val orderedPhotos = PhotobookPhotoOrdering.byMemoryDateAscending(selectedPhotos, memoryById)
            val pages = orderedPhotos.mapIndexed { index, photo ->
                val imageRef = ImageRef(
                    memoryId = photo.memoryId,
                    imageUri = photo.imageUri,
                    sourceImageIndex = photo.sourceImageIndex,
                    sourceImageId = photo.sourceImageId,
                    imageWidth = photo.imageWidth,
                    imageHeight = photo.imageHeight
                )
                AutoLayoutAlgorithm.createSinglePhotoPage(
                    imageRef = imageRef,
                    moodText = memoryById[photo.memoryId]?.moodText,
                    paperSize = paperSize,
                    pageNumber = index + 1,
                    imageWidth = photo.imageWidth,
                    imageHeight = photo.imageHeight
                )
            }
            val bookState = BookState(
                photobook = photobook.copy(id = bookId),
                pages = pages,
                currentPage = 0,
                mode = LayoutMode.AUTO
            )
            withContext(Dispatchers.IO) {
                pages.forEach { page ->
                    photobookRepository.savePageLayout(
                        PageLayoutEntity(
                            photobookId = bookId,
                            pageNumber = page.pageNumber,
                            elementsJson = PageLayoutDocumentSerializer.serialize(page.toDocument()),
                            mode = bookState.mode.name
                        )
                    )
                }
            }
            uiState = uiState.copy(currentBookState = bookState, isLoading = false)
            loadPhotobooks()
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

    fun appendPhotosToBook(selectedPhotos: List<SelectedMemoryPhoto>) {
        viewModelScope.launch {
            val currentState = uiState.currentBookState ?: return@launch
            uiState = uiState.copy(isLoading = true)
            val paperSize = runCatching {
                PaperSize.valueOf(currentState.photobook.paperSize)
            }.getOrDefault(PaperSize.TWELVE_INCH_LANDSCAPE)
            val memoryById = withContext(Dispatchers.IO) {
                selectedPhotos.mapNotNull { photo ->
                    memoryRepository.getMemoryById(photo.memoryId)?.let { photo.memoryId to it }
                }.toMap()
            }
            val startPageNumber = currentState.pages.size + 1
            val orderedPhotos = PhotobookPhotoOrdering.byMemoryDateAscending(selectedPhotos, memoryById)
            val newPages = orderedPhotos.mapIndexed { index, photo ->
                val imageRef = ImageRef(
                    memoryId = photo.memoryId,
                    imageUri = photo.imageUri,
                    sourceImageIndex = photo.sourceImageIndex,
                    sourceImageId = photo.sourceImageId,
                    imageWidth = photo.imageWidth,
                    imageHeight = photo.imageHeight
                )
                AutoLayoutAlgorithm.createSinglePhotoPage(
                    imageRef = imageRef,
                    moodText = memoryById[photo.memoryId]?.moodText,
                    paperSize = paperSize,
                    pageNumber = startPageNumber + index,
                    imageWidth = photo.imageWidth,
                    imageHeight = photo.imageHeight
                )
            }
            val updatedState = currentState.copy(
                pages = currentState.pages + newPages,
                currentPage = currentState.pages.size
            )
            withContext(Dispatchers.IO) {
                newPages.forEach { page ->
                    photobookRepository.savePageLayout(
                        PageLayoutEntity(
                            photobookId = currentState.photobook.id,
                            pageNumber = page.pageNumber,
                            elementsJson = PageLayoutDocumentSerializer.serialize(page.toDocument()),
                            mode = currentState.mode.name
                        )
                    )
                }
            }
            uiState = uiState.copy(currentBookState = updatedState, isLoading = false)
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
