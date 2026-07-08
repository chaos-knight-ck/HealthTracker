package com.example.healthtracker

import android.app.Application
import com.example.healthtracker.data.AppDatabase
import com.example.healthtracker.data.SettingsManager

class HealthTrackerApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val settingsManager: SettingsManager by lazy { SettingsManager(this) }
}
