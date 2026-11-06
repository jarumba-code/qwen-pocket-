package com.pocketqwen.app.viewmodel

/**
 * Current-information detection belongs to the app, not the local language model.
 * These explicit phrases route a query to Web Search before local generation begins.
 */
object QuerySearchTrigger {
    private val currentnessPhrases = listOf(
        "today", "tonight", "yesterday", "this morning", "last night", "right now",
        "currently", "current", "latest", "most recent", "recent", "this week",
        "this month", "this year", "as of", "breaking news", "news today", "news",
        "weather", "forecast", "exchange rate", "stock price", "crypto price",
        "price of", "market price", "live score", "match result", "election result",
        "release date", "latest version", "new release", "available now", "look up",
        "search online", "on the internet", "who is the current"
    )

    fun shouldSearch(query: String): Boolean {
        val normalized = query.lowercase().replace(Regex("\\s+"), " ").trim()
        if (normalized.isBlank()) return false
        val explicitOffline = listOf(
            "don't search", "do not search", "no web search", "offline only",
            "without internet", "don't use the internet"
        )
        if (explicitOffline.any { it in normalized }) return false
        return currentnessPhrases.any { phrase ->
            Regex("(^|[^a-z0-9])" + Regex.escape(phrase) + "([^a-z0-9]|$)").containsMatchIn(normalized)
        }
    }
}
