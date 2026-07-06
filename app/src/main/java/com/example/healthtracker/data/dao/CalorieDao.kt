package com.example.healthtracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.healthtracker.data.entity.CalorieRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface CalorieDao {
    @Insert
    suspend fun insert(record: CalorieRecord)

    @Delete
    suspend fun delete(record: CalorieRecord)

    @Query("SELECT * FROM calorie_records ORDER BY date DESC")
    fun getAll(): Flow<List<CalorieRecord>>

    @Query("SELECT * FROM calorie_records WHERE date >= :startOfDay AND date < :endOfDay ORDER BY date DESC")
    fun getByDay(startOfDay: Long, endOfDay: Long): Flow<List<CalorieRecord>>

    @Query("SELECT SUM(calories) FROM calorie_records WHERE date >= :startOfDay AND date < :endOfDay")
    fun getDayTotal(startOfDay: Long, endOfDay: Long): Flow<Int?>

    @Query("SELECT date, SUM(calories) as totalCalories FROM calorie_records WHERE date >= :since GROUP BY date / 86400000 ORDER BY date ASC")
    fun getDailySummary(since: Long): Flow<List<DailyCalorieSummary>>
}

data class DailyCalorieSummary(
    val date: Long,
    val totalCalories: Int
)
