package com.dswork.list.notify

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import com.dswork.list.data.UpdateChecker
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 应用内更新：下载 GitHub Release 里的 APK → 校验（包名/版本/签名）→ 调起系统安装器。
 * 普通应用无法静默安装，最后一步由系统安装界面完成，用户点「安装」即覆盖升级，数据保留。
 */
object ApkUpdater {

    /** null 表示空闲；0f..1f 表示下载进度。 */
    var progress by mutableStateOf<Float?>(null)
        private set

    /** 给 UI 用的状态/错误文案。 */
    var message by mutableStateOf<String?>(null)
        private set

    var downloading by mutableStateOf(false)
        private set

    fun clearMessage() {
        message = null
    }

    /** 是否已被允许「安装未知应用」。 */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openUnknownSourcesSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** 开始下载并安装指定版本。 */
    fun start(context: Context, version: String) {
        if (downloading) return
        val appContext = context.applicationContext
        if (!canInstall(appContext)) {
            message = "请先允许 List 安装应用"
            openUnknownSourcesSettings(appContext)
            return
        }

        downloading = true
        progress = 0f
        message = "正在下载 v$version …"

        Thread {
            val target = File(appContext.cacheDir, "updates").apply { mkdirs() }
                .resolve("List-$version.apk")
            try {
                val error = download(appContext, version, target, fallbackVersion = version) { p ->
                    progress = p
                }
                if (error != null) {
                    message = error
                    return@Thread
                }
                val verifyError = verify(appContext, target)
                if (verifyError != null) {
                    message = verifyError
                    runCatching { target.delete() }
                    return@Thread
                }
                message = "下载完成，正在打开安装器…"
                install(appContext, target)
            } catch (e: Exception) {
                message = "更新失败：${e.javaClass.simpleName}"
            } finally {
                downloading = false
                progress = null
            }
        }.start()
    }

    private fun download(
        context: Context,
        version: String,
        target: File,
        fallbackVersion: String,
        onProgress: (Float) -> Unit,
    ): String? {
        var url = "https://github.com/${UpdateChecker.OWNER}/${UpdateChecker.REPO_NAME}" +
            "/releases/download/v$version/List-$version.apk"
        var connection = open(url)
        if (connection == null || connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
            connection?.disconnect()
            // 兜底：从 GitHub API 里找 apk 附件地址
            val apiUrl = findAssetUrl(fallbackVersion)
            if (apiUrl != null) {
                url = apiUrl
                connection = open(url)
            }
        }
        val conn = connection ?: return "下载地址不可用"
        return try {
            val code = conn.responseCode
            if (code !in 200..299) return "下载失败：HTTP $code"
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read: Int
                    var written = 0L
                    while (input.read(buffer).also { read = it } > 0) {
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0) onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            }
            if (target.length() < 1024) "下载的安装包不完整" else null
        } finally {
            conn.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection? = runCatching {
        (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "List-Android")
        }
    }.getOrNull()

    private fun findAssetUrl(version: String): String? = runCatching {
        val url = "https://api.github.com/repos/${UpdateChecker.OWNER}/${UpdateChecker.REPO_NAME}" +
            "/releases/tags/v$version"
        val conn = open(url) ?: return null
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        Regex("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.apk)\"")
            .find(body)?.groupValues?.get(1)
    }.getOrNull()

    /** 返回 null 表示校验通过，否则返回错误文案。 */
    fun verify(context: Context, file: File): String? {
        if (!file.isFile) return "安装包不存在"
        val pm = context.packageManager
        val flags = PackageManager.GET_SIGNATURES
        val archive = pm.getPackageArchiveInfo(file.absolutePath, flags) ?: return "安装包无法解析"
        if (archive.packageName != context.packageName) return "安装包包名不匹配，已阻止安装"
        val current = pm.getPackageInfo(context.packageName, 0)
        if (archive.versionCode <= current.versionCode) return "安装包版本不高于当前版本"
        val currentSigs = pm.getPackageInfo(context.packageName, flags).signatures
            ?.map { it.toCharsString() }?.toSet()
        val newSigs = archive.signatures?.map { it.toCharsString() }?.toSet()
        if (currentSigs == null || newSigs == null || currentSigs != newSigs) {
            return "签名校验不通过，已阻止安装"
        }
        return null
    }

    private fun install(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { message = "无法打开安装器：${it.javaClass.simpleName}" }
    }
}
