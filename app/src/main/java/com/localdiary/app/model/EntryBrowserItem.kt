package com.localdiary.app.model

data class EntryBrowserItem(
    val meta: EntryMeta,
    val previewText: String = "",
    val latestEmotion: EntryEmotionSummary? = null,
)

data class EntryEmotionSummary(
    val labels: List<String>,
    val summary: String,
    val createdAt: Long,
)

enum class BrowserCategory(val label: String) {
    ALL("全部"),
    TAG("标签"),
    TIME("时间"),
    MOOD("心情"),
}

enum class BrowserTimeBucket(val label: String) {
    TODAY("今天"),
    THIS_WEEK("本周"),
    THIS_MONTH("本月"),
    EARLIER("更早"),
}
