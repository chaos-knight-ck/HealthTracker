package com.example.healthtracker.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calorie_records")
data class CalorieRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val foodName: String,
    val calories: Int,
    val mealType: String
)
