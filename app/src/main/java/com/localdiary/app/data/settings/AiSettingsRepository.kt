package com.localdiary.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.localdiary.app.model.AppStorageSettings
import com.localdiary.app.model.AiEndpointConfig
import com.localdiary.app.model.StorageMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.aiSettingsDataStore by preferencesDataStore("ai_settings")

class AiSettingsRepository(
    private val context: Context,
) {
    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            "secure_ai_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    val configFlow: Flow<AiEndpointConfig> = context.aiSettingsDataStore.data.map { prefs ->
        AiEndpointConfig(
            baseUrl = prefs[BASE_URL] ?: "",
            apiKey = encryptedPrefs.getString(API_KEY, "").orEmpty(),
            model = prefs[MODEL] ?: "",
            requestTimeoutSeconds = prefs[TIMEOUT] ?: 90,
            supportsVision = prefs[SUPPORTS_VISION] ?: false,
            imageModelEnabled = prefs[IMAGE_MODEL_ENABLED] ?: false,
            imageBaseUrl = prefs[IMAGE_BASE_URL] ?: "",
            imageApiKey = encryptedPrefs.getString(IMAGE_API_KEY, "").orEmpty(),
            imageModel = prefs[IMAGE_MODEL] ?: "",
            emotionPromptTemplate = prefs[EMOTION_PROMPT_TEMPLATE]
                ?: AiEndpointConfig.DEFAULT_EMOTION_PROMPT_TEMPLATE,
        )
    }

    val storageFlow: Flow<AppStorageSettings> = context.aiSettingsDataStore.data.map { prefs ->
        AppStorageSettings(
            mode = prefs[STORAGE_MODE]
                ?.let { runCatching { StorageMode.valueOf(it) }.getOrNull() }
                ?: StorageMode.APP_PRIVATE,
            treeUri = prefs[STORAGE_TREE_URI],
        )
    }

    suspend fun load(): AiEndpointConfig = configFlow.first()

    suspend fun loadStorageSettings(): AppStorageSettings = storageFlow.first()

    suspend fun save(config: AiEndpointConfig) {
        context.aiSettingsDataStore.edit { prefs ->
            prefs[BASE_URL] = config.baseUrl
            prefs[MODEL] = config.model
            prefs[TIMEOUT] = config.requestTimeoutSeconds
            prefs[SUPPORTS_VISION] = config.supportsVision
            prefs[IMAGE_MODEL_ENABLED] = config.imageModelEnabled
            prefs[IMAGE_BASE_URL] = config.imageBaseUrl
            prefs[IMAGE_MODEL] = config.imageModel
            prefs[EMOTION_PROMPT_TEMPLATE] = config.emotionPromptTemplate
        }
        encryptedPrefs.edit()
            .putString(API_KEY, config.apiKey)
            .putString(IMAGE_API_KEY, config.imageApiKey)
            .apply()
    }

    suspend fun saveStorageSettings(settings: AppStorageSettings) {
        context.aiSettingsDataStore.edit { prefs ->
            prefs[STORAGE_MODE] = settings.mode.name
            if (settings.treeUri.isNullOrBlank()) {
                prefs.remove(STORAGE_TREE_URI)
            } else {
                prefs[STORAGE_TREE_URI] = settings.treeUri
            }
        }
    }

    private companion object {
        val BASE_URL = stringPreferencesKey("base_url")
        val MODEL = stringPreferencesKey("model")
        val TIMEOUT = intPreferencesKey("timeout")
        val SUPPORTS_VISION = booleanPreferencesKey("supports_vision")
        val IMAGE_MODEL_ENABLED = booleanPreferencesKey("image_model_enabled")
        val IMAGE_BASE_URL = stringPreferencesKey("image_base_url")
        val IMAGE_MODEL = stringPreferencesKey("image_model")
        val EMOTION_PROMPT_TEMPLATE = stringPreferencesKey("emotion_prompt_template")
        val STORAGE_MODE = stringPreferencesKey("storage_mode")
        val STORAGE_TREE_URI = stringPreferencesKey("storage_tree_uri")
        const val API_KEY = "api_key"
        const val IMAGE_API_KEY = "image_api_key"
    }
}
