package com.example.healthtracker.data

import android.content.Context

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("health_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GOAL_WEIGHT = "goal_weight"
        private const val KEY_HEIGHT_CM = "height_cm"
        private const val KEY_MILESTONE_INTERVAL = "milestone_interval"
    }

    var goalWeight: Float?
        get() = if (prefs.contains(KEY_GOAL_WEIGHT)) prefs.getFloat(KEY_GOAL_WEIGHT, 0f) else null
        set(value) {
            if (value != null) prefs.edit().putFloat(KEY_GOAL_WEIGHT, value).apply()
            else prefs.edit().remove(KEY_GOAL_WEIGHT).apply()
        }

    var heightCm: Float?
        get() = if (prefs.contains(KEY_HEIGHT_CM)) prefs.getFloat(KEY_HEIGHT_CM, 0f) else null
        set(value) {
            if (value != null) prefs.edit().putFloat(KEY_HEIGHT_CM, value).apply()
            else prefs.edit().remove(KEY_HEIGHT_CM).apply()
        }

    var milestoneInterval: Float
        get() = prefs.getFloat(KEY_MILESTONE_INTERVAL, 2f)
        set(value) = prefs.edit().putFloat(KEY_MILESTONE_INTERVAL, value).apply()
}
