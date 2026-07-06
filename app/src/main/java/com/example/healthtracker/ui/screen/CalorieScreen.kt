package com.example.healthtracker.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.healthtracker.data.dao.CalorieDao
import com.example.healthtracker.data.entity.CalorieRecord
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val MEAL_TYPES = listOf("早餐", "午餐", "晚餐", "加餐")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalorieScreen(calorieDao: CalorieDao) {
    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val cal = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val startOfDay = cal.timeInMillis
    val endOfDay = startOfDay + 86_400_000L

    val todayRecords by calorieDao.getByDay(startOfDay, endOfDay).collectAsState(initial = emptyList())
    val todayTotal by calorieDao.getDayTotal(startOfDay, endOfDay).collectAsState(initial = null)

    var foodInput by remember { mutableStateOf("") }
    var calorieInput by remember { mutableStateOf("") }
    var selectedMeal by remember { mutableIntStateOf(0) }

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
                Text("今日摄入", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "${todayTotal ?: 0} kcal",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            MEAL_TYPES.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedMeal == index,
                    onClick = { selectedMeal = index },
                    shape = SegmentedButtonDefaults.itemShape(index, MEAL_TYPES.size)
                ) {
                    Text(label)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = foodInput,
                onValueChange = { foodInput = it },
                label = { Text("食物（可选）") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            OutlinedTextField(
                value = calorieInput,
                onValueChange = { if (it.matches(Regex("^\\d{0,5}$"))) calorieInput = it },
                label = { Text("热量") },
                suffix = { Text("kcal") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(130.dp),
                singleLine = true
            )
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                val kcal = calorieInput.toIntOrNull() ?: return@Button
                scope.launch {
                    calorieDao.insert(
                        CalorieRecord(
                            date = System.currentTimeMillis(),
                            foodName = foodInput.trim().ifBlank { "未填写" },
                            calories = kcal,
                            mealType = MEAL_TYPES[selectedMeal]
                        )
                    )
                    foodInput = ""
                    calorieInput = ""
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = calorieInput.toIntOrNull() != null
        ) {
            Text("保存记录")
        }

        Spacer(Modifier.height(16.dp))

        Text("今日记录", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MEAL_TYPES.forEach { meal ->
                val mealRecords = todayRecords.filter { it.mealType == meal }
                if (mealRecords.isNotEmpty()) {
                    item {
                        Text(
                            meal,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(mealRecords, key = { it.id }) { record ->
                        CalorieRecordItem(
                            record = record,
                            dateFormat = dateFormat,
                            onDelete = { scope.launch { calorieDao.delete(record) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalorieRecordItem(
    record: CalorieRecord,
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
                Text(record.foodName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    dateFormat.format(Date(record.date)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${record.calories} kcal",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
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
