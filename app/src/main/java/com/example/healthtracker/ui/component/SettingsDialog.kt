package com.example.healthtracker.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SettingsDialog(
    currentGoalWeight: Float?,
    currentHeightCm: Float?,
    currentMilestoneInterval: Float,
    onSave: (goalWeight: Float?, heightCm: Float?, milestoneInterval: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var goalInput by remember { mutableStateOf(currentGoalWeight?.let { "%.1f".format(it) } ?: "") }
    var heightInput by remember { mutableStateOf(currentHeightCm?.let { "%.1f".format(it) } ?: "") }
    var milestoneInput by remember { mutableStateOf("%.1f".format(currentMilestoneInterval)) }
    val inputRegex = remember { Regex("^\\d{0,3}(\\.\\d{0,1})?\$") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置") },
        text = {
            Column {
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = { if (it.isEmpty() || it.matches(inputRegex)) goalInput = it },
                    label = { Text("目标体重 (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = heightInput,
                    onValueChange = { if (it.isEmpty() || it.matches(inputRegex)) heightInput = it },
                    label = { Text("身高 (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = milestoneInput,
                    onValueChange = { if (it.isEmpty() || it.matches(inputRegex)) milestoneInput = it },
                    label = { Text("里程碑间隔 (kg)") },
                    supportingText = { Text("每达到此间隔标记一个里程碑") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    goalInput.toFloatOrNull(),
                    heightInput.toFloatOrNull(),
                    milestoneInput.toFloatOrNull() ?: 2f
                )
            }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
