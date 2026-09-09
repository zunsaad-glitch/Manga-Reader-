package com.example

import com.example.api.MangaAttributes
import com.example.api.MangaData
import com.example.util.MangaDeduplicator
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun deduplication_picksMaximumChaptersWithEnglishTitle() {
        // ManhwaToon has 100 chapters for Manga A
        val manhwaToonMangaA = MangaData(
            id = "mt_manga-a",
            attributes = MangaAttributes(
                title = mapOf("en" to "Manga A - ManhwaToon"),
                latestUploadedChapter = "Chapter 100",
                lastChapter = "100"
            )
        )

        // MangaDex has 90 chapters for Manga A with official English title
        val mangaDexMangaA = MangaData(
            id = "dex-uuid-manga-a",
            attributes = MangaAttributes(
                title = mapOf("en" to "Manga A"),
                lastChapter = "90"
            )
        )

        val input = listOf(mangaDexMangaA, manhwaToonMangaA)
        val deduplicated = MangaDeduplicator.deduplicate(input)

        // Exactly one result for Manga A
        assertEquals(1, deduplicated.size)
        val winner = deduplicated[0]

        // Should pick the one with maximum number of chapters (ManhwaToon with 100 chapters)
        assertEquals("mt_manga-a", winner.id)
        val chapterCount = MangaDeduplicator.getChapterCount(winner)
        assertEquals(100.0, chapterCount, 0.01)

        // Should have clean English title
        assertEquals("Manga A", winner.attributes?.title?.get("en"))
    }
}
