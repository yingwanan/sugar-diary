package com.localdiary.app.data.transfer

import android.content.Context
import android.content.ContentResolver
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.localdiary.app.data.file.LocalEntryFileStore
import com.localdiary.app.data.local.entity.EmotionAnalysisEntity
import com.localdiary.app.data.local.entity.EntryEntity
import com.localdiary.app.data.local.entity.MoodReportEntity
import com.localdiary.app.data.local.entity.PsychologyChatMessageEntity
import com.localdiary.app.data.local.entity.UserPsychologyProfileEntity
import com.localdiary.app.data.local.entity.PsychologyAgentProcessEventEntity
import com.localdiary.app.data.local.entity.PsychologyAnalysisRunEntity
import com.localdiary.app.data.local.entity.StylePresetEntity
import com.localdiary.app.data.local.entity.VersionSnapshotEntity
import com.localdiary.app.model.EntryFormat
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class TransferManager(
    private val context: Context,
    private val fileStore: LocalEntryFileStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun exportBundle(
        resolver: ContentResolver,
        uri: Uri,
        entries: List<EntryEntity>,
        styles: List<StylePresetEntity>,
        analyses: List<EmotionAnalysisEntity>,
        reports: List<MoodReportEntity>,
        versions: List<VersionSnapshotEntity>,
        psychologyChats: List<PsychologyChatMessageEntity>,
        psychologyRuns: List<PsychologyAnalysisRunEntity>,
        psychologyEvents: List<PsychologyAgentProcessEventEntity>,
        userProfiles: List<UserPsychologyProfileEntity>,
    ) = withContext(Dispatchers.IO) {
        resolver.openOutputStream(uri)?.use { stream ->
            ZipOutputStream(stream).use { zip ->
                val manifest = BundleManifest(
                    entries = entries.map {
                        val format = EntryFormat.valueOf(it.format)
                        BundleEntry(
                            id = it.id,
                            title = it.title,
                            format = it.format,
                            tagsJson = it.tagsJson,
                            createdAt = it.createdAt,
                            updatedAt = it.updatedAt,
                            relativePath = archivePathForEntry(it.id, format),
                        )
                    },
                    styles = styles,
                    analyses = analyses,
                    reports = reports,
                    psychologyChats = psychologyChats,
                    psychologyRuns = psychologyRuns,
                    psychologyEvents = psychologyEvents,
                    userProfiles = userProfiles,
                    versions = versions.map {
                        val format = EntryFormat.valueOf(it.format)
                        BundleVersion(
                            versionId = it.versionId,
                            entryId = it.entryId,
                            source = it.source,
                            format = it.format,
                            createdAt = it.createdAt,
                            relativePath = archivePathForVersion(it.entryId, it.versionId, format),
                        )
                    },
                )

                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(json.encodeToString(BundleManifest.serializer(), manifest).toByteArray())
                zip.closeEntry()

                entries.forEach { entry ->
                    zipFile(zip, archivePathForEntry(entry.id, EntryFormat.valueOf(entry.format)), entry.filePath)
                }
                versions.forEach { version ->
                    zipFile(
                        zip,
                        archivePathForVersion(version.entryId, version.versionId, EntryFormat.valueOf(version.format)),
                        version.filePath,
                    )
                }
            }
        } ?: error("Unable to open export destination.")
    }

    suspend fun importBundle(
        resolver: ContentResolver,
        uri: Uri,
    ): ImportedBundle = withContext(Dispatchers.IO) {
        val files = mutableMapOf<String, ByteArray>()
        resolver.openInputStream(uri)?.use { stream ->
            ZipInputStream(stream).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    val buffer = ByteArrayOutputStream()
                    zip.copyTo(buffer)
                    files[entry.name] = buffer.toByteArray()
                }
            }
        } ?: error("Unable to open bundle.")

        val manifestBytes = files["manifest.json"] ?: error("Missing manifest.")
        val manifest = json.decodeFromString(BundleManifest.serializer(), manifestBytes.decodeToString())
        ImportedBundle(manifest = manifest, fileBytes = files)
    }

    suspend fun exportRawEntries(
        resolver: ContentResolver,
        folderUri: Uri,
        entries: List<EntryEntity>,
    ) = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, folderUri)
            ?: error("Unable to open target folder.")
        entries.forEach { entry ->
            val format = EntryFormat.valueOf(entry.format)
            val mimeType = if (format == EntryFormat.HTML) "text/html" else "text/markdown"
            val name = sanitize("${entry.updatedAt}_${entry.title}.${format.extension}")
            val file = root.createFile(mimeType, name) ?: return@forEach
            resolver.openOutputStream(file.uri)?.use { output ->
                output.write(fileStore.readBytes(entry.filePath))
            }
        }
    }

    suspend fun importRawEntries(
        resolver: ContentResolver,
        folderUri: Uri,
    ): List<RawImportFile> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, folderUri)
            ?: error("Unable to open source folder.")
        root.listFiles()
            .filter { it.isFile }
            .mapNotNull { file ->
                val name = file.name ?: return@mapNotNull null
                val format = EntryFormat.fromFileName(name) ?: return@mapNotNull null
                val bytes = resolver.openInputStream(file.uri)?.use { it.readBytes() } ?: return@mapNotNull null
                RawImportFile(name = name, format = format, bytes = bytes)
            }
    }

    private suspend fun zipFile(zip: ZipOutputStream, relativePath: String, sourcePath: String) {
        zip.putNextEntry(ZipEntry(relativePath))
        zip.write(fileStore.readBytes(sourcePath))
        zip.closeEntry()
    }

    private fun archivePathForEntry(entryId: String, format: EntryFormat): String = "entries/$entryId/${format.fileName}"

    private fun archivePathForVersion(entryId: String, versionId: String, format: EntryFormat): String =
        "entries/$entryId/versions/$versionId.${format.extension}"

    private fun sanitize(input: String): String = input.replace(Regex("[^a-zA-Z0-9._-]"), "_")
}

data class ImportedBundle(
    val manifest: BundleManifest,
    val fileBytes: Map<String, ByteArray>,
)

data class RawImportFile(
    val name: String,
    val format: EntryFormat,
    val bytes: ByteArray,
)

@Serializable
data class BundleManifest(
    val entries: List<BundleEntry>,
    val styles: List<StylePresetEntity>,
    val analyses: List<EmotionAnalysisEntity>,
    val reports: List<MoodReportEntity>,
    val versions: List<BundleVersion>,
    val psychologyChats: List<PsychologyChatMessageEntity> = emptyList(),
    val psychologyRuns: List<PsychologyAnalysisRunEntity> = emptyList(),
    val psychologyEvents: List<PsychologyAgentProcessEventEntity> = emptyList(),
    val userProfiles: List<UserPsychologyProfileEntity> = emptyList(),
)

@Serializable
data class BundleEntry(
    val id: String,
    val title: String,
    val format: String,
    val tagsJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val relativePath: String,
)

@Serializable
data class BundleVersion(
    val versionId: String,
    val entryId: String,
    val source: String,
    val format: String,
    val createdAt: Long,
    val relativePath: String,
)
