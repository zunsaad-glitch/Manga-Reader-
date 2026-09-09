package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val IS_AGE_VERIFIED = booleanPreferencesKey("is_age_verified")
        val RATING_SAFE = booleanPreferencesKey("rating_safe")
        val RATING_SUGGESTIVE = booleanPreferencesKey("rating_suggestive")
        val RATING_EROTICA = booleanPreferencesKey("rating_erotica")
        val RATING_PORNOGRAPHIC = booleanPreferencesKey("rating_pornographic")
        val THEME_MILF_STEPMOTHER = booleanPreferencesKey("theme_milf_stepmother")
        val BOOKMARKED_IDS = stringSetPreferencesKey("bookmarked_manga_ids")
        val FAVORITE_IDS = stringSetPreferencesKey("favorite_manga_ids")
        val AGE_WARNING_DISMISSED_PERMANENT = booleanPreferencesKey("age_warning_dismissed_permanent")
        val THEME_MODE = androidx.datastore.preferences.core.stringPreferencesKey("theme_mode")
        val READER_MODE = androidx.datastore.preferences.core.stringPreferencesKey("reader_mode")
        val NH_SAVED_IDS = stringSetPreferencesKey("nh_saved_ids")
        val NH_SAVED_DATA = androidx.datastore.preferences.core.stringPreferencesKey("nh_saved_data")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val SUBSCRIBED_IDS = stringSetPreferencesKey("subscribed_manga_ids")
        val INCOGNITO_MODE = booleanPreferencesKey("incognito_mode")
        val AUTO_SCROLL_SPEED = androidx.datastore.preferences.core.floatPreferencesKey("auto_scroll_speed")
        val DUAL_PAGE_MODE = booleanPreferencesKey("dual_page_mode")
        val READING_STREAK_DAYS = androidx.datastore.preferences.core.intPreferencesKey("reading_streak_days")
        val TOTAL_CHAPTERS_READ = androidx.datastore.preferences.core.intPreferencesKey("total_chapters_read")
        val TOTAL_READING_MINUTES = androidx.datastore.preferences.core.intPreferencesKey("total_reading_minutes")
        val LAST_READ_DAY = androidx.datastore.preferences.core.stringPreferencesKey("last_read_day")
        val READER_THEME = androidx.datastore.preferences.core.stringPreferencesKey("reader_theme")
        val READER_BRIGHTNESS_DIM = androidx.datastore.preferences.core.floatPreferencesKey("reader_brightness_dim")
        val PAGE_TURN_HAPTICS = booleanPreferencesKey("page_turn_haptics")
        val PAGE_TURN_SOUND = booleanPreferencesKey("page_turn_sound")
        val SURPRISE_ROLLS_COUNT = androidx.datastore.preferences.core.intPreferencesKey("surprise_rolls_count")
        val CUSTOM_SHELVES_JSON = androidx.datastore.preferences.core.stringPreferencesKey("custom_shelves_json")
        val FEED_CONFIG_JSON = androidx.datastore.preferences.core.stringPreferencesKey("feed_config_json")
        val READER_EXP = androidx.datastore.preferences.core.intPreferencesKey("reader_exp")
        val PANEL_BOOKMARKS_JSON = androidx.datastore.preferences.core.stringPreferencesKey("panel_bookmarks_json")
        val ACTION_RUMBLE_ENABLED = booleanPreferencesKey("action_rumble_enabled")
        val EXCLUDED_TAGS = stringSetPreferencesKey("excluded_tags")
        val INCLUDED_TAGS = stringSetPreferencesKey("included_tags")
        val MIN_CHAPTER_COUNT = androidx.datastore.preferences.core.intPreferencesKey("min_chapter_count")
        val STATUS_FILTER = androidx.datastore.preferences.core.stringPreferencesKey("status_filter")
        val SOURCE_MANGADEX_ENABLED = booleanPreferencesKey("source_mangadex_enabled")
        val SOURCE_MANTA_ENABLED = booleanPreferencesKey("source_manta_enabled")
        val SOURCE_MANGATOON_ENABLED = booleanPreferencesKey("source_mangatoon_enabled")
        val SOURCE_MANHWATOON_ENABLED = booleanPreferencesKey("source_manhwatoon_enabled")
        val SOURCE_3D_ENABLED = booleanPreferencesKey("source_3d_enabled")
    }

    val sourceMangaDexEnabled: Flow<Boolean> = context.dataStore.data.map { it[SOURCE_MANGADEX_ENABLED] ?: true }
    val sourceMantaEnabled: Flow<Boolean> = context.dataStore.data.map { it[SOURCE_MANTA_ENABLED] ?: true }
    val sourceMangaToonEnabled: Flow<Boolean> = context.dataStore.data.map { it[SOURCE_MANGATOON_ENABLED] ?: true }
    val sourceManhwaToonEnabled: Flow<Boolean> = context.dataStore.data.map { it[SOURCE_MANHWATOON_ENABLED] ?: true }
    val source3dEnabled: Flow<Boolean> = context.dataStore.data.map { it[SOURCE_3D_ENABLED] ?: true }

    val isAgeVerified: Flow<Boolean> = context.dataStore.data.map { it[IS_AGE_VERIFIED] ?: true }
    val isAgeWarningDismissedPermanent: Flow<Boolean> = context.dataStore.data.map { it[AGE_WARNING_DISMISSED_PERMANENT] ?: false }
    val ratingSafe: Flow<Boolean> = context.dataStore.data.map { it[RATING_SAFE] ?: true }
    val ratingSuggestive: Flow<Boolean> = context.dataStore.data.map { it[RATING_SUGGESTIVE] ?: true }
    val ratingErotica: Flow<Boolean> = context.dataStore.data.map { it[RATING_EROTICA] ?: true }
    val ratingPornographic: Flow<Boolean> = context.dataStore.data.map { it[RATING_PORNOGRAPHIC] ?: true }
    val themeMilfStepmother: Flow<Boolean> = context.dataStore.data.map { it[THEME_MILF_STEPMOTHER] ?: false }
    val bookmarkedIds: Flow<Set<String>> = context.dataStore.data.map { it[BOOKMARKED_IDS] ?: emptySet() }
    val favoriteIds: Flow<Set<String>> = context.dataStore.data.map { it[FAVORITE_IDS] ?: emptySet() }
    val subscribedIds: Flow<Set<String>> = context.dataStore.data.map { it[SUBSCRIBED_IDS] ?: emptySet() }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    val incognitoMode: Flow<Boolean> = context.dataStore.data.map { it[INCOGNITO_MODE] ?: false }
    val autoScrollSpeed: Flow<Float> = context.dataStore.data.map { it[AUTO_SCROLL_SPEED] ?: 1.0f }
    val dualPageMode: Flow<Boolean> = context.dataStore.data.map { it[DUAL_PAGE_MODE] ?: false }
    val readingStreakDays: Flow<Int> = context.dataStore.data.map { it[READING_STREAK_DAYS] ?: 3 }
    val totalChaptersRead: Flow<Int> = context.dataStore.data.map { it[TOTAL_CHAPTERS_READ] ?: 24 }
    val totalReadingMinutes: Flow<Int> = context.dataStore.data.map { it[TOTAL_READING_MINUTES] ?: 380 }
    val nhSavedIds: Flow<Set<String>> = context.dataStore.data.map { it[NH_SAVED_IDS] ?: emptySet() }
    val nhSavedData: Flow<String> = context.dataStore.data.map { it[NH_SAVED_DATA] ?: "[]" }
    val themeMode: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "DARK" }
    val readerMode: Flow<String> = context.dataStore.data.map { it[READER_MODE] ?: "VERTICAL" }
    val readerTheme: Flow<String> = context.dataStore.data.map { it[READER_THEME] ?: "DARK" }
    val readerBrightnessDim: Flow<Float> = context.dataStore.data.map { it[READER_BRIGHTNESS_DIM] ?: 0.0f }
    val pageTurnHaptics: Flow<Boolean> = context.dataStore.data.map { it[PAGE_TURN_HAPTICS] ?: true }
    val pageTurnSound: Flow<Boolean> = context.dataStore.data.map { it[PAGE_TURN_SOUND] ?: true }
    val surpriseRollsCount: Flow<Int> = context.dataStore.data.map { it[SURPRISE_ROLLS_COUNT] ?: 0 }
    val customShelvesJson: Flow<String> = context.dataStore.data.map { it[CUSTOM_SHELVES_JSON] ?: "{}" }
    val feedConfigJson: Flow<String> = context.dataStore.data.map { it[FEED_CONFIG_JSON] ?: "[]" }
    val readerExp: Flow<Int> = context.dataStore.data.map { it[READER_EXP] ?: 420 }
    val panelBookmarksJson: Flow<String> = context.dataStore.data.map { it[PANEL_BOOKMARKS_JSON] ?: "[]" }
    val actionRumbleEnabled: Flow<Boolean> = context.dataStore.data.map { it[ACTION_RUMBLE_ENABLED] ?: true }
    val excludedTags: Flow<Set<String>> = context.dataStore.data.map { it[EXCLUDED_TAGS] ?: emptySet() }
    val includedTags: Flow<Set<String>> = context.dataStore.data.map { it[INCLUDED_TAGS] ?: emptySet() }
    val minChapterCount: Flow<Int> = context.dataStore.data.map { it[MIN_CHAPTER_COUNT] ?: 0 }
    val statusFilter: Flow<String> = context.dataStore.data.map { it[STATUS_FILTER] ?: "ALL" }

    suspend fun addExp(amount: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[READER_EXP] ?: 420
            prefs[READER_EXP] = current + amount
        }
    }

    suspend fun setPanelBookmarksJson(json: String) {
        context.dataStore.edit { it[PANEL_BOOKMARKS_JSON] = json }
    }

    suspend fun setActionRumbleEnabled(enabled: Boolean) {
        context.dataStore.edit { it[ACTION_RUMBLE_ENABLED] = enabled }
    }

    suspend fun setSmartTagFilters(included: Set<String>, excluded: Set<String>, minChapters: Int) {
        context.dataStore.edit { prefs ->
            prefs[INCLUDED_TAGS] = included
            prefs[EXCLUDED_TAGS] = excluded
            prefs[MIN_CHAPTER_COUNT] = minChapters
        }
    }

    suspend fun setReaderTheme(theme: String) {
        context.dataStore.edit { it[READER_THEME] = theme }
    }

    suspend fun setReaderBrightnessDim(dim: Float) {
        context.dataStore.edit { it[READER_BRIGHTNESS_DIM] = dim }
    }

    suspend fun setPageTurnHaptics(enabled: Boolean) {
        context.dataStore.edit { it[PAGE_TURN_HAPTICS] = enabled }
    }

    suspend fun setPageTurnSound(enabled: Boolean) {
        context.dataStore.edit { it[PAGE_TURN_SOUND] = enabled }
    }

    suspend fun incrementSurpriseRolls() {
        context.dataStore.edit { prefs ->
            val count = prefs[SURPRISE_ROLLS_COUNT] ?: 0
            prefs[SURPRISE_ROLLS_COUNT] = count + 1
        }
    }

    suspend fun setCustomShelvesJson(json: String) {
        context.dataStore.edit { it[CUSTOM_SHELVES_JSON] = json }
    }

    suspend fun setFeedConfigJson(json: String) {
        context.dataStore.edit { it[FEED_CONFIG_JSON] = json }
    }

    suspend fun toggleSubscription(mangaId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[SUBSCRIBED_IDS]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(mangaId)) {
                current.remove(mangaId)
            } else {
                current.add(mangaId)
            }
            prefs[SUBSCRIBED_IDS] = current
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setIncognitoMode(enabled: Boolean) {
        context.dataStore.edit { it[INCOGNITO_MODE] = enabled }
    }

    suspend fun setAutoScrollSpeed(speed: Float) {
        context.dataStore.edit { it[AUTO_SCROLL_SPEED] = speed }
    }

    suspend fun setDualPageMode(enabled: Boolean) {
        context.dataStore.edit { it[DUAL_PAGE_MODE] = enabled }
    }

    suspend fun recordChapterRead(minutesSpent: Int = 5) {
        context.dataStore.edit { prefs ->
            val currentChapters = prefs[TOTAL_CHAPTERS_READ] ?: 24
            val currentMinutes = prefs[TOTAL_READING_MINUTES] ?: 380
            prefs[TOTAL_CHAPTERS_READ] = currentChapters + 1
            prefs[TOTAL_READING_MINUTES] = currentMinutes + minutesSpent

            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val lastDay = prefs[LAST_READ_DAY]
            val currentStreak = prefs[READING_STREAK_DAYS] ?: 3
            if (lastDay == null || lastDay != today) {
                prefs[READING_STREAK_DAYS] = currentStreak + 1
                prefs[LAST_READ_DAY] = today
            }
        }
    }

    suspend fun toggleNhSaved(id: String, galleryJson: String? = null) {
        context.dataStore.edit { prefs ->
            val currentIds = prefs[NH_SAVED_IDS]?.toMutableSet() ?: mutableSetOf()
            if (currentIds.contains(id)) {
                currentIds.remove(id)
            } else {
                currentIds.add(id)
            }
            prefs[NH_SAVED_IDS] = currentIds
        }
    }

    suspend fun setReaderMode(mode: String) {
        context.dataStore.edit { it[READER_MODE] = mode }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun setAgeVerified(verified: Boolean) {
        context.dataStore.edit { it[IS_AGE_VERIFIED] = verified }
    }

    suspend fun setAgeWarningDismissedPermanent(dismissed: Boolean) {
        context.dataStore.edit { it[AGE_WARNING_DISMISSED_PERMANENT] = dismissed }
    }

    suspend fun toggleBookmark(mangaId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[BOOKMARKED_IDS]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(mangaId)) {
                current.remove(mangaId)
            } else {
                current.add(mangaId)
            }
            prefs[BOOKMARKED_IDS] = current
        }
    }

    suspend fun toggleFavorite(mangaId: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[FAVORITE_IDS]?.toMutableSet() ?: mutableSetOf()
            if (current.contains(mangaId)) {
                current.remove(mangaId)
            } else {
                current.add(mangaId)
            }
            prefs[FAVORITE_IDS] = current
        }
    }

    suspend fun setRating(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, value: Boolean) {
        context.dataStore.edit { it[key] = value }
    }

    suspend fun setSourceEnabled(key: androidx.datastore.preferences.core.Preferences.Key<Boolean>, enabled: Boolean) {
        context.dataStore.edit { it[key] = enabled }
    }
}
