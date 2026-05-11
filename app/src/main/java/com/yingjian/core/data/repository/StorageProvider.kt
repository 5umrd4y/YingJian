package com.yingjian.core.data.repository

import android.content.Context
import android.net.Uri
import java.io.File

interface StorageProvider {
    val name: String
    suspend fun savePdf(fileName: String, data: ByteArray): Uri
    suspend fun listPdfFiles(): List<Uri>
    suspend fun testConnection(): Result<Unit>
}

class LocalStorageProvider(private val context: Context) : StorageProvider {
    override val name: String = "本地存储"

    private val storageDir: File
        get() = context.getExternalFilesDir(null)?.resolve("photobooks")
            ?: context.filesDir.resolve("photobooks")

    override suspend fun savePdf(fileName: String, data: ByteArray): Uri {
        val dir = storageDir.also { it.mkdirs() }
        val file = File(dir, fileName)
        file.writeBytes(data)
        return Uri.fromFile(file)
    }

    override suspend fun listPdfFiles(): List<Uri> {
        val dir = storageDir
        if (!dir.exists()) return emptyList()
        return dir.listFiles { f -> f.extension == "pdf" }
            ?.map { Uri.fromFile(it) }
            ?: emptyList()
    }

    override suspend fun testConnection(): Result<Unit> =
        runCatching { storageDir.mkdirs() }
}

class S3StorageProvider : StorageProvider {
    override val name = "Amazon S3"
    override suspend fun savePdf(fileName: String, data: ByteArray): Uri = throw NotImplementedError("S3 not implemented in MVP")
    override suspend fun listPdfFiles(): List<Uri> = emptyList()
    override suspend fun testConnection(): Result<Unit> = Result.failure(NotImplementedError("S3 not implemented in MVP"))
}

class SMBStorageProvider : StorageProvider {
    override val name = "SMB"
    override suspend fun savePdf(fileName: String, data: ByteArray): Uri = throw NotImplementedError("SMB not implemented in MVP")
    override suspend fun listPdfFiles(): List<Uri> = emptyList()
    override suspend fun testConnection(): Result<Unit> = Result.failure(NotImplementedError("SMB not implemented in MVP"))
}

class FTPStorageProvider : StorageProvider {
    override val name = "FTP"
    override suspend fun savePdf(fileName: String, data: ByteArray): Uri = throw NotImplementedError("FTP not implemented in MVP")
    override suspend fun listPdfFiles(): List<Uri> = emptyList()
    override suspend fun testConnection(): Result<Unit> = Result.failure(NotImplementedError("FTP not implemented in MVP"))
}
