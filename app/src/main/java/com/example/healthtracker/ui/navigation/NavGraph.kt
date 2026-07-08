package com.example.healthtracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.healthtracker.data.SettingsManager
import com.example.healthtracker.data.dao.CalorieDao
import com.example.healthtracker.data.dao.WeightDao
import com.example.healthtracker.ui.screen.CalorieScreen
import com.example.healthtracker.ui.screen.HistoryScreen
import com.example.healthtracker.ui.screen.WeightScreen

enum class Screen(val route: String, val label: String, val icon: ImageVector) {
    Weight("weight", "记重", Icons.Default.MonitorWeight),
    Calorie("calorie", "热量", Icons.Default.Restaurant),
    History("history", "统计", Icons.Default.BarChart)
}

@Composable
fun AppNavGraph(weightDao: WeightDao, calorieDao: CalorieDao, settingsManager: SettingsManager) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    var goalWeight by remember { mutableStateOf(settingsManager.goalWeight) }
    var heightCm by remember { mutableStateOf(settingsManager.heightCm) }
    var milestoneInterval by remember { mutableFloatStateOf(settingsManager.milestoneInterval) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Weight.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Weight.route) {
                WeightScreen(weightDao, heightCm = heightCm, goalWeight = goalWeight)
            }
            composable(Screen.Calorie.route) { CalorieScreen(calorieDao) }
            composable(Screen.History.route) {
                HistoryScreen(
                    weightDao = weightDao,
                    calorieDao = calorieDao,
                    goalWeight = goalWeight,
                    heightCm = heightCm,
                    milestoneInterval = milestoneInterval,
                    onSettingsChanged = { newGoal, newHeight, newInterval ->
                        goalWeight = newGoal
                        heightCm = newHeight
                        milestoneInterval = newInterval
                        settingsManager.goalWeight = newGoal
                        settingsManager.heightCm = newHeight
                        settingsManager.milestoneInterval = newInterval
                    }
                )
            }
        }
    }
}
