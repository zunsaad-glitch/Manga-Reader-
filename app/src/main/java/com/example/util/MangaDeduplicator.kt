package com.example.util

import com.example.api.MangaData
import com.example.api.MangaAttributes
import com.example.repository.MangaToonRepository
import com.example.repository.ManhwaToonRepository
import com.example.repository.MantaRepository
import com.example.repository.ThreeDComicsRepository

object MangaDeduplicator {

    /**
     * Aggressively normalizes a title for cross-source comparison:
     * - Strips brackets/parentheses with metadata (e.g. [Official], (Webtoon), [Raw], etc.)
     * - Strips provider badges/watermarks (e.g. - ManhwaToon, - MangaToon, etc.)
     * - Strips leading badges (e.g. 18+, HOT, RAW)
     * - Converts to lowercase alphanumeric characters only
     */
    fun normalizeTitle(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var s = raw.lowercase().trim()

        // Remove bracketed or parenthesized tags like [Official], (Webtoon), [18+], (Raw), etc.
        s = s.replace(Regex("""\((?:official|webtoon|manhwa|manga|raw|uncensored|complete|color|scanlation|full color|18\+|adult)[^)]*\)""", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("""\[(?:official|webtoon|manhwa|manga|raw|uncensored|complete|color|scanlation|full color|18\+|adult)[^\]]*\]""", RegexOption.IGNORE_CASE), "")

        // Remove source suffix watermarks
        s = s.replace(Regex("""\s*[-–—:]\s*(?:manhwatoon|mangatoon|manta|mangadex|webtoon)\s*$""", RegexOption.IGNORE_CASE), "")

        // Remove leading prefixes like 18+, HOT, RAW, NEW, ADULT
        s = s.replace(Regex("""^(?:18\+\s*|hot\s*|raw\s*|new\s*|adult\s*)+""", RegexOption.IGNORE_CASE), "")

        // Unicode apostrophe normalization
        s = s.replace("’", "'").replace("‘", "'").replace("“", "\"").replace("”", "\"")
        s = s.replace("i'm", "iam")

        // Retain only lowercase alphanumeric
        s = s.replace(Regex("""[^a-z0-9]"""), "")
        return s
    }

    /**
     * Cleans an English title by removing watermarks and brackets while preserving proper case and punctuation.
     */
    fun cleanEnglishTitle(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        var s = raw.trim()

        // Remove trailing source branding like "- ManhwaToon", "- MangaToon", etc.
        s = s.replace(Regex("""\s*[-–—:]\s*(?:manhwatoon|mangatoon|manta|mangadex|webtoon)\s*$""", RegexOption.IGNORE_CASE), "")

        // Remove brackets like [Uncensored], [Official], [Full Color]
        s = s.replace(Regex("""\s*\[(?:official|webtoon|manhwa|manga|raw|uncensored|complete|color|scanlation|full color|18\+|adult)[^\]]*\]""", RegexOption.IGNORE_CASE), "")
        s = s.replace(Regex("""\s*\((?:official|webtoon|manhwa|manga|raw|uncensored|complete|color|scanlation|full color|18\+|adult)[^)]*\)""", RegexOption.IGNORE_CASE), "")

        // Remove leading badges like "18+ "
        s = s.replace(Regex("""^(?:18\+\s*|hot\s*|raw\s*|new\s*|adult\s*)+""", RegexOption.IGNORE_CASE), "")

        return s.trim()
    }

    /**
     * Checks if a title is primarily written in Latin / English alphabet
     */
    fun isEnglishLike(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val latinLetters = text.count { it in 'a'..'z' || it in 'A'..'Z' }
        return latinLetters >= 2 && latinLetters.toDouble() / text.length > 0.3
    }

    /**
     * Extracts the best available English title for a manga
     */
    fun getBestEnglishTitle(manga: MangaData): String? {
        val titles = manga.attributes?.title ?: emptyMap()

        // 1. Explicit English key
        titles["en"]?.let {
            val cleaned = cleanEnglishTitle(it)
            if (cleaned.isNotBlank()) return cleaned
        }

        // 2. Alt titles with "en"
        manga.attributes?.altTitles?.forEach { map ->
            map["en"]?.let {
                val cleaned = cleanEnglishTitle(it)
                if (cleaned.isNotBlank()) return cleaned
            }
        }

        // 3. Any title that is written in Latin characters
        titles.values.forEach { raw ->
            val cleaned = cleanEnglishTitle(raw)
            if (isEnglishLike(cleaned)) return cleaned
        }

        // 4. Alt titles in Latin characters
        manga.attributes?.altTitles?.forEach { map ->
            map.values.forEach { raw ->
                val cleaned = cleanEnglishTitle(raw)
                if (isEnglishLike(cleaned)) return cleaned
            }
        }

        // 5. If ID has slug e.g. mt_solo-leveling -> "Solo Leveling"
        val idSlug = manga.id.substringAfter('_')
        if (idSlug.contains('-') && idSlug.any { it in 'a'..'z' }) {
            val slugTitle = idSlug.split('-').joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            if (isEnglishLike(slugTitle)) return slugTitle
        }

        // Fallback to first available title
        return titles.values.firstOrNull { it.isNotBlank() }?.let { cleanEnglishTitle(it) }
    }

    /**
     * Extracts all normalized matching keys for grouping duplicates
     */
    fun getNormalizedKeys(manga: MangaData): Set<String> {
        val keys = mutableSetOf<String>()

        // Titles in attributes.title
        manga.attributes?.title?.values?.forEach { t ->
            val norm = normalizeTitle(t)
            if (norm.length >= 3) keys.add(norm)
        }

        // Alt titles
        manga.attributes?.altTitles?.forEach { map ->
            map.values.forEach { t ->
                val norm = normalizeTitle(t)
                if (norm.length >= 3) keys.add(norm)
            }
        }

        // Slug from ID if meaningful
        val idClean = manga.id.substringAfter('_')
        if (idClean.contains('-') && !idClean.all { it.isDigit() || it == '-' }) {
            val norm = normalizeTitle(idClean.replace("-", " "))
            if (norm.length >= 4) keys.add(norm)
        }

        return keys
    }

    /**
     * Determines the estimated or known chapter count for a manga:
     * - Parses `lastChapter` (e.g. "100", "95.5")
     * - Parses `latestUploadedChapter` (e.g. "Chapter 100", "Ch. 120")
     * - Checks in-memory caches of repository modules
     * - Extracts from description hints (e.g. "120 chapters", "Latest: Chapter 95")
     */
    fun getChapterCount(
        manga: MangaData,
        manhwaToonRepo: ManhwaToonRepository? = null,
        mangaToonRepo: MangaToonRepository? = null,
        mantaRepo: MantaRepository? = null,
        threeDRepo: ThreeDComicsRepository? = null
    ): Double {
        // 1. Direct lastChapter field (from MangaDex API or scrapers)
        manga.attributes?.lastChapter?.let { lc ->
            Regex("""(\d+(?:\.\d+)?)""").find(lc)?.groupValues?.get(1)?.toDoubleOrNull()?.let {
                if (it > 0) return it
            }
        }

        // 2. Direct latestUploadedChapter field
        manga.attributes?.latestUploadedChapter?.let { luc ->
            Regex("""(?:chapter|ch\.?|ep\.?|episode)?\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)
                .find(luc)?.groupValues?.get(1)?.toDoubleOrNull()?.let {
                    if (it > 0) return it
                }
        }

        // 3. Repository in-memory caches
        try {
            if (manga.id.startsWith("mt_") && manhwaToonRepo != null) {
                val count = manhwaToonRepo.getCachedChapterCount(manga.id)
                if (count > 0) return count.toDouble()
            } else if (manga.id.startsWith("mto_") && mangaToonRepo != null) {
                val count = mangaToonRepo.getCachedChapterCount(manga.id)
                if (count > 0) return count.toDouble()
            } else if (manga.id.startsWith("mta_") && mantaRepo != null) {
                val count = mantaRepo.getCachedChapterCount(manga.id)
                if (count > 0) return count.toDouble()
            } else if (manga.id.startsWith("3d_") && threeDRepo != null) {
                val count = threeDRepo.getChaptersForManga(manga.id)?.size ?: 0
                if (count > 0) return count.toDouble()
            }
        } catch (_: Exception) {}

        // 4. Description hints (e.g. "Read 120 chapters", "Latest: Chapter 88")
        val desc = manga.attributes?.description?.values?.firstOrNull().orEmpty()
        val descMatch = Regex("""(?:latest|total|over)?\s*(\d+(?:\.\d+)?)\s*(?:chapters?|episodes?|chaps?|eps?)""", RegexOption.IGNORE_CASE).find(desc)
        if (descMatch != null) {
            descMatch.groupValues[1].toDoubleOrNull()?.let {
                if (it > 0) return it
            }
        }

        // Fallback baseline
        return 1.0
    }

    /**
     * Deduplicates a list of mangas across all sources.
     * When duplicates exist for the same manga:
     * - Chooses the one with the MAXIMUM number of chapters with English title
     * - Guarantees that the resulting card has the clean English title
     * - Enriches metadata (covers, tags, description) from all candidates
     */
    fun deduplicate(
        mangas: List<MangaData>,
        manhwaToonRepo: ManhwaToonRepository? = null,
        mangaToonRepo: MangaToonRepository? = null,
        mantaRepo: MantaRepository? = null,
        threeDRepo: ThreeDComicsRepository? = null
    ): List<MangaData> {
        if (mangas.isEmpty()) return emptyList()

        val clusters = mutableListOf<MutableList<MangaData>>()
        val keyToCluster = mutableMapOf<String, Int>()

        for (manga in mangas) {
            val keys = getNormalizedKeys(manga)
            val matchingClusterIndices = keys.mapNotNull { keyToCluster[it] }.distinct()

            if (matchingClusterIndices.isEmpty()) {
                val newIndex = clusters.size
                clusters.add(mutableListOf(manga))
                for (k in keys) {
                    keyToCluster[k] = newIndex
                }
            } else {
                val primaryIndex = matchingClusterIndices.first()
                clusters[primaryIndex].add(manga)
                for (k in keys) {
                    keyToCluster[k] = primaryIndex
                }
                // Merge multiple clusters if keys bridge them
                for (otherIndex in matchingClusterIndices.drop(1)) {
                    val otherList = clusters[otherIndex]
                    clusters[primaryIndex].addAll(otherList)
                    otherList.clear()
                }
            }
        }

        val result = mutableListOf<MangaData>()

        for (cluster in clusters) {
            if (cluster.isEmpty()) continue

            // 1. Identify best English title across the cluster
            val bestEnglishTitle = cluster
                .mapNotNull { getBestEnglishTitle(it) }
                .firstOrNull { it.isNotBlank() && isEnglishLike(it) }
                ?: cluster.firstOrNull()?.let { getBestEnglishTitle(it) }

            // 2. Select the candidate with MAXIMUM chapter count with English title
            val winner = cluster.maxWithOrNull(
                compareBy<MangaData> { candidate ->
                    getChapterCount(candidate, manhwaToonRepo, mangaToonRepo, mantaRepo, threeDRepo)
                }.thenBy { candidate ->
                    // Favor candidates that natively have an English title
                    if (isEnglishLike(candidate.attributes?.title?.get("en"))) 1 else 0
                }.thenBy { candidate ->
                    // Favor candidates with high quality covers
                    if (candidate.getCoverImageUrl() != null) 1 else 0
                }
            ) ?: cluster.first()

            // 3. Ensure the chosen manga carries the clean English title
            val finalTitle = bestEnglishTitle ?: winner.attributes?.title?.values?.firstOrNull() ?: winner.id
            val updatedTitleMap = (winner.attributes?.title ?: emptyMap()).toMutableMap().apply {
                put("en", finalTitle)
            }

            // 4. Enrich tags and description if winner lacks them but duplicates have them
            val mergedTags = (winner.attributes?.tags.orEmpty() + cluster.flatMap { it.attributes?.tags.orEmpty() }).distinctBy { it.id }
            val bestDescription = winner.attributes?.description ?: cluster.firstNotNullOfOrNull { it.attributes?.description }

            val enrichedWinner = winner.copy(
                attributes = (winner.attributes ?: MangaAttributes()).copy(
                    title = updatedTitleMap,
                    tags = mergedTags,
                    description = bestDescription
                )
            )

            result.add(enrichedWinner)
        }

        return result
    }
}
