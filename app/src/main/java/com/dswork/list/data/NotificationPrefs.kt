package com.dswork.list.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 通知提醒设置：
 * - enabled 总开关；
 * - 每个未完成事项在截止前 24 小时、30 分钟，以及一个用户自定义的「提前 N 小时」各提醒一次。
 */
class NotificationPrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("notify_prefs", Context.MODE_PRIVATE)

    var enabled by mutableStateOf(prefs.getBoolean(KEY_ENABLED, false))
        private set

    var before24h by mutableStateOf(prefs.getBoolean(KEY_24H, true))
        private set

    var before30m by mutableStateOf(prefs.getBoolean(KEY_30M, true))
        private set

    /** 自定义提前量：小时数（1..24）。 */
    var customHours by mutableStateOf(
        prefs.getInt(KEY_CUSTOM_HOURS, DEFAULT_CUSTOM_HOURS).coerceIn(MIN_CUSTOM_HOURS, MAX_CUSTOM_HOURS)
    )
        private set

    /** 自定义提前量那条提醒是否开启。 */
    var customEnabled by mutableStateOf(prefs.getBoolean(KEY_CUSTOM_ENABLED, true))
        private set

    /** 发现新版本时是否推送通知（与待办提醒总开关相互独立）。 */
    var updateNotifyEnabled by mutableStateOf(prefs.getBoolean(KEY_UPDATE_NOTIFY, true))
        private set

    /** 当前开启的提前量（毫秒）。 */
    val enabledOffsets: List<Long>
        get() = buildList {
            if (before24h) add(HOURS_24)
            if (before30m) add(MINUTES_30)
            if (customEnabled) add(customHours * HOUR_MS)
        }

    fun updateEnabled(value: Boolean) {
        enabled = value
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun updateBefore24h(value: Boolean) {
        before24h = value
        prefs.edit().putBoolean(KEY_24H, value).apply()
    }

    fun updateBefore30m(value: Boolean) {
        before30m = value
        prefs.edit().putBoolean(KEY_30M, value).apply()
    }

    fun updateCustomEnabled(value: Boolean) {
        customEnabled = value
        prefs.edit().putBoolean(KEY_CUSTOM_ENABLED, value).apply()
    }

    fun updateCustomHours(hours: Int) {
        customHours = hours.coerceIn(MIN_CUSTOM_HOURS, MAX_CUSTOM_HOURS)
        prefs.edit().putInt(KEY_CUSTOM_HOURS, customHours).apply()
    }

    fun updateUpdateNotify(value: Boolean) {
        updateNotifyEnabled = value
        prefs.edit().putBoolean(KEY_UPDATE_NOTIFY, value).apply()
    }

    companion object {
        const val HOUR_MS = 60L * 60 * 1000
        const val MINUTE_MS = 60L * 1000

        const val HOURS_24 = 24 * HOUR_MS
        const val MINUTES_30 = 30 * MINUTE_MS

        const val MIN_CUSTOM_HOURS = 1
        const val MAX_CUSTOM_HOURS = 24
        const val DEFAULT_CUSTOM_HOURS = 2

        private const val KEY_ENABLED = "enabled"
        private const val KEY_24H = "before_24h"
        private const val KEY_30M = "before_30m"
        private const val KEY_CUSTOM_ENABLED = "custom_enabled"
        private const val KEY_CUSTOM_HOURS = "custom_hours"
        private const val KEY_UPDATE_NOTIFY = "update_notify"
    }
}
