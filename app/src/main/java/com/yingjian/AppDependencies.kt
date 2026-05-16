package com.yingjian

import android.content.Context
import androidx.room.Room
import com.yingjian.core.data.database.YingJianDatabase
import com.yingjian.core.data.repository.LocalStorageProvider
import com.yingjian.core.data.repository.MemoryRepository
import com.yingjian.core.data.repository.MemoryRepositoryImpl
import com.yingjian.core.data.repository.PhotobookRepository
import com.yingjian.core.data.repository.PhotobookRepositoryImpl
import com.yingjian.core.data.repository.StorageProvider

class AppDependencies(context: Context) {
    private val database: YingJianDatabase = Room.databaseBuilder(
        context.applicationContext,
        YingJianDatabase::class.java,
        YingJianDatabase.DATABASE_NAME
    ).fallbackToDestructiveMigration().build()

    val memoryRepository: MemoryRepository by lazy {
        MemoryRepositoryImpl(database.memoryDao())
    }

    val photobookRepository: PhotobookRepository by lazy {
        PhotobookRepositoryImpl(
            photobookDao = database.photobookDao(),
            pageLayoutDao = database.pageLayoutDao()
        )
    }

    val localStorageProvider: StorageProvider by lazy {
        LocalStorageProvider(context.applicationContext)
    }
}
