package com.yingjian.core.data.repository

import com.yingjian.core.data.database.MemoryDao
import com.yingjian.core.data.database.MemoryRecordEntity

interface MemoryRepository {
    suspend fun insertMemory(memory: MemoryRecordEntity): Long
    suspend fun updateMemory(memory: MemoryRecordEntity)
    suspend fun deleteMemory(memory: MemoryRecordEntity)
    suspend fun getAllMemories(): List<MemoryRecordEntity>
    suspend fun getMemoryById(id: Long): MemoryRecordEntity?
    suspend fun getMemoriesByIds(ids: List<Long>): List<MemoryRecordEntity>
    suspend fun getPaginatedMemories(limit: Int, offset: Int = 0): List<MemoryRecordEntity>
    suspend fun getMemoryCount(): Int
}

class MemoryRepositoryImpl(private val memoryDao: MemoryDao) : MemoryRepository {
    override suspend fun insertMemory(memory: MemoryRecordEntity): Long = memoryDao.insert(memory)
    override suspend fun updateMemory(memory: MemoryRecordEntity) = memoryDao.update(memory)
    override suspend fun deleteMemory(memory: MemoryRecordEntity) = memoryDao.delete(memory)
    override suspend fun getAllMemories(): List<MemoryRecordEntity> = memoryDao.getAll()
    override suspend fun getMemoryById(id: Long): MemoryRecordEntity? = memoryDao.getById(id)
    override suspend fun getMemoriesByIds(ids: List<Long>): List<MemoryRecordEntity> = memoryDao.getByIds(ids)
    override suspend fun getPaginatedMemories(limit: Int, offset: Int): List<MemoryRecordEntity> = memoryDao.getPaginated(limit, offset)
    override suspend fun getMemoryCount(): Int = memoryDao.count()
}
