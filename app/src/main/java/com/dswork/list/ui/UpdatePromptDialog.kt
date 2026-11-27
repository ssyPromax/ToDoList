package com.dswork.list.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dswork.list.notify.ApkUpdater
import com.dswork.list.ui.theme.LocalListPalette

/** 发现新版本后的更新提示：下载 → 校验 → 打开系统安装器。 */
@Composable
fun UpdatePromptDialog(version: String, onDismiss: () -> Unit) {
    val palette = LocalListPalette.current
    val context = LocalContext.current
    val canInstall = remember { ApkUpdater.canInstall(context) }
    val downloading = ApkUpdater.downloading
    val progress = ApkUpdater.progress
    val message = ApkUpdater.message

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = {
            Text(
                text = "发现新版本 v$version",
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary,
            )
        },
        text = {
            Column {
                Text(
                    text = "点「立即更新」会下载安装包并打开系统安装界面，覆盖安装后数据保留。",
                    fontSize = 13.sp,
                    color = palette.textSecondary,
                )
                if (!canInstall) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "首次更新需要允许「安装未知应用」，点下方按钮去授权，回来后再次更新。",
                        fontSize = 12.sp,
                        color = palette.danger,
                    )
                }
                if (downloading) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progress ?: 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = palette.accent,
                        trackColor = palette.border,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${((progress ?: 0f) * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                    )
                }
                message?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(text = it, fontSize = 12.sp, color = palette.textSecondary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!ApkUpdater.canInstall(context)) {
                        ApkUpdater.openUnknownSourcesSettings(context)
                    } else {
                        ApkUpdater.start(context, version)
                    }
                },
                enabled = !downloading,
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
            ) {
                Text(if (ApkUpdater.canInstall(context)) "立即更新" else "去授权")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("稍后", color = palette.textSecondary)
            }
        },
    )
}
