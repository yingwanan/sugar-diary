package com.localdiary.app.data.file

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.localdiary.app.model.AppStorageSettings
import com.localdiary.app.model.EntryFormat
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalEntryFileStore(
    private val context: Context,
) {
    private val entriesRoot: File
        get() = File(context.filesDir, "entries")

    suspend fun createEntry(
        entryId: String,
        format: EntryFormat,
        content: String,
        storageSettings: AppStorageSettings,
    ): String = withContext(Dispatchers.IO) {
        when (storageSettings.mode) {
            com.localdiary.app.model.StorageMode.APP_PRIVATE -> {
                val file = File(File(entriesRoot, entryId).apply { mkdirs() }, format.fileName)
                file.writeText(content)
                file.absolutePath
            }

            com.localdiary.app.model.StorageMode.SYSTEM_FOLDER -> {
                val root = resolveTreeRoot(storageSettings)
                val entriesDir = ensureDirectory(root, "entries")
                val entryDir = ensureDirectory(entriesDir, entryId)
                val file = entryDir.findFile(format.fileName)
                    ?: entryDir.createFile(format.mimeType, format.fileName)
                    ?: error("Unable to create article file in the selected folder.")
                writeBytes(file.uri.toString(), content.encodeToByteArray())
                file.uri.toString()
            }
        }
    }

    suspend fun readContent(path: String): String = withContext(Dispatchers.IO) {
        readBytes(path).decodeToString()
    }

    suspend fun readBytes(path: String): ByteArray = withContext(Dispatchers.IO) {
        when {
            path.startsWith("content://") -> {
                context.contentResolver.openInputStream(Uri.parse(path))?.use { it.readBytes() }
                    ?: error("Unable to read article content.")
            }

            path.isBlank() -> byteArrayOf()
            else -> {
                val file = File(path)
                if (file.exists()) file.readBytes() else byteArrayOf()
            }
        }
    }

    suspend fun overwrite(path: String, content: String) = withContext(Dispatchers.IO) {
        writeBytes(path, content.encodeToByteArray())
    }

    suspend fun overwriteEntry(
        entryId: String,
        currentPath: String,
        currentFormat: EntryFormat,
        targetFormat: EntryFormat,
        content: String,
        storageSettings: AppStorageSettings,
    ): String = withContext(Dispatchers.IO) {
        if (currentFormat == targetFormat) {
            writeBytes(currentPath, content.encodeToByteArray())
            return@withContext currentPath
        }

        when (storageSettings.mode) {
            com.localdiary.app.model.StorageMode.APP_PRIVATE -> {
                val entryDir = File(entriesRoot, entryId).apply { mkdirs() }
                val targetFile = File(entryDir, targetFormat.fileName)
                targetFile.writeText(content)
                deletePathIfNeeded(currentPath, keepPath = targetFile.absolutePath)
                targetFile.absolutePath
            }

            com.localdiary.app.model.StorageMode.SYSTEM_FOLDER -> {
                val root = resolveTreeRoot(storageSettings)
                val entriesDir = ensureDirectory(root, "entries")
                val entryDir = ensureDirectory(entriesDir, entryId)
                val currentName = currentFormat.fileName
                val targetFile = entryDir.findFile(targetFormat.fileName)
                    ?: entryDir.createFile(targetFormat.mimeType, targetFormat.fileName)
                    ?: error("Unable to create converted article file.")
                writeBytes(targetFile.uri.toString(), content.encodeToByteArray())
                if (currentName != targetFormat.fileName) {
                    entryDir.findFile(currentName)?.delete()
                }
                targetFile.uri.toString()
            }
        }
    }

    suspend fun saveVersion(
        entryId: String,
        format: EntryFormat,
        content: String,
        source: String,
    ): String = withContext(Dispatchers.IO) {
        val versionName = "${System.currentTimeMillis()}_${source.lowercase()}_${UUID.randomUUID().toString().take(8)}.${format.extension}"
        val file = File(File(File(entriesRoot, entryId), "versions").apply { mkdirs() }, versionName)
        file.writeText(content)
        file.absolutePath
    }

    suspend fun importContent(
        entryId: String,
        sourceName: String,
        bytes: ByteArray,
        storageSettings: AppStorageSettings,
    ): Pair<EntryFormat, String> =
        withContext(Dispatchers.IO) {
            val format = EntryFormat.fromFileName(sourceName) ?: EntryFormat.MARKDOWN
            when (storageSettings.mode) {
                com.localdiary.app.model.StorageMode.APP_PRIVATE -> {
                    val file = File(File(entriesRoot, entryId).apply { mkdirs() }, format.fileName)
                    file.writeBytes(bytes)
                    format to file.absolutePath
                }

                com.localdiary.app.model.StorageMode.SYSTEM_FOLDER -> {
                    val root = resolveTreeRoot(storageSettings)
                    val entriesDir = ensureDirectory(root, "entries")
                    val entryDir = ensureDirectory(entriesDir, entryId)
                    val file = entryDir.findFile(format.fileName)
                        ?: entryDir.createFile(format.mimeType, format.fileName)
                        ?: error("Unable to import article into the selected folder.")
                    writeBytes(file.uri.toString(), bytes)
                    format to file.uri.toString()
                }
            }
        }

    suspend fun importVersion(entryId: String, sourceName: String, bytes: ByteArray): String =
        withContext(Dispatchers.IO) {
            val target = File(File(File(entriesRoot, entryId), "versions").apply { mkdirs() }, File(sourceName).name)
            target.writeBytes(bytes)
            target.absolutePath
        }

    suspend fun deleteEntryFiles(
        entryId: String,
        filePath: String,
        storageSettings: AppStorageSettings,
    ) = withContext(Dispatchers.IO) {
        File(entriesRoot, entryId).deleteRecursively()
        if (storageSettings.mode == com.localdiary.app.model.StorageMode.SYSTEM_FOLDER || filePath.startsWith("content://")) {
            runCatching {
                val root = resolveTreeRoot(storageSettings)
                val entriesDir = root.findFile("entries")?.takeIf { it.isDirectory } ?: return@runCatching
                entriesDir.findFile(entryId)?.let(::deleteRecursively)
            }.getOrElse { error ->
                throw IllegalStateException(error.message ?: "Unable to delete the entry folder from the selected system folder.")
            }
        }
    }

    private fun writeBytes(path: String, bytes: ByteArray) {
        when {
            path.startsWith("content://") -> {
                context.contentResolver.openOutputStream(Uri.parse(path), "wt")?.use { output ->
                    output.write(bytes)
                } ?: error("Unable to write article content.")
            }

            else -> {
                File(path).apply {
                    parentFile?.mkdirs()
                    writeBytes(bytes)
                }
            }
        }
    }

    private fun deletePathIfNeeded(path: String, keepPath: String) {
        if (path.isBlank() || path == keepPath || path.startsWith("content://")) return
        File(path).takeIf { it.exists() }?.delete()
    }

    private fun resolveTreeRoot(storageSettings: AppStorageSettings): DocumentFile {
        val treeUri = storageSettings.treeUri ?: error("Please select a system folder before saving articles.")
        return DocumentFile.fromTreeUri(context, Uri.parse(treeUri))
            ?: error("Unable to open the selected system folder.")
    }

    private fun ensureDirectory(parent: DocumentFile, name: String): DocumentFile {
        return parent.findFile(name)?.takeIf { it.isDirectory }
            ?: parent.createDirectory(name)
            ?: error("Unable to create folder: $name")
    }

    private fun deleteRecursively(file: DocumentFile) {
        if (file.isDirectory) {
            file.listFiles().forEach(::deleteRecursively)
        }
        if (file.exists() && !file.delete()) {
            error("Unable to delete file or folder: ${file.name ?: "unknown"}")
        }
    }
}
