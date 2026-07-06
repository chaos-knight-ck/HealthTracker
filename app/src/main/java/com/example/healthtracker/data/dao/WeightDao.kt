package com.example.healthtracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.healthtracker.data.entity.WeightRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Insert
    suspend fun insert(record: WeightRecord)

    @Delete
    suspend fun delete(record: WeightRecord)

    @Query("SELECT * FROM weight_records ORDER BY date DESC")
    fun getAll(): Flow<List<WeightRecord>>

    @Query("SELECT * FROM weight_records ORDER BY date DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<WeightRecord>>

    @Query("SELECT * FROM weight_records WHERE date >= :since ORDER BY date ASC")
    fun getSince(since: Long): Flow<List<WeightRecord>>

    @Query("SELECT * FROM weight_records ORDER BY date DESC LIMIT 1")
    fun getLatest(): Flow<WeightRecord?>
}
