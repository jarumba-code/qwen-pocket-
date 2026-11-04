package com.pocketqwen.app.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class WebSearchResult(val title: String, val url: String, val snippet: String)

/** Optional retrieval. Normal local chat never calls this service. */
class WebSearchRepository(private val endpoint: String = SEARCH_ENDPOINT) {
    suspend fun search(query: String): List<WebSearchResult> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        require(clean.length in 2..400) { "Enter a search query between 2 and 400 characters." }
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use {
                it.write(JSONObject().put("query", clean).toString())
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val error = runCatching { JSONObject(body).optString("error") }.getOrNull()
                throw IOException(error?.takeIf { it.isNotBlank() } ?: "Web Search failed (HTTP $status).")
            }
            val results = JSONObject(body).optJSONArray("results") ?: return@withContext emptyList()
            buildList {
                for (i in 0 until minOf(results.length(), 4)) {
                    val item = results.optJSONObject(i) ?: continue
                    val title = item.optString("title").trim().take(180)
                    val url = item.optString("url").trim().take(1000)
                    val snippet = item.optString("snippet").trim().take(1200)
                    if (title.isNotBlank() && url.startsWith("https://") && snippet.isNotBlank()) add(WebSearchResult(title, url, snippet))
                }
            }
        } finally { connection.disconnect() }
    }
    companion object { const val SEARCH_ENDPOINT = "https://qwen-pocket.vercel.app/api/search" }
}
