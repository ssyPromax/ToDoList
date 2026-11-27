package com.dswork.list

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.dswork.list.data.NotificationPrefs
import com.dswork.list.data.TodoStore
import com.dswork.list.notify.NotificationScheduler
import com.dswork.list.ui.HistoryScreen
import com.dswork.list.ui.HomeScreen
import com.dswork.list.ui.theme.ListTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val store = TodoStore(applicationContext)
        val notifications = NotificationPrefs(applicationContext)

        NotificationScheduler.ensureChannel(applicationContext)
        // 事项增删改、完成状态变化后，重新安排提醒
        store.onChange = {
            NotificationScheduler.sync(applicationContext, store.items, notifications)
        }
        NotificationScheduler.sync(applicationContext, store.items, notifications)

        setContent {
            val systemDark = isSystemInDarkTheme()
            var dark by remember { mutableStateOf(store.darkPref ?: systemDark) }
            var showHistory by rememberSaveable { mutableStateOf(false) }
            val view = LocalView.current

            SideEffect {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }

            ListTheme(dark = dark) {
                if (showHistory) {
                    BackHandler { showHistory = false }
                    HistoryScreen(store = store, onBack = { showHistory = false })
                } else {
                    HomeScreen(
                        store = store,
                        notifications = notifications,
                        dark = dark,
                        onToggleTheme = {
                            dark = !dark
                            store.setDark(dark)
                        },
                        onOpenHistory = { showHistory = true },
                    )
                }
            }
        }
    }
}
