package com.dswork.list.notify

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dswork.list.MainActivity
import com.dswork.list.R
import com.dswork.list.data.NotificationPrefs
import com.dswork.list.data.Todo
import com.dswork.list.data.TodoStore
import com.dswork.list.data.dateCn
import com.dswork.list.data.weekdayCn

/** 闹钟到点时弹出本地通知；发送前再核对一次：事项存在、未完成、总开关仍开着。 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val todoId = intent.getLongExtra(EXTRA_TODO_ID, -1L)
        if (todoId <= 0L) return

        val appContext = context.applicationContext
        val prefs = NotificationPrefs(appContext)
        if (!prefs.enabled) return

        val store = TodoStore(appContext)
        val todo = store.items.firstOrNull { it.id == todoId } ?: return
        if (todo.done) return

        val offset = intent.getLongExtra(EXTRA_OFFSET_MS, 0L)
        showNotification(appContext, todo, offset)
    }

    private fun showNotification(context: Context, todo: Todo, offsetMillis: Long) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        NotificationScheduler.ensureChannel(context)

        val head = when (offsetMillis) {
            NotificationPrefs.HOURS_24 -> "还有 24 小时"
            NotificationPrefs.HOURS_12 -> "还有 12 小时"
            NotificationPrefs.MINUTES_30 -> "还有 30 分钟"
            else -> "即将开始"
        }

        val openApp = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            todo.id.hashCode(),
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell)
            .setContentTitle("待办提醒 · $head")
            .setContentText("${todo.title}（${todo.date.dateCn()}${todo.date.weekdayCn()} ${todo.timeText}）")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${todo.title}\n${todo.date.dateCn()}${todo.date.weekdayCn()} ${todo.timeText} · $head"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(todo.id.hashCode(), notification)
        }
    }

    companion object {
        const val EXTRA_TODO_ID = "todo_id"
        const val EXTRA_OFFSET_MS = "offset_ms"
    }
}
