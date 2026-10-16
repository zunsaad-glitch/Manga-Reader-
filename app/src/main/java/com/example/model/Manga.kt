package com.example.model

enum class MangaSource(val displayName: String) {
    ALL("All Sources"),
    MANGADEX("MangaDex"),
    MANHWATOON("ManhwaToon"),
    MANGATOON("MangaToon"),
    MANTA("Manta")
}

enum class MangaVariety(val id: String, val displayName: String, val badge: String) {
    ALL_POPULAR("popular", "Trending Hits", "🔥"),
    TOP_RATED("top_rated", "Top Rated", "⭐"),
    LATEST_UPDATES("latest", "Latest Updates", "⚡"),
    COMPLETED("completed", "Completed Series", "✅"),
    MANHWA("manhwa", "Korean Webtoons", "🇰🇷"),
    MANHUA("manhua", "Chinese Manhua", "🇨🇳"),
    ROMANCE("romance", "Romance & Drama", "💖"),
    ISEKAI("isekai", "Isekai & Fantasy", "🌀"),
    ACTION("action", "Action & Martial Arts", "⚔️"),
    PSYCHOLOGICAL("psychological", "Psychological Thrillers", "🧠"),
    DARK_FANTASY("dark_fantasy", "Dark Fantasy & Horror", "💀"),
    COMEDY("comedy", "Comedy & Fun", "😂"),
    SLICE_OF_LIFE("slice_of_life", "Slice of Life", "☕"),
    SCI_FI("sci_fi", "Sci-Fi & Cyberpunk", "🚀"),
    MARTIAL_ARTS("martial_arts", "Martial Arts & Cultivation", "🥋"),
    MYSTERY("mystery", "Mystery & Detective", "🔍"),
    SUPERNATURAL("supernatural", "Supernatural & Magic", "🔮"),
    SPORTS("sports", "Sports & Shounen", "⚽")
}

enum class ContentRating(val apiValue: String, val label: String) {
    SAFE("safe", "Safe"),
    SUGGESTIVE("suggestive", "Suggestive"),
    EROTICA("erotica", "Erotica"),
    PORNOGRAPHIC("pornographic", "18+ Explicit")
}

enum class ContentRatingPreset(val label: String, val ratings: Set<String>) {
    ALL("All (Includes 18+)", setOf("safe", "suggestive", "erotica", "pornographic")),
    SAFE_CLEAN("Safe & Clean", setOf("safe", "suggestive")),
    EXPLICIT_ONLY("18+ Explicit Only", setOf("erotica", "pornographic"))
}

data class Manga(
    val id: String,
    val title: String,
    val coverUrl: String,
    val author: String,
    val rating: Float = 4.8f,
    val status: String = "Ongoing",
    val synopsis: String,
    val genres: List<String>,
    val source: MangaSource,
    val totalChapters: Int = 120,
    val isTrending: Boolean = false,
    val isEditorPick: Boolean = false,
    val latestChapter: String = "Ch. 120",
    val contentRating: String = "safe"
)

data class Chapter(
    val id: String,
    val mangaId: String,
    val chapterNumber: Float,
    val title: String,
    val releaseDate: String,
    val pages: List<String> = emptyList(),
    val language: String = "en"
)

data class ReadingProgress(
    val mangaId: String,
    val mangaTitle: String,
    val coverUrl: String,
    val source: MangaSource,
    val lastChapter: Float,
    val lastPage: Int,
    val totalPages: Int,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ReaderOrientation {
    VERTICAL_WEBTOON,
    HORIZONTAL_PAGE
}
