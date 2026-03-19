package com.localdiary.app.model

enum class StorageMode {
    APP_PRIVATE,
    SYSTEM_FOLDER,
}

data class AppStorageSettings(
    val mode: StorageMode = StorageMode.APP_PRIVATE,
    val treeUri: String? = null,
) {
    val hasConfiguredSystemFolder: Boolean
        get() = !treeUri.isNullOrBlank()
}
