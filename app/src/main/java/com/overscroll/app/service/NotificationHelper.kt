package com.overscroll.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import com.overscroll.app.MainActivity
import com.overscroll.app.R

/**
 * Centralized notification channel creation and notification builders.
 * Used by OverlayBubbleService (Phase 3) and CounterForegroundService (Phase 4).
 *
 * Channels:
 * - COUNTER: Live count notification (low priority, no sound)
 * - OVERLAY: Minimal foreground service notification for the overlay (min priority)
 * - ALERTS: Health check alerts (default priority)
 */
object NotificationHelper {

    const val CHANNEL_COUNTER = "overscroll_counter_v3"
    const val CHANNEL_OVERLAY = "overscroll_overlay"
    const val CHANNEL_ALERTS = "overscroll_alerts"

    const val NOTIFICATION_ID_COUNTER = 1
    const val NOTIFICATION_ID_OVERLAY = 2
    const val NOTIFICATION_ID_ALERT = 3

    /**
     * Create all notification channels. Safe to call multiple times —
     * creating an existing channel is a no-op.
     */
    fun createChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val counterChannel = NotificationChannel(
            CHANNEL_COUNTER,
            context.getString(R.string.notification_channel_counter),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_counter_desc)
            setShowBadge(false)
        }

        val overlayChannel = NotificationChannel(
            CHANNEL_OVERLAY,
            "Overlay Service",
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            description = "Required notification for the overlay bubble"
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }

        val alertsChannel = NotificationChannel(
            CHANNEL_ALERTS,
            context.getString(R.string.notification_channel_alerts),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_alerts_desc)
        }

        nm.createNotificationChannels(listOf(counterChannel, overlayChannel, alertsChannel))
    }

    /**
     * Minimal foreground notification for the overlay service.
     * Uses IMPORTANCE_MIN channel so it's barely visible in the shade.
     */
    fun createOverlayNotification(context: Context): Notification {
        val pendingIntent = createMainActivityPendingIntent(context)

        return NotificationCompat.Builder(context, CHANNEL_OVERLAY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Overscroll")
            .setContentText("Overlay bubble active")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(pendingIntent)
            .build()
    }

    /**
     * Live count notification for the foreground counter service.
     * Updated frequently as the count changes.
     */
    fun createCounterNotification(context: Context, counts: Map<String, Int>): Notification {
        val pendingIntent = createMainActivityPendingIntent(context)
        val combinedCount = counts.values.sum()
        val icon = createCountIcon(context, combinedCount)

        val activeApps = counts.filterValues { it > 0 }.map { (pkg, _) ->
            com.overscroll.app.config.AppTrackerConfig.SUPPORTED_APPS.find { it.packageName == pkg }?.displayName ?: "App"
        }.joinToString(", ")

        val contentText = if (activeApps.isEmpty()) {
            "Overscroll is monitoring"
        } else {
            "Active: $activeApps"
        }

        return NotificationCompat.Builder(context, CHANNEL_COUNTER)
            .setSmallIcon(icon)
            .setContentTitle("$combinedCount Shorts/Reels today")
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    /**
     * Nudge notification fired when crossing a threshold.
     */
    fun sendNudgeNotification(context: Context, copy: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val snoozeIntent = createSnoozePendingIntent(context)
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Overscroll Check-in")
            .setContentText(copy)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(0, "Snooze nudges today", snoozeIntent)
            .build()
            
        nm.notify(NOTIFICATION_ID_ALERT, notification)
    }

    private fun createSnoozePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, NudgeActionReceiver::class.java).apply {
            action = "com.overscroll.app.ACTION_SNOOZE_NUDGES"
        }
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * PendingIntent that opens MainActivity when notification is tapped.
     */
    private fun createMainActivityPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Dynamically generates a small icon bitmap containing the count text.
     * This makes the number visible directly in the status bar when closed.
     */
    private fun createCountIcon(context: Context, count: Int): IconCompat {
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val text = if (count > 999) "999+" else count.toString()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = if (text.length > 2) 56f else 84f
            typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }

        val xPos = size / 2f
        val yPos = (size / 2f) - ((paint.descent() + paint.ascent()) / 2f)

        canvas.drawText(text, xPos, yPos, paint)

        return IconCompat.createWithBitmap(bitmap)
    }
}
