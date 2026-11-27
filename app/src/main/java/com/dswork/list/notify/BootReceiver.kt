package com.dswork.list.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dswork.list.BuildConfig
import com.dswork.list.data.NotificationPrefs
import com.dswork.list.data.TodoStore

/** 重启后 AlarmManager 安排会全部丢失，这里按本地数据重新排一遍。 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val appContext = context.applicationContext
        val store = TodoStore(appContext)
        val prefs = NotificationPrefs(appContext)
        NotificationScheduler.sync(appContext, store.items, prefs)
        if (BuildConfig.SELF_UPDATE) {
            UpdateNotifier.ensureChannel(appContext)
            UpdateNotifier.scheduleDailyCheck(appContext)
        }
    }
}
