package com.yingjian.core.media

import java.io.File
import java.io.InputStream
import java.util.UUID

class ImageArchiveStore(
    rootDir: File,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val idFactory: () -> String = { UUID.randomUUID().toString() }
) {
    private val archiveDir: File = rootDir.resolve(ARCHIVE_DIR_NAME)

    fun archive(
        displayName: String?,
        mimeType: String?,
        openInputStream: () -> InputStream
    ): File {
        archiveDir.mkdirs()
        val extension = extensionFor(displayName, mimeType)
        val target = uniqueTargetFile(extension)
        val temp = File(target.parentFile, "${target.name}.tmp")
        openInputStream().use { input ->
            temp.outputStream().use { output -> input.copyTo(output) }
        }
        if (target.exists()) {
            target.delete()
        }
        check(temp.renameTo(target)) { "Unable to archive image to ${target.absolutePath}" }
        return target
    }

    fun isArchived(file: File): Boolean {
        val archivePath = archiveDir.canonicalFile.toPath()
        return runCatching {
            file.canonicalFile.toPath().startsWith(archivePath)
        }.getOrDefault(false)
    }

    private fun uniqueTargetFile(extension: String): File {
        var candidate = archiveDir.resolve("${clock()}-${idFactory()}.$extension")
        while (candidate.exists() || File(candidate.parentFile, "${candidate.name}.tmp").exists()) {
            candidate = archiveDir.resolve("${clock()}-${idFactory()}.$extension")
        }
        return candidate
    }

    private fun extensionFor(displayName: String?, mimeType: String?): String {
        val nameExtension = displayName
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            ?.takeIf { it in SUPPORTED_EXTENSIONS }
        if (nameExtension != null) return normalizeExtension(nameExtension)

        return when (mimeType?.lowercase()) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/heic" -> "heic"
            "image/heif" -> "heif"
            else -> "jpg"
        }
    }

    private fun normalizeExtension(extension: String): String =
        if (extension == "jpeg") "jpg" else extension

    private companion object {
        const val ARCHIVE_DIR_NAME = "image_archive"
        val SUPPORTED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "heic", "heif")
    }
}
