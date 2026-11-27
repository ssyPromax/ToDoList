package com.dswork.list.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 从 GitHub Releases 读取最新版本，用于「关于 → 检查更新」。
 *
 * 优先用网页的 `releases/latest` 重定向（不消耗 GitHub API 配额，60 次/小时的匿名
 * API 限流经常被同一出口 IP 打满）；拿到 Location 里的 tag 即可判断版本。
 * 重定向方式失败时再回退到 api.github.com，顺便取 release 说明。
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
        val viaRedirect = checkViaRedirect()
        if (viaRedirect.error == null) return viaRedirect
        val viaApi = checkViaApi()
        return if (viaApi.error == null) viaApi else viaRedirect
    }

    /** 读取 https://github.com/{owner}/{repo}/releases/latest 的 302 Location。 */
    private fun checkViaRedirect(): Info {
        val connection = try {
            (URL("$REPO_URL/releases/latest").openConnection() as HttpURLConnection)
        } catch (e: Exception) {
            return Info(error = "网络不可用")
        }

        return try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) List/App")

            when (val code = connection.responseCode) {
                in 300..399 -> {
                    val location = connection.getHeaderField("Location").orEmpty()
                    val tag = location.substringAfter("/tag/", "")
                        .substringBefore("?")
                        .substringBefore("#")
                        .trim()
                    if (tag.isNotBlank()) {
                        Info(
                            latestVersion = tag.removePrefix("v").removePrefix("V"),
                            releaseUrl = location.ifBlank { RELEASES_URL },
                        )
                    } else {
                        Info(error = "没解析到版本号")
                    }
                }
                404 -> Info(error = "仓库还没有发布版本")
                403 -> Info(error = "接口被限流，稍后再试")
                else -> Info(error = "HTTP $code")
            }
        } catch (e: Exception) {
            Info(error = "检查失败：${e.javaClass.simpleName}")
        } finally {
            connection.disconnect()
        }
    }

    /** 回退方案：GitHub REST API（附带 release 说明，但受 60 次/小时匿名限流）。 */
    private fun checkViaApi(): Info {
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
                403 -> Info(error = "接口被限流，稍后再试")
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
