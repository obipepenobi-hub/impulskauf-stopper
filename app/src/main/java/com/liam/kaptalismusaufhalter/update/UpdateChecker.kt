package com.liam.kaptalismusaufhalter.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val versionName: String,
    val releaseNotes: String,
    val apkDownloadUrl: String,
    val releaseUrl: String
)

/**
 * Checks the GitHub Releases API for a newer published version. No auth token is used,
 * so this relies on GitHub's unauthenticated rate limit (60 req/h per IP) — fine for a
 * manual "check for update" tap, not for polling.
 */
object UpdateChecker {

    suspend fun checkForUpdate(owner: String, repo: String, currentVersionName: String): UpdateInfo? =
        withContext(Dispatchers.IO) {
            val url = URL("https://api.github.com/repos/$owner/$repo/releases/latest")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)

                val tagName = json.optString("tag_name").removePrefix("v")
                if (tagName.isBlank() || !isNewer(tagName, currentVersionName)) return@withContext null

                val assets = json.optJSONArray("assets")
                var apkUrl: String? = null
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name")
                        if (name.endsWith(".apk")) {
                            apkUrl = asset.optString("browser_download_url")
                            break
                        }
                    }
                }
                val apk = apkUrl ?: return@withContext null

                UpdateInfo(
                    versionName = tagName,
                    releaseNotes = json.optString("body"),
                    apkDownloadUrl = apk,
                    releaseUrl = json.optString("html_url")
                )
            } catch (e: Exception) {
                null
            } finally {
                connection.disconnect()
            }
        }

    private fun isNewer(remote: String, current: String): Boolean {
        val r = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, c.size)) {
            val rv = r.getOrElse(i) { 0 }
            val cv = c.getOrElse(i) { 0 }
            if (rv != cv) return rv > cv
        }
        return false
    }
}
