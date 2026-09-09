package com.example.data

import android.content.Context
import android.util.Log
import com.example.api.ChapterData
import com.example.api.MangaData
import com.example.data.db.AppDatabase
import com.example.data.db.OfflineChapterEntity
import com.example.data.db.OfflineMangaEntity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class OfflineMangaRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val offlineDao = database.offlineDao()
    private val gson = Gson()
    private val httpClient = com.example.api.NetworkClient.getClient(context)

    val allOfflineMangas: Flow<List<OfflineMangaEntity>> = offlineDao.getAllOfflineMangas()
    val downloadedChapterIds: Flow<List<String>> = offlineDao.getAllDownloadedChapterIds()
    val downloadedMangaIds: Flow<List<String>> = offlineDao.getAllDownloadedMangaIds()

    fun getOfflineChapters(mangaId: String): Flow<List<OfflineChapterEntity>> {
        return offlineDao.getOfflineChaptersForManga(mangaId)
    }

    suspend fun getOfflineChapter(chapterId: String): OfflineChapterEntity? {
        return withContext(Dispatchers.IO) {
            offlineDao.getOfflineChapter(chapterId)
        }
    }

    suspend fun isChapterDownloaded(chapterId: String): Boolean {
        return withContext(Dispatchers.IO) {
            val entity = offlineDao.getOfflineChapter(chapterId) ?: return@withContext false
            try {
                val listType = object : TypeToken<List<String>>() {}.type
                val paths: List<String> = gson.fromJson(entity.pagesJson, listType)
                val allValid = paths.isNotEmpty() && paths.all { path ->
                    val file = File(path)
                    file.exists() && file.length() > 0
                }
                if (!allValid) {
                    offlineDao.deleteOfflineChapter(chapterId)
                    false
                } else {
                    true
                }
            } catch (_: Exception) {
                offlineDao.deleteOfflineChapter(chapterId)
                false
            }
        }
    }

    suspend fun getOfflinePageUrls(chapterId: String): List<String>? {
        return withContext(Dispatchers.IO) {
            val entity = offlineDao.getOfflineChapter(chapterId) ?: return@withContext null
            try {
                val listType = object : TypeToken<List<String>>() {}.type
                val paths: List<String> = gson.fromJson(entity.pagesJson, listType)
                val validLocalFiles = paths.mapNotNull { path ->
                    val file = File(path)
                    if (file.exists() && file.length() > 0) {
                        file.toURI().toString()
                    } else {
                        null
                    }
                }
                if (validLocalFiles.isNotEmpty() && validLocalFiles.size == paths.size) {
                    validLocalFiles
                } else {
                    // Purge stale/broken DB record so it won't block online streaming
                    offlineDao.deleteOfflineChapter(chapterId)
                    null
                }
            } catch (e: Exception) {
                Log.e("OfflineRepo", "Error parsing pages JSON", e)
                offlineDao.deleteOfflineChapter(chapterId)
                null
            }
        }
    }

    suspend fun saveChapterForOffline(
        manga: MangaData,
        chapter: ChapterData,
        imageUrls: List<String>,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Boolean = withContext(Dispatchers.IO) {
        if (imageUrls.isEmpty()) return@withContext false

        try {
            val mangaDir = File(context.filesDir, "offline_manga/${manga.id}")
            val chapterDir = File(mangaDir, chapter.id)
            if (!chapterDir.exists()) {
                chapterDir.mkdirs()
            }

            val localPaths = mutableListOf<String>()
            var chapterTotalBytes = 0L

            for (index in imageUrls.indices) {
                val url = imageUrls[index]
                val ext = if (url.contains(".png", ignoreCase = true)) "png" else if (url.contains(".webp", ignoreCase = true)) "webp" else "jpg"
                val pageFile = File(chapterDir, "page_${index + 1}.$ext")

                var downloaded = false
                var attempts = 0
                while (!downloaded && attempts < 2) {
                    attempts++
                    try {
                        val requestBuilder = Request.Builder().url(url)
                            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        if (url.contains("mangadex.org") || url.contains("uploads.mangadex.org")) {
                            requestBuilder.addHeader("Referer", "https://mangadex.org/")
                        } else if (url.contains("nhentai") || url.contains("pururin") || url.contains("hentaifox") || url.contains("3hentai")) {
                            requestBuilder.addHeader("Referer", "https://nhentai.net/")
                        }

                        val response = httpClient.newCall(requestBuilder.build()).execute()
                        if (response.isSuccessful && response.body != null) {
                            val bytes = response.body!!.bytes()
                            if (bytes.isNotEmpty()) {
                                FileOutputStream(pageFile).use { fos ->
                                    fos.write(bytes)
                                }
                                chapterTotalBytes += bytes.size
                                localPaths.add(pageFile.absolutePath)
                                downloaded = true
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("OfflineRepo", "Attempt $attempts failed for page $index: ${e.message}")
                    }
                }

                if (!downloaded) {
                    Log.w("OfflineRepo", "Failed to download page $index after retries: $url")
                    return@withContext false
                }

                onProgress(index + 1, imageUrls.size)
            }

            // Save Chapter Entity
            val chapterEntity = OfflineChapterEntity(
                chapterId = chapter.id,
                mangaId = manga.id,
                chapterNumber = chapter.attributes?.chapter ?: "1",
                title = chapter.attributes?.title ?: "Chapter ${chapter.attributes?.chapter ?: "1"}",
                pagesJson = gson.toJson(localPaths),
                pageCount = localPaths.size,
                downloadTimestamp = System.currentTimeMillis()
            )
            offlineDao.insertOfflineChapter(chapterEntity)

            // Save or Update Manga Entity
            val existingManga = offlineDao.getOfflineMangaSync(manga.id)
            val currentChapters = offlineDao.getOfflineChaptersForMangaSync(manga.id)
            val mangaTitle = manga.attributes?.title?.get("en")
                ?: manga.attributes?.title?.values?.firstOrNull()
                ?: "Manga"
            val mangaCover = manga.getCoverImageUrl()

            val mangaEntity = OfflineMangaEntity(
                mangaId = manga.id,
                title = mangaTitle,
                coverUrl = mangaCover,
                author = manga.getAuthorName(),
                description = manga.attributes?.description?.get("en") ?: manga.attributes?.description?.values?.firstOrNull(),
                contentRating = manga.attributes?.contentRating ?: "safe",
                downloadedChaptersCount = currentChapters.size,
                totalChaptersCount = maxOf(existingManga?.totalChaptersCount ?: 0, currentChapters.size),
                totalSizeBytes = (existingManga?.totalSizeBytes ?: 0L) + chapterTotalBytes,
                downloadTimestamp = System.currentTimeMillis()
            )
            offlineDao.insertOfflineManga(mangaEntity)
            return@withContext true
        } catch (e: Exception) {
            Log.e("OfflineRepo", "Error saving chapter offline", e)
            return@withContext false
        }
    }

    suspend fun cachePagesAutomatically(
        mangaId: String,
        mangaTitle: String,
        coverUrl: String?,
        chapterId: String,
        chapterNumber: String,
        chapterTitle: String,
        imageUrls: List<String>
    ) = withContext(Dispatchers.IO) {
        // No-op: do not mark streamed chapters as offline unless user explicitly downloads them
    }

    suspend fun deleteOfflineManga(mangaId: String) = withContext(Dispatchers.IO) {
        try {
            offlineDao.deleteOfflineChaptersForManga(mangaId)
            offlineDao.deleteOfflineManga(mangaId)
            val mangaDir = File(context.filesDir, "offline_manga/$mangaId")
            if (mangaDir.exists()) {
                mangaDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.e("OfflineRepo", "Error deleting offline manga", e)
        }
    }

    suspend fun deleteOfflineChapter(chapterId: String, mangaId: String) = withContext(Dispatchers.IO) {
        try {
            offlineDao.deleteOfflineChapter(chapterId)
            val chapterDir = File(context.filesDir, "offline_manga/$mangaId/$chapterId")
            if (chapterDir.exists()) {
                chapterDir.deleteRecursively()
            }
            val remaining = offlineDao.getOfflineChaptersForMangaSync(mangaId)
            if (remaining.isEmpty()) {
                offlineDao.deleteOfflineManga(mangaId)
            } else {
                val existingManga = offlineDao.getOfflineMangaSync(mangaId)
                if (existingManga != null) {
                    offlineDao.insertOfflineManga(existingManga.copy(downloadedChaptersCount = remaining.size))
                }
            }
        } catch (e: Exception) {
            Log.e("OfflineRepo", "Error deleting offline chapter", e)
        }
    }
}
