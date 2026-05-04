package com.openclaw.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openclaw.app.OpenClawApp
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = (application as OpenClawApp).settingsRepository

    val apiEndpoint = settings.apiEndpoint.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val apiKey = settings.apiKey.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")
    val model = settings.model.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "gpt-4o-mini")
    val systemPrompt = settings.systemPrompt.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "你是 OpenClaw，一个智能 AI 助手。请用中文回复。"
    )
    val darkMode = settings.darkMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")
    val dynamicColor = settings.dynamicColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun saveApiEndpoint(value: String) {
        viewModelScope.launch { settings.saveApiEndpoint(value) }
    }

    fun saveApiKey(value: String) {
        viewModelScope.launch { settings.saveApiKey(value) }
    }

    fun saveModel(value: String) {
        viewModelScope.launch { settings.saveModel(value) }
    }

    fun saveSystemPrompt(value: String) {
        viewModelScope.launch { settings.saveSystemPrompt(value) }
    }

    fun saveDarkMode(value: String) {
        viewModelScope.launch { settings.saveDarkMode(value) }
    }

    fun saveDynamicColor(value: Boolean) {
        viewModelScope.launch { settings.saveDynamicColor(value) }
    }
}
