package com.dswork.list.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 从 GitHub Releases 读取最新版本，用于「关于 → 检查更新」。
 * 不引入第三方网络库，直接用 HttpURLConnection。
 */
object UpdateChecker {

    const val OWNER = "ssyPromax"
    const val REPO_NAME = "ToDoList"
    const val REPO_URL = "https://github.com/ssyPromax/ToDoList"
    const val RELEASES_URL = "$REPO_URL/releases"

    data class Info(
        val latestVersion: String? = null,
        val releaseUrl: String? = null,
        val releaseNotes: String? = null,
        val error: String? = null,
    )

    fun check(): Info {
        val connection = try {
            (URL("https://api.github.com/repos/$OWNER/$REPO_NAME/releases/latest").openConnection() as HttpURLConnection)
        } catch (e: Exception) {
            return Info(error = "网络不可用")
        }

        return try {
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "List-Android")

            when (val code = connection.responseCode) {
                404 -> Info(error = "仓库还没有发布版本")
                in 200..299 -> {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(body)
                    val tag = json.optString("tag_name").trim().removePrefix("v").removePrefix("V")
                    Info(
                        latestVersion = tag.ifBlank { null },
                        releaseUrl = json.optString("html_url").ifBlank { RELEASES_URL },
                        releaseNotes = json.optString("body").ifBlank { null },
                    )
                }
                else -> Info(error = "HTTP $code")
            }
        } catch (e: Exception) {
            Info(error = "检查失败：${e.javaClass.simpleName}")
        } finally {
            connection.disconnect()
        }
    }

    /** 返回 latest 与 current 的比较：>0 表示有新版本。 */
    fun compareVersions(latest: String, current: String): Int {
        fun parts(v: String): List<Int> {
            val cleaned = v.trim().removePrefix("v").removePrefix("V")
                .split('-', '+').first()
            return cleaned.split('.').map { it.toIntOrNull() ?: 0 }
        }
        val a = parts(latest)
        val b = parts(current)
        val size = maxOf(a.size, b.size)
        for (i in 0 until size) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x - y
        }
        return 0
    }
}
