package com.example.healthtracker.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.healthtracker.data.dao.WeightDao
import com.example.healthtracker.data.entity.WeightRecord
import com.example.healthtracker.ui.theme.Green40
import com.example.healthtracker.ui.theme.Red400
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(weightDao: WeightDao) {
    val scope = rememberCoroutineScope()
    val recentRecords by weightDao.getRecent(20).collectAsState(initial = emptyList())
    val latestWeight by weightDao.getLatest().collectAsState(initial = null)

    var weightInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("当前体重", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = latestWeight?.let { "%.1f kg".format(it.weight) } ?: "-- kg",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = weightInput,
            onValueChange = { if (it.matches(Regex("^\\d{0,3}(\\.\\d{0,1})?$"))) weightInput = it },
            label = { Text("体重 (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = noteInput,
            onValueChange = { noteInput = it },
            label = { Text("备注（可选）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                val w = weightInput.toFloatOrNull() ?: return@Button
                scope.launch {
                    weightDao.insert(
                        WeightRecord(
                            date = System.currentTimeMillis(),
                            weight = w,
                            note = noteInput.ifBlank { null }
                        )
                    )
                    weightInput = ""
                    noteInput = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = weightInput.toFloatOrNull() != null
        ) {
            Text("保存记录")
        }

        Spacer(Modifier.height(16.dp))

        Text("最近记录", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(recentRecords, key = { it.id }) { record ->
                val prevRecord = recentRecords.let { list ->
                    val idx = list.indexOf(record)
                    if (idx < list.size - 1) list[idx + 1] else null
                }
                WeightRecordItem(
                    record = record,
                    prevWeight = prevRecord?.weight,
                    dateFormat = dateFormat,
                    onDelete = { scope.launch { weightDao.delete(record) } }
                )
            }
        }
    }
}

@Composable
private fun WeightRecordItem(
    record: WeightRecord,
    prevWeight: Float?,
    dateFormat: SimpleDateFormat,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateFormat.format(Date(record.date)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                record.note?.let {
                    Text(it, style = MaterialTheme.typography.labelMedium)
                }
            }

            Text(
                "%.1f kg".format(record.weight),
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(Modifier.width(8.dp))

            if (prevWeight != null) {
                val diff = record.weight - prevWeight
                val icon = when {
                    diff > 0.05f -> Icons.Default.TrendingUp
                    diff < -0.05f -> Icons.Default.TrendingDown
                    else -> Icons.Default.TrendingFlat
                }
                val color = when {
                    diff > 0.05f -> Red400
                    diff < -0.05f -> Green40
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
