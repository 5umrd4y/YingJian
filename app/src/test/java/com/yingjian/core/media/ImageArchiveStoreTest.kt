package com.yingjian.core.media

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class ImageArchiveStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun archiveCopiesImageIntoPrivateDirectoryAndReturnsFileUri() {
        val root = temporaryFolder.newFolder("files")
        val store = ImageArchiveStore(
            rootDir = root,
            clock = { 1234L },
            idFactory = { "image-id" }
        )
        val bytes = byteArrayOf(1, 2, 3, 4, 5)

        val archivedFile = store.archive(
            displayName = "holiday original.jpeg",
            mimeType = "image/jpeg",
            openInputStream = { ByteArrayInputStream(bytes) }
        )

        assertEquals(root.resolve("image_archive").canonicalFile, archivedFile.parentFile?.canonicalFile)
        assertTrue(archivedFile.exists())
        assertTrue(archivedFile.name.endsWith(".jpg"))
        assertArrayEquals(bytes, archivedFile.readBytes())
    }
}
