package com.example.healthtracker.ui.screen

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.healthtracker.data.dao.CalorieDao
import com.example.healthtracker.data.dao.WeightDao
import com.example.healthtracker.export.CsvExporter
import com.example.healthtracker.ui.component.BarChart
import com.example.healthtracker.ui.component.LineChart
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(weightDao: WeightDao, calorieDao: CalorieDao) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val thirtyDaysAgo = remember {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
    }
    val sevenDaysAgo = remember {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
    }

    val weightRecords by weightDao.getSince(thirtyDaysAgo).collectAsState(initial = emptyList())
    val calorieSummary by calorieDao.getDailySummary(sevenDaysAgo).collectAsState(initial = emptyList())

    val dateFormat = remember { SimpleDateFormat("M/d", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("体重趋势（近30天）", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (weightRecords.size >= 2) {
            LineChart(
                data = weightRecords.map { it.date to it.weight },
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
                    "至少需要2条记录才能显示图表",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
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
        context.startActivity(Intent.createChooser(intent, "导出健康数据"))
    } catch (e: Exception) {
        Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
