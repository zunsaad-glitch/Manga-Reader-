package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

object MangaNotificationManager {

    const val CHANNEL_NEW_DROPS = "channel_manga_drops"
    const val CHANNEL_SUBSCRIBED = "channel_subscribed_drops"
    const val CHANNEL_INSIGHTS = "channel_reading_insights"

    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val dropChannel = NotificationChannel(
                CHANNEL_NEW_DROPS,
                "🔥 New Manga Drops",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Get notified instantly when new chapters & hot series drop"
                enableVibration(true)
                enableLights(true)
            }

            val subscribedChannel = NotificationChannel(
                CHANNEL_SUBSCRIBED,
                "🔔 Subscribed Manga Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when your subscribed manga has a new chapter release"
                enableVibration(true)
                enableLights(true)
            }

            val insightsChannel = NotificationChannel(
                CHANNEL_INSIGHTS,
                "📊 Reading Insights & Streaks",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reading streak reminders and weekly summaries"
            }

            manager.createNotificationChannel(dropChannel)
            manager.createNotificationChannel(subscribedChannel)
            manager.createNotificationChannel(insightsChannel)
        }
    }

    suspend fun showMangaDropNotification(
        context: Context,
        mangaId: String,
        mangaTitle: String,
        chapterTitle: String,
        coverUrl: String? = null,
        isSubscribed: Boolean = false
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("extra_manga_id", mangaId)
                putExtra("extra_is_subscribed", isSubscribed)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                mangaId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val channelId = if (isSubscribed) CHANNEL_SUBSCRIBED else CHANNEL_NEW_DROPS
            val headerText = if (isSubscribed) "🔔 Subscribed Release: $mangaTitle" else "🔥 New Chapter Drop: $mangaTitle"
            val bodyText = "New Chapter Released: $chapterTitle. Tap to read now in HD!"

            var bitmap: Bitmap? = null
            if (!coverUrl.isNullOrBlank()) {
                withContext(Dispatchers.IO) {
                    try {
                        val url = URL(coverUrl)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.doInput = true
                        connection.connectTimeout = 4000
                        connection.readTimeout = 4000
                        connection.connect()
                        val input = connection.inputStream
                        bitmap = BitmapFactory.decodeStream(input)
                    } catch (_: Exception) {
                    }
                }
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_menu_agenda)
                .setContentTitle(headerText)
                .setContentText(bodyText)
                .setStyle(
                    if (bitmap != null) {
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(bitmap)
                            .setSummaryText(bodyText)
                    } else {
                        NotificationCompat.BigTextStyle().bigText(bodyText)
                    }
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                val notificationManager = NotificationManagerCompat.from(context)
                val notificationId = Random.nextInt(1000, 99999)
                notificationManager.notify(notificationId, builder.build())
            }
        } catch (_: Throwable) {
        }
    }

    suspend fun showTestDropNotification(context: Context) {
        showMangaDropNotification(
            context = context,
            mangaId = "reverend_insanity",
            mangaTitle = "Reverend Insanity",
            chapterTitle = "Chapter 2334 - The Gu Immortal Ascension [NEW DROP]",
            coverUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=600",
            isSubscribed = true
        )
    }

    suspend fun showAdultDropNotification(context: Context, title: String = "Sinful Lust") {
        showMangaDropNotification(
            context = context,
            mangaId = "451290",
            mangaTitle = title,
            chapterTitle = "Episode 42 - Uncensored Secret Encounter [NEW 18+ DROP]",
            coverUrl = null,
            isSubscribed = true
        )
    }
}
