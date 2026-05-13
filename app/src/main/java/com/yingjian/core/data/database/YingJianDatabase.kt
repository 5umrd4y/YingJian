package com.yingjian.core.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Update

// --- MemoryRecord Entity ---
@Entity(tableName = "memory_record")
data class MemoryRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imageUri: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val moodText: String?,
    val tags: String,
    val createdAt: Long,
    @Deprecated("Derived from imageUrisJson.length, not written")
    @androidx.room.ColumnInfo(name = "photo_count", defaultValue = "1")
    val photoCount: Int = 1,
    @androidx.room.ColumnInfo(name = "image_uris_json", defaultValue = "[]")
    val imageUrisJson: String = "[]"
)

@Dao
interface MemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryRecordEntity): Long

    @Update
    suspend fun update(memory: MemoryRecordEntity)

    @Delete
    suspend fun delete(memory: MemoryRecordEntity)

    @Query("SELECT * FROM memory_record ORDER BY timestamp DESC")
    suspend fun getAll(): List<MemoryRecordEntity>

    @Query("SELECT * FROM memory_record WHERE id = :id")
    suspend fun getById(id: Long): MemoryRecordEntity?

    @Query("SELECT * FROM memory_record WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<Long>): List<MemoryRecordEntity>

    @Query("SELECT * FROM memory_record ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    suspend fun getPaginated(limit: Int, offset: Int): List<MemoryRecordEntity>

    @Query("SELECT COUNT(*) FROM memory_record")
    suspend fun count(): Int
}

// --- Photobook Entity ---
@Entity(tableName = "photobook")
data class PhotobookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val paperSize: String,
    val coverImageUri: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

@Dao
interface PhotobookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photobook: PhotobookEntity): Long

    @Update
    suspend fun update(photobook: PhotobookEntity)

    @Delete
    suspend fun delete(photobook: PhotobookEntity)

    @Query("SELECT * FROM photobook ORDER BY updatedAt DESC")
    suspend fun getAll(): List<PhotobookEntity>

    @Query("SELECT * FROM photobook WHERE id = :id")
    suspend fun getById(id: Long): PhotobookEntity?

    @Query("DELETE FROM photobook WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}

// --- PageLayout Entity ---
@Entity(tableName = "page_layout")
data class PageLayoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val photobookId: Long,
    val pageNumber: Int,
    val elementsJson: String,
    val mode: String
)

@Dao
interface PageLayoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pageLayout: PageLayoutEntity): Long

    @Query("SELECT * FROM page_layout WHERE photobookId = :photobookId ORDER BY pageNumber")
    suspend fun getByPhotobookId(photobookId: Long): List<PageLayoutEntity>

    @Query("DELETE FROM page_layout WHERE photobookId = :photobookId")
    suspend fun deleteByPhotobookId(photobookId: Long)

    @Query("SELECT COUNT(*) FROM page_layout WHERE photobookId = :photobookId")
    suspend fun countByPhotobookId(photobookId: Long): Int
}

// --- Database ---
@TypeConverters(Converters::class)
@androidx.room.Database(
    entities = [MemoryRecordEntity::class, PhotobookEntity::class, PageLayoutEntity::class],
    version = 4,
    autoMigrations = [
        androidx.room.AutoMigration(from = 1, to = 2),
        androidx.room.AutoMigration(from = 2, to = 3),
        androidx.room.AutoMigration(from = 3, to = 4)
    ],
    exportSchema = true
)
abstract class YingJianDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun photobookDao(): PhotobookDao
    abstract fun pageLayoutDao(): PageLayoutDao

    companion object {
        const val DATABASE_NAME = "yingjian.db"
    }
}
