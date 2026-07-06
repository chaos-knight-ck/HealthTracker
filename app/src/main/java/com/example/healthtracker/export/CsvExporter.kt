package com.example.healthtracker.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.healthtracker.data.dao.CalorieDao
import com.example.healthtracker.data.dao.WeightDao
import com.example.healthtracker.data.entity.CalorieRecord
import com.example.healthtracker.data.entity.WeightRecord
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun export(context: Context, weightDao: WeightDao, calorieDao: CalorieDao): List<Uri> {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val uris = mutableListOf<Uri>()

        val weights = weightDao.getAll().first()
        if (weights.isNotEmpty()) {
            val file = File(exportDir, "weight_records.csv")
            writeWeightCsv(file, weights)
            uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
        }

        val calories = calorieDao.getAll().first()
        if (calories.isNotEmpty()) {
            val file = File(exportDir, "calorie_records.csv")
            writeCalorieCsv(file, calories)
            uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
        }

        return uris
    }

    private fun writeWeightCsv(file: File, records: List<WeightRecord>) {
        FileWriter(file, Charsets.UTF_8).use { writer ->
            writer.appendLine("﻿日期,体重(kg),腰围(cm),备注")
            records.forEach { r ->
                val date = dateFormat.format(Date(r.date))
                val waist = r.waist?.toString() ?: ""
                val note = r.note?.replace(",", "，") ?: ""
                writer.appendLine("$date,${r.weight},$waist,$note")
            }
        }
    }

    private fun writeCalorieCsv(file: File, records: List<CalorieRecord>) {
        FileWriter(file, Charsets.UTF_8).use { writer ->
            writer.appendLine("﻿日期,餐次,食物,热量(kcal)")
            records.forEach { r ->
                val date = dateFormat.format(Date(r.date))
                val food = r.foodName.replace(",", "，")
                writer.appendLine("$date,${r.mealType},$food,${r.calories}")
            }
        }
    }

    suspend fun importCsv(context: Context, uri: Uri, weightDao: WeightDao, calorieDao: CalorieDao): ImportResult {
        val reader = BufferedReader(InputStreamReader(context.contentResolver.openInputStream(uri), Charsets.UTF_8))
        val lines = reader.use { it.readLines() }

        if (lines.size < 2) return ImportResult(0, "文件为空")

        val header = lines[0].removePrefix("﻿").trim()

        return when {
            header.contains("体重") && header.contains("腰围") -> importWeightCsv(lines, weightDao)
            header.contains("体重") -> importWeightCsvLegacy(lines, weightDao)
            header.contains("热量") || header.contains("食物") -> importCalorieCsv(lines, calorieDao)
            else -> ImportResult(0, "无法识别 CSV 格式，请使用本应用导出的文件")
        }
    }

    private suspend fun importWeightCsv(lines: List<String>, weightDao: WeightDao): ImportResult {
        var count = 0
        for (i in 1 until lines.size) {
            val parts = lines[i].split(",", limit = 4)
            if (parts.size < 2) continue
            val date = parseDate(parts[0].trim()) ?: continue
            val weight = parts[1].trim().toFloatOrNull() ?: continue
            val waist = parts.getOrNull(2)?.trim()?.toFloatOrNull()
            val note = parts.getOrNull(3)?.trim()?.ifBlank { null }
            weightDao.insert(WeightRecord(date = date, weight = weight, waist = waist, note = note))
            count++
        }
        return ImportResult(count, "成功导入 $count 条体重记录")
    }

    private suspend fun importWeightCsvLegacy(lines: List<String>, weightDao: WeightDao): ImportResult {
        var count = 0
        for (i in 1 until lines.size) {
            val parts = lines[i].split(",", limit = 3)
            if (parts.size < 2) continue
            val date = parseDate(parts[0].trim()) ?: continue
            val weight = parts[1].trim().toFloatOrNull() ?: continue
            val note = parts.getOrNull(2)?.trim()?.ifBlank { null }
            weightDao.insert(WeightRecord(date = date, weight = weight, note = note))
            count++
        }
        return ImportResult(count, "成功导入 $count 条体重记录")
    }

    private suspend fun importCalorieCsv(lines: List<String>, calorieDao: CalorieDao): ImportResult {
        var count = 0
        for (i in 1 until lines.size) {
            val parts = lines[i].split(",", limit = 4)
            if (parts.size < 4) continue
            val date = parseDate(parts[0].trim()) ?: continue
            val mealType = parts[1].trim()
            val foodName = parts[2].trim()
            val calories = parts[3].trim().toIntOrNull() ?: continue
            calorieDao.insert(CalorieRecord(date = date, foodName = foodName, calories = calories, mealType = mealType))
            count++
        }
        return ImportResult(count, "成功导入 $count 条热量记录")
    }

    private fun parseDate(text: String): Long? {
        return try {
            dateFormat.parse(text)?.time
        } catch (e: Exception) {
            null
        }
    }

    data class ImportResult(val count: Int, val message: String)
}
