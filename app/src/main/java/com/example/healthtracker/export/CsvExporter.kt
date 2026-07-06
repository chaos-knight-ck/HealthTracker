package com.example.healthtracker.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.healthtracker.data.dao.CalorieDao
import com.example.healthtracker.data.dao.WeightDao
import com.example.healthtracker.data.entity.CalorieRecord
import com.example.healthtracker.data.entity.WeightRecord
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    suspend fun export(context: Context, weightDao: WeightDao, calorieDao: CalorieDao): List<Uri> {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val uris = mutableListOf<Uri>()

        val weights = weightDao.getAll().first()
        if (weights.isNotEmpty()) {
            val file = File(exportDir, "weight_records.csv")
            writeWeightCsv(file, weights, dateFormat)
            uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
        }

        val calories = calorieDao.getAll().first()
        if (calories.isNotEmpty()) {
            val file = File(exportDir, "calorie_records.csv")
            writeCalorieCsv(file, calories, dateFormat)
            uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file))
        }

        return uris
    }

    private fun writeWeightCsv(file: File, records: List<WeightRecord>, dateFormat: SimpleDateFormat) {
        FileWriter(file, Charsets.UTF_8).use { writer ->
            writer.appendLine("﻿日期,体重(kg),备注")
            records.forEach { r ->
                val date = dateFormat.format(Date(r.date))
                val note = r.note?.replace(",", "，") ?: ""
                writer.appendLine("$date,${r.weight},$note")
            }
        }
    }

    private fun writeCalorieCsv(file: File, records: List<CalorieRecord>, dateFormat: SimpleDateFormat) {
        FileWriter(file, Charsets.UTF_8).use { writer ->
            writer.appendLine("﻿日期,餐次,食物,热量(kcal)")
            records.forEach { r ->
                val date = dateFormat.format(Date(r.date))
                val food = r.foodName.replace(",", "，")
                writer.appendLine("$date,${r.mealType},$food,${r.calories}")
            }
        }
    }
}
