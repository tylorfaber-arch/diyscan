package dev.diyscan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** A newer build published on GitHub Releases. */
data class UpdateInfo(val build: Int, val title: String, val notes: String, val apkUrl: String, val pageUrl: String)

/**
 * Checks GitHub Releases for a build newer than the installed one.
 * CI tags each release "build-N" and stamps N into versionCode, so comparing is just N > versionCode.
 * Only talks to api.github.com; sends no device or vehicle data.
 */
object UpdateChecker {
    private const val LATEST = "https://api.github.com/repos/tylorfaber-arch/diyscan/releases/latest"

    /** Tag "build-12" → 12. Anything else → null. */
    fun buildNumber(tag: String): Int? = tag.removePrefix("build-").toIntOrNull()?.takeIf { tag.startsWith("build-") }

    /** Returns the release if it is newer than [installed], null if up to date. Throws on network errors. */
    suspend fun check(installed: Int): UpdateInfo? = withContext(Dispatchers.IO) {
        val c = (URL(LATEST).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000; readTimeout = 8_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "DIY-Scan-Android")
        }
        try {
            if (c.responseCode != 200) throw java.io.IOException("GitHub answered ${c.responseCode}")
            val j = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
            parse(j, installed)
        } finally { c.disconnect() }
    }

    fun parse(j: JSONObject, installed: Int): UpdateInfo? {
        val build = buildNumber(j.optString("tag_name")) ?: return null
        if (build <= installed) return null
        val assets = j.optJSONArray("assets")
        var apk: String? = null
        if (assets != null) for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            if (a.optString("name").endsWith(".apk")) { apk = a.optString("browser_download_url"); break }
        }
        val page = j.optString("html_url")
        return UpdateInfo(
            build = build,
            title = j.optString("name").ifBlank { "Build $build" },
            notes = j.optString("body").trim(),
            apkUrl = apk ?: page,
            pageUrl = page,
        )
    }
}
