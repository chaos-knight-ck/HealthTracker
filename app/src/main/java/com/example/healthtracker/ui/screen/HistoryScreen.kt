package com.example.healthtracker.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.healthtracker.data.dao.CalorieDao
import com.example.healthtracker.data.dao.WeightDao
import com.example.healthtracker.export.CsvExporter
import com.example.healthtracker.ui.component.*
import com.example.healthtracker.ui.theme.*
import com.example.healthtracker.util.PredictionStatus
import com.example.healthtracker.util.TrendCalculator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun HistoryScreen(
    weightDao: WeightDao,
    calorieDao: CalorieDao,
    goalWeight: Float?,
    heightCm: Float?,
    milestoneInterval: Float,
    onSettingsChanged: (goalWeight: Float?, heightCm: Float?, milestoneInterval: Float) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val allWeightRecords by weightDao.getAll().collectAsState(initial = emptyList())

    val sevenDaysAgo = remember {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
    }
    val calorieSummary by calorieDao.getDailySummary(sevenDaysAgo).collectAsState(initial = emptyList())

    val dateFormat = remember { SimpleDateFormat("M/d", Locale.getDefault()) }

    var showSettings by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val result = CsvExporter.importCsv(context, it, weightDao, calorieDao)
                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            currentGoalWeight = goalWeight,
            currentHeightCm = heightCm,
            currentMilestoneInterval = milestoneInterval,
            onSave = { newGoal, newHeight, newInterval ->
                onSettingsChanged(newGoal, newHeight, newInterval)
                showSettings = false
            },
            onDismiss = { showSettings = false }
        )
    }

    val weightData = remember(allWeightRecords) {
        allWeightRecords.map { it.date to it.weight }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("体重趋势 banana~", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Default.Settings, contentDescription = "设置")
            }
        }

        Spacer(Modifier.height(4.dp))

        InteractiveWeightChart(
            allData = weightData,
            goalWeight = goalWeight,
            modifier = Modifier.fillMaxWidth()
        )

        if (goalWeight != null && weightData.size >= 2) {
            Spacer(Modifier.height(16.dp))

            val prediction = remember(weightData, goalWeight) {
                TrendCalculator.predict(weightData, goalWeight)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("趋势预测", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))

                    val weeklyChange = prediction.slopePerDay * 7
                    if (abs(prediction.slopePerDay) >= 0.01f) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                "周均变化",
                                "%+.2f kg".format(weeklyChange),
                                if (weeklyChange > 0) Red400 else Green40
                            )
                            when (prediction.status) {
                                PredictionStatus.ON_TRACK -> {
                                    val days = prediction.predictedDaysToGoal ?: 0
                                    val weeks = days / 7
                                    StatItem(
                                        "预计达标",
                                        if (weeks > 0) "${weeks}周" else "${days}天",
                                        Green40
                                    )
                                }
                                PredictionStatus.WRONG_DIRECTION ->
                                    StatItem("状态", "趋势反向", Red400)
                                PredictionStatus.PLATEAU ->
                                    StatItem("状态", "平台期", Orange400)
                                PredictionStatus.GOAL_REACHED ->
                                    StatItem("状态", "已达标!", Green40)
                                PredictionStatus.INSUFFICIENT_DATA ->
                                    StatItem("状态", "数据不足", MinionBlue)
                            }
                        }
                    } else {
                        Text(
                            when (prediction.status) {
                                PredictionStatus.GOAL_REACHED -> "已达标!"
                                PredictionStatus.INSUFFICIENT_DATA -> "近30天记录不足7条，无法预测"
                                else -> "体重变化平稳，处于平台期"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            val milestoneInfo = remember(weightData, goalWeight, milestoneInterval) {
                TrendCalculator.calculateMilestones(weightData, goalWeight, milestoneInterval)
            }
            MilestoneCard(info = milestoneInfo)
        }

        val weeklyChanges = remember(weightData) {
            TrendCalculator.calculateWeeklyChanges(weightData)
        }

        if (weeklyChanges.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("每周变化", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            WeeklyChangeChart(data = weeklyChanges)
        }

        Spacer(Modifier.height(24.dp))

        Text("每日热量（近7天）", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (calorieSummary.isNotEmpty()) {
            BarChart(
                data = calorieSummary.map {
                    dateFormat.format(Date(it.date)) to it.totalCalories
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    "暂无热量记录",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                scope.launch {
                    exportAndShare(context, weightDao, calorieDao)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("导出 CSV")
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = { importLauncher.launch("text/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.FileUpload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("导入 CSV")
        }
    }
}

private suspend fun exportAndShare(context: Context, weightDao: WeightDao, calorieDao: CalorieDao) {
    try {
        val uris = CsvExporter.export(context, weightDao, calorieDao)
        if (uris.isEmpty()) {
            Toast.makeText(context, "没有数据可导出", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "text/csv"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "导出小黄人数据"))
    } catch (e: Exception) {
        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
