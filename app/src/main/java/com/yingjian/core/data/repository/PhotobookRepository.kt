package com.yingjian.core.data.repository

import com.yingjian.core.data.database.PageLayoutDao
import com.yingjian.core.data.database.PageLayoutEntity
import com.yingjian.core.data.database.PhotobookDao
import com.yingjian.core.data.database.PhotobookEntity
import com.yingjian.core.data.database.YingJianDatabase
import androidx.room.withTransaction

interface PhotobookRepository {
    suspend fun createPhotobook(photobook: PhotobookEntity): Long
    suspend fun updatePhotobook(photobook: PhotobookEntity)
    suspend fun deletePhotobook(photobook: PhotobookEntity)
    suspend fun getAllPhotobooks(): List<PhotobookEntity>
    suspend fun getPhotobookById(id: Long): PhotobookEntity?
    suspend fun getPageLayouts(photobookId: Long): List<PageLayoutEntity>
    suspend fun savePageLayout(pageLayout: PageLayoutEntity): Long
    suspend fun replacePageLayoutsAndUpdatePhotobook(
        photobook: PhotobookEntity,
        pageLayouts: List<PageLayoutEntity>
    )
    suspend fun deletePageLayouts(photobookId: Long)
    suspend fun getPageCount(photobookId: Long): Int
    suspend fun getPageCountsByPhotobook(): Map<Long, Int>
    suspend fun deletePhotobooks(ids: List<Long>)
}

class PhotobookRepositoryImpl(
    private val database: YingJianDatabase,
    private val photobookDao: PhotobookDao,
    private val pageLayoutDao: PageLayoutDao
) : PhotobookRepository {
    override suspend fun createPhotobook(photobook: PhotobookEntity): Long = photobookDao.insert(photobook)
    override suspend fun updatePhotobook(photobook: PhotobookEntity) = photobookDao.update(photobook)
    override suspend fun deletePhotobook(photobook: PhotobookEntity) = photobookDao.delete(photobook)
    override suspend fun getAllPhotobooks(): List<PhotobookEntity> = photobookDao.getAll()
    override suspend fun getPhotobookById(id: Long): PhotobookEntity? = photobookDao.getById(id)
    override suspend fun getPageLayouts(photobookId: Long): List<PageLayoutEntity> = pageLayoutDao.getByPhotobookId(photobookId)
    override suspend fun savePageLayout(pageLayout: PageLayoutEntity): Long = pageLayoutDao.insert(pageLayout)
    override suspend fun replacePageLayoutsAndUpdatePhotobook(
        photobook: PhotobookEntity,
        pageLayouts: List<PageLayoutEntity>
    ) {
        database.withTransaction {
            pageLayoutDao.deleteByPhotobookId(photobook.id)
            pageLayouts.forEach { pageLayoutDao.insert(it) }
            photobookDao.update(photobook)
        }
    }
    override suspend fun deletePageLayouts(photobookId: Long) = pageLayoutDao.deleteByPhotobookId(photobookId)
    override suspend fun getPageCount(photobookId: Long): Int = pageLayoutDao.countByPhotobookId(photobookId)
    override suspend fun getPageCountsByPhotobook(): Map<Long, Int> =
        pageLayoutDao.countByPhotobookIds().associate { it.photobookId to it.pageCount }

    override suspend fun deletePhotobooks(ids: List<Long>) {
        ids.forEach { id ->
            pageLayoutDao.deleteByPhotobookId(id)
        }
        photobookDao.deleteByIds(ids)
    }
}
