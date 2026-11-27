package com.dswork.list.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 通知提醒设置：
 * - enabled 总开关；
 * - 每个事项在截止前 24 小时 / 12 小时 / 30 分钟各提醒一次（仅未完成的事项）。
 */
class NotificationPrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("notify_prefs", Context.MODE_PRIVATE)

    var enabled by mutableStateOf(prefs.getBoolean(KEY_ENABLED, false))
        private set

    var before24h by mutableStateOf(prefs.getBoolean(KEY_24H, true))
        private set

    var before12h by mutableStateOf(prefs.getBoolean(KEY_12H, true))
        private set

    var before30m by mutableStateOf(prefs.getBoolean(KEY_30M, true))
        private set

    /** 当前开启的提前量（毫秒）。 */
    val enabledOffsets: List<Long>
        get() = buildList {
            if (before24h) add(HOURS_24)
            if (before12h) add(HOURS_12)
            if (before30m) add(MINUTES_30)
        }

    fun updateEnabled(value: Boolean) {
        enabled = value
        prefs.edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun updateBefore24h(value: Boolean) {
        before24h = value
        prefs.edit().putBoolean(KEY_24H, value).apply()
    }

    fun updateBefore12h(value: Boolean) {
        before12h = value
        prefs.edit().putBoolean(KEY_12H, value).apply()
    }

    fun updateBefore30m(value: Boolean) {
        before30m = value
        prefs.edit().putBoolean(KEY_30M, value).apply()
    }

    companion object {
        const val HOURS_24 = 24L * 60 * 60 * 1000
        const val HOURS_12 = 12L * 60 * 60 * 1000
        const val MINUTES_30 = 30L * 60 * 1000

        private const val KEY_ENABLED = "enabled"
        private const val KEY_24H = "before_24h"
        private const val KEY_12H = "before_12h"
        private const val KEY_30M = "before_30m"
    }
}
