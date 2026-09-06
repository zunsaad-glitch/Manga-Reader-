package com.example.util

data class AchievementBadge(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val category: String,
    val isUnlocked: Boolean,
    val progress: Float, // 0f to 1f
    val progressText: String
)

object AchievementManager {

    fun computeBadges(
        totalChaptersRead: Int,
        readingStreakDays: Int,
        totalReadingMinutes: Int,
        offlineCount: Int,
        surpriseRollsCount: Int,
        shelvesCount: Int
    ): List<AchievementBadge> {
        val badges = mutableListOf<AchievementBadge>()

        // 1. First Page Turned
        val ch1Unlocked = totalChaptersRead >= 1
        badges.add(
            AchievementBadge(
                id = "first_chapter",
                title = "First Step",
                description = "Read your first chapter",
                icon = "🌱",
                category = "Reading",
                isUnlocked = ch1Unlocked,
                progress = if (ch1Unlocked) 1f else 0f,
                progressText = if (ch1Unlocked) "Completed" else "0 / 1"
            )
        )

        // 2. Binge Specialist
        val ch25Unlocked = totalChaptersRead >= 25
        badges.add(
            AchievementBadge(
                id = "binge_specialist",
                title = "Binge Specialist",
                description = "Read 25 chapters",
                icon = "🥈",
                category = "Reading",
                isUnlocked = ch25Unlocked,
                progress = (totalChaptersRead / 25f).coerceIn(0f, 1f),
                progressText = "$totalChaptersRead / 25"
            )
        )

        // 3. Manga Master
        val ch100Unlocked = totalChaptersRead >= 100
        badges.add(
            AchievementBadge(
                id = "manga_master",
                title = "Manga Sovereign",
                description = "Reach 100 chapters read",
                icon = "👑",
                category = "Reading",
                isUnlocked = ch100Unlocked,
                progress = (totalChaptersRead / 100f).coerceIn(0f, 1f),
                progressText = "$totalChaptersRead / 100"
            )
        )

        // 4. Reading Streak 3 Days
        val streak3Unlocked = readingStreakDays >= 3
        badges.add(
            AchievementBadge(
                id = "streak_3",
                title = "Flame of Routine",
                description = "Maintain a 3-day reading streak",
                icon = "🔥",
                category = "Dedication",
                isUnlocked = streak3Unlocked,
                progress = (readingStreakDays / 3f).coerceIn(0f, 1f),
                progressText = "$readingStreakDays / 3 days"
            )
        )

        // 5. Reading Streak 7 Days
        val streak7Unlocked = readingStreakDays >= 7
        badges.add(
            AchievementBadge(
                id = "streak_7",
                title = "Immortal Dedication",
                description = "Maintain a 7-day reading streak",
                icon = "⚡",
                category = "Dedication",
                isUnlocked = streak7Unlocked,
                progress = (readingStreakDays / 7f).coerceIn(0f, 1f),
                progressText = "$readingStreakDays / 7 days"
            )
        )

        // 6. Time Spent
        val timeUnlocked = totalReadingMinutes >= 120
        badges.add(
            AchievementBadge(
                id = "marathon_reader",
                title = "Deep Immersion",
                description = "Spend over 2 hours reading",
                icon = "⏳",
                category = "Time",
                isUnlocked = timeUnlocked,
                progress = (totalReadingMinutes / 120f).coerceIn(0f, 1f),
                progressText = "${totalReadingMinutes}m / 120m"
            )
        )

        // 7. Offline Vault Archivist
        val offlineUnlocked = offlineCount >= 1
        badges.add(
            AchievementBadge(
                id = "offline_vault",
                title = "Offline Archivist",
                description = "Download chapters for offline reading",
                icon = "💾",
                category = "Library",
                isUnlocked = offlineUnlocked,
                progress = (offlineCount / 3f).coerceIn(0f, 1f),
                progressText = "$offlineCount / 3"
            )
        )

        // 8. Destiny Gambler (Dice Roll)
        val diceUnlocked = surpriseRollsCount >= 3
        badges.add(
            AchievementBadge(
                id = "destiny_gambler",
                title = "Destiny Gambler",
                description = "Roll the Surprise Dice 3+ times",
                icon = "🎲",
                category = "Discovery",
                isUnlocked = diceUnlocked,
                progress = (surpriseRollsCount / 3f).coerceIn(0f, 1f),
                progressText = "$surpriseRollsCount / 3"
            )
        )

        // 9. Shelf Curator
        val shelfUnlocked = shelvesCount >= 1
        badges.add(
            AchievementBadge(
                id = "shelf_curator",
                title = "Curator Supreme",
                description = "Create and organize custom shelves",
                icon = "📁",
                category = "Library",
                isUnlocked = shelfUnlocked,
                progress = if (shelfUnlocked) 1f else 0f,
                progressText = "$shelvesCount shelves"
            )
        )

        return badges
    }
}
