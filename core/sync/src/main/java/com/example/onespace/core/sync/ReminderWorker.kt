package com.example.onespace.core.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.onespace.domain.session.Reminder
import com.example.onespace.domain.session.ReminderPolicy
import com.example.onespace.domain.session.repository.MissionRepository
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Repositories the worker reads; resolved from the app's Hilt graph. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun preferences(): SessionPreferencesRepository
    fun missions(): MissionRepository
}

/**
 * F-COM-12: wakes about once an hour, asks [ReminderPolicy] whether a reminder is due and posts at
 * most one notification of each kind per day. All decisions live in the domain policy; this class
 * only reads state, remembers what was sent today and talks to the notification system.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deps = EntryPointAccessors.fromApplication(applicationContext, ReminderEntryPoint::class.java)
        val today = LocalDate.now().toString()
        val prefs = deps.preferences().preferences.first()
        deps.missions().ensureTodayMissions(today)
        val missions = deps.missions().missionsFor(today).first()
        val sent = applicationContext.getSharedPreferences(SENT_PREFS, Context.MODE_PRIVATE)

        val reminder = ReminderPolicy.decide(
            hour = LocalTime.now().hour,
            today = today,
            notificationsEnabled = prefs.notificationsEnabled,
            streakReminder = prefs.streakReminder,
            quietHours = prefs.quietHours,
            missions = missions,
            lastMissionsReminder = sent.getString(KEY_MISSIONS, null),
            lastStreakReminder = sent.getString(KEY_STREAK, null)
        ) ?: return Result.success()

        if (show(applicationContext, reminder)) {
            sent.edit().putString(if (reminder is Reminder.Streak) KEY_STREAK else KEY_MISSIONS, today).apply()
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "daily-reminders"
        private const val SENT_PREFS = "onespace_reminders"
        private const val KEY_MISSIONS = "missions_sent_on"
        private const val KEY_STREAK = "streak_sent_on"
        const val CHANNEL_ID = "reminders"
        /** Extra on the launch intent: the app opens Home, where today's missions are shown. */
        const val EXTRA_OPEN_SECTION = "open_section"

        /** Idempotent: safe to call on every app start. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** Returns false when the user has not granted the notification permission (Android 13+). */
        internal fun show(context: Context, reminder: Reminder): Boolean {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return false
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Nhắc nhiệm vụ", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Nhắc nhiệm vụ hằng ngày và giữ chuỗi ngày" }
            )
            val names = reminder.openMissions.joinToString(", ") { it.title }
            val (title, text) = when (reminder) {
                is Reminder.Streak -> "Giữ chuỗi ngày của bạn" to "Hôm nay bạn chưa làm nhiệm vụ nào: $names"
                is Reminder.Missions -> "Nhiệm vụ hôm nay" to "Còn lại: $names"
            }
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?.putExtra(EXTRA_OPEN_SECTION, "HOME")
            val tap = launch?.let {
                PendingIntent.getActivity(context, 1, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_reminder)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .apply { if (tap != null) setContentIntent(tap) }
                .build()
            return runCatching {
                NotificationManagerCompat.from(context).notify(if (reminder is Reminder.Streak) 2 else 1, notification)
            }.isSuccess
        }
    }
}
