package com.dswork.list.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.dswork.list.data.NotificationPrefs
import com.dswork.list.data.Todo
import java.time.ZoneId

/**
 * 用 AlarmManager 为每个「未完成事项 × 开启的提前量」安排本地通知。
 * 同一份数据每次变化都全量重排：先取消上一轮登记过的 alarm，再按当前数据排新的。
 */
object NotificationScheduler {

    const val CHANNEL_ID = "todo_reminders"
    const val CHANNEL_NAME = "待办提醒"

    private const val SCHEDULE_PREFS = "notify_scheduled"
    private const val KEY_CODES = "codes"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "在事项截止前 24 小时 / 12 小时 / 30 分钟提醒"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    /** 根据当前事项与开关，重新安排全部提醒。 */
    fun sync(context: Context, todos: List<Todo>, prefs: NotificationPrefs) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val schedulePrefs = appContext.getSharedPreferences(SCHEDULE_PREFS, Context.MODE_PRIVATE)

        // 1. 取消上一轮登记的提醒
        schedulePrefs.getStringSet(KEY_CODES, emptySet()).orEmpty().forEach { raw ->
            val code = raw.toIntOrNull() ?: return@forEach
            alarmManager.cancel(reminderIntent(appContext, code, 0L, 0L))
        }
        schedulePrefs.edit().putStringSet(KEY_CODES, emptySet()).apply()

        if (!prefs.enabled) return

        ensureChannel(appContext)
        val offsets = prefs.enabledOffsets
        if (offsets.isEmpty()) return

        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val codes = mutableSetOf<String>()

        todos.forEach { todo ->
            if (todo.done) return@forEach
            val dueMillis = todo.date
                .atTime(todo.minutes / 60, todo.minutes % 60)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()

            offsets.forEachIndexed { index, offset ->
                val triggerAt = dueMillis - offset
                if (triggerAt <= now + 5_000) return@forEachIndexed
                val code = codeFor(todo.id, index)
                val pending = reminderIntent(appContext, code, todo.id, offset)
                try {
                    val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        alarmManager.canScheduleExactAlarms()
                    if (canExact) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pending,
                        )
                    } else {
                        alarmManager.setWindow(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            10L * 60 * 1000,
                            pending,
                        )
                    }
                    codes.add(code.toString())
                } catch (e: SecurityException) {
                    // 没有精确闹钟权限时退化为普通闹钟
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
                    codes.add(code.toString())
                }
            }
        }

        schedulePrefs.edit().putStringSet(KEY_CODES, codes).apply()
    }

    /** 取消某条事项的全部提醒（删除/完成时无需单独调用，sync 会全量重排）。 */
    fun cancelAll(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val schedulePrefs = appContext.getSharedPreferences(SCHEDULE_PREFS, Context.MODE_PRIVATE)
        schedulePrefs.getStringSet(KEY_CODES, emptySet()).orEmpty().forEach { raw ->
            val code = raw.toIntOrNull() ?: return@forEach
            alarmManager.cancel(reminderIntent(appContext, code, 0L, 0L))
        }
        schedulePrefs.edit().putStringSet(KEY_CODES, emptySet()).apply()
    }

    private fun codeFor(todoId: Long, offsetIndex: Int): Int =
        ((todoId and 0x0FFFFFFF) * 4 + offsetIndex).toInt()

    private fun reminderIntent(
        context: Context,
        code: Int,
        todoId: Long,
        offsetMillis: Long,
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            data = Uri.parse("list://reminder/$code")
            putExtra(ReminderReceiver.EXTRA_TODO_ID, todoId)
            putExtra(ReminderReceiver.EXTRA_OFFSET_MS, offsetMillis)
        }
        return PendingIntent.getBroadcast(
            context,
            code,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
