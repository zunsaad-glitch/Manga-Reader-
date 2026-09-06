package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ChapterExporter {

    suspend fun exportChapterToCbz(
        context: Context,
        mangaTitle: String,
        chapterTitle: String,
        imageUrls: List<String>
    ): File? = withContext(Dispatchers.IO) {
        try {
            val sanitizedManga = mangaTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val sanitizedChapter = chapterTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cbzFile = File(exportDir, "${sanitizedManga}_${sanitizedChapter}.cbz")

            if (cbzFile.exists()) {
                cbzFile.delete()
            }

            ZipOutputStream(FileOutputStream(cbzFile)).use { zipOut ->
                imageUrls.forEachIndexed { index, urlString ->
                    try {
                        val pageNum = String.format("%03d", index + 1)
                        val ext = if (urlString.contains(".png", ignoreCase = true)) "png" else "jpg"
                        val entryName = "page_$pageNum.$ext"

                        val zipEntry = ZipEntry(entryName)
                        zipOut.putNextEntry(zipEntry)
                        
                        if (urlString.startsWith("file://") || urlString.startsWith("/")) {
                            val localPath = if (urlString.startsWith("file://")) urlString.removePrefix("file://") else urlString
                            val localFile = File(localPath)
                            if (localFile.exists() && localFile.canRead()) {
                                localFile.inputStream().use { input ->
                                    input.copyTo(zipOut)
                                }
                            }
                        } else {
                            val url = URL(urlString)
                            val connection = url.openConnection()
                            connection.connectTimeout = 8000
                            connection.readTimeout = 8000
                            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")

                            connection.getInputStream().use { input ->
                                input.copyTo(zipOut)
                            }
                        }
                        zipOut.closeEntry()
                    } catch (_: Exception) {
                    }
                }
            }

            if (cbzFile.exists() && cbzFile.length() > 0) cbzFile else null
        } catch (e: Exception) {
            null
        }
    }

    fun shareCbzFile(context: Context, file: File, title: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/x-cbz"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share $title CBZ"))
        } catch (_: Exception) {
            // Fallback generic send
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_TEXT, "Exported chapter: $title")
            }
            context.startActivity(Intent.createChooser(intent, "Share Chapter"))
        }
    }
}
