package com.openclaw.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "openclaw_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val API_ENDPOINT = stringPreferencesKey("api_endpoint")
        private val API_KEY = stringPreferencesKey("api_key")
        private val MODEL = stringPreferencesKey("model")
        private val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        private val DARK_MODE = stringPreferencesKey("dark_mode")
        private val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }

    val apiEndpoint: Flow<String> = context.dataStore.data.map { it[API_ENDPOINT] ?: "" }
    val apiKey: Flow<String> = context.dataStore.data.map { it[API_KEY] ?: "" }
    val model: Flow<String> = context.dataStore.data.map { it[MODEL] ?: "gpt-4o-mini" }
    val systemPrompt: Flow<String> = context.dataStore.data.map {
        it[SYSTEM_PROMPT] ?: "你是 OpenClaw，一个智能 AI 助手。请用中文回复。"
    }
    val darkMode: Flow<String> = context.dataStore.data.map { it[DARK_MODE] ?: "system" }
    val dynamicColor: Flow<Boolean> = context.dataStore.data.map { it[DYNAMIC_COLOR] ?: true }

    suspend fun saveApiEndpoint(endpoint: String) {
        context.dataStore.edit { it[API_ENDPOINT] = endpoint }
    }

    suspend fun saveApiKey(key: String) {
        context.dataStore.edit { it[API_KEY] = key }
    }

    suspend fun saveModel(model: String) {
        context.dataStore.edit { it[MODEL] = model }
    }

    suspend fun saveSystemPrompt(prompt: String) {
        context.dataStore.edit { it[SYSTEM_PROMPT] = prompt }
    }

    suspend fun saveDarkMode(mode: String) {
        context.dataStore.edit { it[DARK_MODE] = mode }
    }

    suspend fun saveDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    }
}
