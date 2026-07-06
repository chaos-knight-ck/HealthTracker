package com.example.healthtracker.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
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
    var waistInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var selectedDate by remember { mutableLongStateOf(todayStartMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val displayDateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val listDateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("当前体重", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = latestWeight?.let { "%.1f kg".format(it.weight) } ?: "-- kg",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("当前腰围", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = latestWeight?.waist?.let { "%.1f cm".format(it) } ?: "-- cm",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = displayDateFormat.format(Date(selectedDate)),
            onValueChange = {},
            label = { Text("日期（默认今天）") },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "选择日期")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
            singleLine = true
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = weightInput,
                onValueChange = { if (it.matches(Regex("^\\d{0,3}(\\.\\d{0,1})?$"))) weightInput = it },
                label = { Text("体重 (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = waistInput,
                onValueChange = { if (it.matches(Regex("^\\d{0,3}(\\.\\d{0,1})?$"))) waistInput = it },
                label = { Text("腰围 (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

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
                val now = Calendar.getInstance()
                val recordDate = Calendar.getInstance().apply {
                    timeInMillis = selectedDate
                    set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, now.get(Calendar.MINUTE))
                    set(Calendar.SECOND, now.get(Calendar.SECOND))
                }
                scope.launch {
                    weightDao.insert(
                        WeightRecord(
                            date = recordDate.timeInMillis,
                            weight = w,
                            waist = waistInput.toFloatOrNull(),
                            note = noteInput.ifBlank { null }
                        )
                    )
                    weightInput = ""
                    waistInput = ""
                    noteInput = ""
                    selectedDate = todayStartMillis()
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
                    dateFormat = listDateFormat,
                    onDelete = { scope.launch { weightDao.delete(record) } }
                )
            }
        }
    }
}

private fun todayStartMillis(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
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

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "%.1f kg".format(record.weight),
                    style = MaterialTheme.typography.titleMedium
                )
                record.waist?.let {
                    Text(
                        "腰围 %.1f cm".format(it),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

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
