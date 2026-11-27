package com.dswork.list.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dswork.list.BuildConfig
import com.dswork.list.R
import com.dswork.list.data.NotificationPrefs
import com.dswork.list.data.UpdateChecker

/**
 * 版本更新推送：
 * - App 启动时和每天一次的后台任务里检查 GitHub Release；
 * - 12 小时内最多查一次，同一个新版本只提醒一次；
 * - 开关在「通知设置 → 版本更新」里，默认开启。
 */
object UpdateNotifier {

    const val CHANNEL_ID = "app_updates"
    const val CHANNEL_NAME = "版本更新"
    private const val PREFS = "update_notify_prefs"
    private const val KEY_LAST_CHECK = "last_check"
    private const val KEY_LAST_VERSION = "last_notified_version"
    private const val CHECK_INTERVAL_MS = 12L * 60 * 60 * 1000

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "发现新版本时推送到通知栏"
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    /** 每天一次的检查闹钟（不精确，系统会合并唤醒，省电）。 */
    fun scheduleDailyCheck(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val pending = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            Intent(appContext, UpdateCheckReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 6L * 60 * 60 * 1000,
            AlarmManager.INTERVAL_DAY,
            pending,
        )
    }

    /** 后台检查版本；节流、去重后按需发通知。 */
    fun checkAndNotify(context: Context) {
        // 商店渠道由应用商店负责更新；这里的自更新通道整体关闭。
        if (!BuildConfig.SELF_UPDATE) return
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(KEY_LAST_CHECK, 0L) < CHECK_INTERVAL_MS) return
        prefs.edit().putLong(KEY_LAST_CHECK, now).apply()

        if (!NotificationPrefs(appContext).updateNotifyEnabled) return

        Thread {
            val info = UpdateChecker.check()
            val latest = info.latestVersion ?: return@Thread
            val current = currentVersionName(appContext) ?: return@Thread
            if (UpdateChecker.compareVersions(latest, current) <= 0) return@Thread
            if (prefs.getString(KEY_LAST_VERSION, null) == latest) return@Thread

            prefs.edit().putString(KEY_LAST_VERSION, latest).apply()
            postUpdateNotification(context = appContext, version = latest)
        }.start()
    }

    private fun postUpdateNotification(context: Context, version: String) {
        ensureChannel(context)
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return

        val openApp = Intent(context, com.dswork.list.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_UPDATE_VERSION, version)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val installIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE + 1,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("List 有新版本 v$version")
            .setContentText("点按下载并安装更新")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .addAction(R.drawable.ic_bell, "立即更新", installIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(REQUEST_CODE, notification)
        }
    }

    private fun currentVersionName(context: Context): String? = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull()

    private const val REQUEST_CODE = 9527

    /** 通知点开后传给 MainActivity 的版本号。 */
    const val EXTRA_UPDATE_VERSION = "extra_update_version"
}
