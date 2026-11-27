package com.dswork.list.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dswork.list.BuildConfig
import com.dswork.list.R
import com.dswork.list.data.UpdateChecker
import com.dswork.list.notify.ApkUpdater
import com.dswork.list.ui.theme.LocalListPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 关于：应用信息 + GitHub 仓库地址 + 检查更新（读取 GitHub Releases）。 */
@Composable
fun AboutDialog(versionName: String, onDismiss: () -> Unit) {
    val palette = LocalListPalette.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<UpdateChecker.Info?>(null) }

    val latest = result?.latestVersion
    val hasUpdate = latest != null && UpdateChecker.compareVersions(latest, versionName) > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.card,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher),
                    contentDescription = "List 图标",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(13.dp)),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "List",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                    )
                    Text(
                        text = "版本 $versionName",
                        fontSize = 12.sp,
                        color = palette.textSecondary,
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    text = "一个极简的本地待办 / 日程应用，数据只保存在本机，离线可用。",
                    fontSize = 13.sp,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "GitHub 仓库",
                    fontSize = 12.sp,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = UpdateChecker.REPO_URL,
                    fontSize = 13.sp,
                    color = palette.accent,
                    modifier = Modifier.clickable {
                        uriHandler.openUri(UpdateChecker.REPO_URL)
                    },
                )
                if (BuildConfig.SELF_UPDATE) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            if (checking) return@OutlinedButton
                            checking = true
                            result = null
                            scope.launch {
                                val info = withContext(Dispatchers.IO) { UpdateChecker.check() }
                                result = info
                                checking = false
                            }
                        },
                        enabled = !checking,
                    ) {
                        if (checking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = palette.accent,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("检查中…", fontSize = 13.sp, color = palette.accent)
                        } else {
                            Text("检查更新", fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    val info = result
                    when {
                        info == null -> Unit
                        info.error != null -> Text(
                            text = info.error,
                            fontSize = 12.sp,
                            color = palette.danger,
                            modifier = Modifier.weight(1f),
                        )
                        hasUpdate -> Text(
                            text = "有新版 v$latest",
                            fontSize = 12.sp,
                            color = palette.accent,
                            modifier = Modifier.weight(1f),
                        )
                        else -> Text(
                            text = "已是最新版本",
                            fontSize = 12.sp,
                            color = palette.textSecondary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (hasUpdate) {
                    Spacer(Modifier.height(10.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = {
                                    if (!ApkUpdater.canInstall(context)) {
                                        ApkUpdater.openUnknownSourcesSettings(context)
                                    } else {
                                        result?.latestVersion?.let { ApkUpdater.start(context, it) }
                                    }
                                },
                                enabled = !ApkUpdater.downloading,
                                colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                            ) {
                                Text("立即更新", fontSize = 13.sp)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "查看 Release",
                                fontSize = 13.sp,
                                color = palette.accent,
                                modifier = Modifier.clickable {
                                    uriHandler.openUri(result?.releaseUrl ?: UpdateChecker.RELEASES_URL)
                                },
                            )
                        }
                        val status = ApkUpdater.message
                        if (status != null) {
                            Spacer(Modifier.height(6.dp))
                            Text(text = status, fontSize = 11.sp, color = palette.textSecondary)
                        }
                        val notes = result?.releaseNotes?.trim()
                        if (!notes.isNullOrEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = notes.take(240),
                                fontSize = 11.sp,
                                color = palette.textSecondary,
                            )
                        }
                    }
                }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = palette.textSecondary)
            }
        },
    )
}
