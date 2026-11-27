package com.dswork.list.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 每天一次的版本检查闹钟。 */
class UpdateCheckReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        UpdateNotifier.checkAndNotify(context)
    }
}
