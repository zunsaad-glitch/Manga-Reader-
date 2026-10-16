package com.example

import com.example.model.ContentRating
import com.example.model.ContentRatingPreset
import com.example.repository.MangaRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testTagMappingContainsRequestedTags() {
        val repo = MangaRepository()
        // Verify requested tags like Incest exist and map to official MangaDex UUID
        assertNotNull(repo.tagMap["Incest"])
        assertEquals("5bd0e105-4481-44ca-b6e7-7544da56b1a3", repo.tagMap["Incest"])
        assertNotNull(repo.tagMap["Gore"])
        assertNotNull(repo.tagMap["Sexual Violence"])
        assertNotNull(repo.tagMap["Doujinshi"])
        assertNotNull(repo.tagMap["Harem"])
    }

    @Test
    fun testContentRatingPresets() {
        val allPreset = ContentRatingPreset.ALL
        assertTrue(allPreset.ratings.contains("pornographic"))
        assertTrue(allPreset.ratings.contains("erotica"))
        assertTrue(allPreset.ratings.contains("safe"))
        assertTrue(allPreset.ratings.contains("suggestive"))

        val safeCleanPreset = ContentRatingPreset.SAFE_CLEAN
        assertTrue(safeCleanPreset.ratings.contains("safe"))
        assertTrue(safeCleanPreset.ratings.contains("suggestive"))
        assertTrue(!safeCleanPreset.ratings.contains("pornographic"))
        assertTrue(!safeCleanPreset.ratings.contains("erotica"))

        val explicitPreset = ContentRatingPreset.EXPLICIT_ONLY
        assertTrue(explicitPreset.ratings.contains("pornographic"))
        assertTrue(explicitPreset.ratings.contains("erotica"))
        assertTrue(!explicitPreset.ratings.contains("safe"))
    }

    @Test
    fun testCacheRetrievalOnInitialLoad() {
        val repo = MangaRepository()
        val cached = repo.getCachedMangas(
            source = com.example.model.MangaSource.MANHWATOON,
            page = 1,
            selectedTag = "All"
        )
        // Initially null before network call
        org.junit.Assert.assertNull(cached)
    }

    @Test
    fun testViewModelConstructorReflection() {
        val constructor = com.example.viewmodel.MainViewModel::class.java.getConstructor(android.app.Application::class.java)
        assertNotNull(constructor)
    }
}
