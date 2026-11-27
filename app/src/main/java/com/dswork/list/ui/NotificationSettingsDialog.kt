package com.dswork.list.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.dswork.list.data.NotificationPrefs
import com.dswork.list.ui.theme.LocalListPalette

/**
 * 通知提醒设置：总开关 + 三个提前量开关（24 小时 / 12 小时 / 30 分钟）。
 * 打开总开关时会请求 Android 13+ 的通知权限。
 */
@Composable
fun NotificationSettingsDialog(
    prefs: NotificationPrefs,
    onDismiss: () -> Unit,
    onChanged: () -> Unit,
) {
    val palette = LocalListPalette.current
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            prefs.updateEnabled(true)
            onChanged()
        } else {
            permissionDenied = true
        }
    }

    fun enableMaster(next: Boolean) {
        if (!next) {
            prefs.updateEnabled(false)
            onChanged()
            return
        }
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            permissionDenied = false
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            prefs.updateEnabled(true)
            onChanged()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = {
            Text(
                text = "通知提醒",
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                SettingSwitchRow(
                    title = "开启通知",
                    subtitle = "仅提醒未完成的事项",
                    checked = prefs.enabled,
                    onCheckedChange = { enableMaster(it) },
                )
                Spacer(Modifier.height(10.dp))
                Column {
                    Text(
                        text = "提醒时间",
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                        modifier = Modifier.padding(bottom = 2.dp),
                    )
                    SettingSwitchRow(
                        title = "提前 24 小时",
                        subtitle = "例如明天 9:00 的事项，今天 9:00 提醒",
                        checked = prefs.before24h,
                        enabled = prefs.enabled,
                        onCheckedChange = {
                            prefs.updateBefore24h(it)
                            onChanged()
                        },
                    )
                    SettingSwitchRow(
                        title = "提前 12 小时",
                        subtitle = "例如明天 9:00 的事项，今天 21:00 提醒",
                        checked = prefs.before12h,
                        enabled = prefs.enabled,
                        onCheckedChange = {
                            prefs.updateBefore12h(it)
                            onChanged()
                        },
                    )
                    SettingSwitchRow(
                        title = "提前 30 分钟",
                        subtitle = "即将开始时再提醒一次",
                        checked = prefs.before30m,
                        enabled = prefs.enabled,
                        onCheckedChange = {
                            prefs.updateBefore30m(it)
                            onChanged()
                        },
                    )
                }
                if (permissionDenied) {
                    Text(
                        text = "通知权限被拒绝，请到系统设置 → 应用 → List → 通知中开启。",
                        fontSize = 12.sp,
                        color = palette.danger,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                if (prefs.enabled && !androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                    Text(
                        text = "系统通知已关闭，请到系统设置里开启 List 的通知。",
                        fontSize = 12.sp,
                        color = palette.danger,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("完成", color = palette.textSecondary)
            }
        },
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val palette = LocalListPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) palette.textPrimary else palette.textSecondary,
            )
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = palette.textSecondary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.card,
                checkedTrackColor = palette.accent,
                uncheckedThumbColor = palette.textSecondary,
                uncheckedTrackColor = palette.border,
                disabledCheckedTrackColor = palette.accent.copy(alpha = 0.35f),
            ),
        )
    }
}
