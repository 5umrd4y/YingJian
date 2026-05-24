package com.yingjian.core.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

class ImageArchiveRepository(
    private val context: Context,
    private val store: ImageArchiveStore = ImageArchiveStore(context.filesDir)
) {
    fun archiveUri(uri: Uri): String {
        if (uri.scheme == "file") {
            val file = uri.path?.let(::File)
            if (file != null && store.isArchived(file)) {
                return Uri.fromFile(file).toString()
            }
        }

        val archivedFile = store.archive(
            displayName = queryDisplayName(uri),
            mimeType = context.contentResolver.getType(uri),
            openInputStream = {
                context.contentResolver.openInputStream(uri)
                    ?: throw FileNotFoundException("Cannot open image URI: $uri")
            }
        )
        return Uri.fromFile(archivedFile).toString()
    }

    fun archiveUris(uris: List<Uri>): List<String> = uris.map { archiveUri(it) }

    fun archiveUriString(uriString: String): String {
        return archiveUri(Uri.parse(uriString))
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else {
                    null
                }
            }
        }.getOrNull()
    }
}
