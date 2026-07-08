package com.example.healthtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.healthtracker.ui.navigation.AppNavGraph
import com.example.healthtracker.ui.theme.HealthTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as HealthTrackerApp
        val db = app.database
        val settings = app.settingsManager

        setContent {
            HealthTrackerTheme {
                AppNavGraph(
                    weightDao = db.weightDao(),
                    calorieDao = db.calorieDao(),
                    settingsManager = settings
                )
            }
        }
    }
}
