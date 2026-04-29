package com.localdiary.app.ui.navigation

object DiaryRoutes {
    const val TIMELINE = "timeline"
    const val BROWSER = "browser"
    const val EMOTION = "emotion"
    const val SETTINGS = "settings"
    const val EDITOR = "editor"
    const val VIEWER = "viewer"
    const val EMOTION_DETAIL = "emotion-detail"
    const val EMOTION_REPORTS = "emotion-reports"
    const val PSYCHOLOGY_CHAT = "psychology-chat"
    const val PSYCHOLOGY_PROFILE = "psychology-profile"

    fun editor(entryId: String) = "$EDITOR/$entryId"
    fun viewer(entryId: String) = "$VIEWER/$entryId"
    fun emotionDetail(entryId: String) = "$EMOTION_DETAIL/$entryId"
    fun psychologyChat(entryId: String) = "$PSYCHOLOGY_CHAT/$entryId"

    val topLevelRoutes = setOf(TIMELINE, BROWSER, EMOTION, SETTINGS)
}
