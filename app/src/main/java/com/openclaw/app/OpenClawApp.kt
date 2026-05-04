package com.openclaw.app

import android.app.Application
import com.openclaw.app.data.repository.SettingsRepository

class OpenClawApp : Application() {

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
    }
}
