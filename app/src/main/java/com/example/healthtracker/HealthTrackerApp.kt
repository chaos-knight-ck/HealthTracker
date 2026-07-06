package com.example.healthtracker

import android.app.Application
import com.example.healthtracker.data.AppDatabase

class HealthTrackerApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
}
