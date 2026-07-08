package com.example.healthtracker.util

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

enum class PredictionStatus {
    ON_TRACK, WRONG_DIRECTION, PLATEAU, GOAL_REACHED, INSUFFICIENT_DATA
}

data class TrendPrediction(
    val slopePerDay: Float,
    val predictedDaysToGoal: Int?,
    val status: PredictionStatus
)

data class MilestoneInfo(
    val startWeight: Float,
    val goalWeight: Float,
    val currentWeight: Float,
    val milestones: List<Float>,
    val achievedCount: Int,
    val nextMilestone: Float?,
    val overallProgress: Float
)

data class WeeklyChange(
    val weekLabel: String,
    val avgWeight: Float,
    val change: Float
)

object TrendCalculator {

    fun predict(
        data: List<Pair<Long, Float>>,
        goalWeight: Float
    ): TrendPrediction {
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - 30L * 86_400_000
        val recent = data.filter { it.first >= thirtyDaysAgo }.sortedBy { it.first }

        if (recent.size < 7) return TrendPrediction(0f, null, PredictionStatus.INSUFFICIENT_DATA)

        val currentWeight = recent.last().second
        if (abs(currentWeight - goalWeight) < 0.1f) {
            return TrendPrediction(0f, 0, PredictionStatus.GOAL_REACHED)
        }

        val firstTs = recent.first().first.toDouble()
        val xs = recent.map { ((it.first.toDouble() - firstTs) / 86_400_000.0) }
        val ys = recent.map { it.second.toDouble() }
        val n = xs.size.toDouble()
        val sumX = xs.sum()
        val sumY = ys.sum()
        val sumXY = xs.zip(ys).sumOf { (a, b) -> a * b }
        val sumX2 = xs.sumOf { it * it }
        val denom = n * sumX2 - sumX * sumX

        if (abs(denom) < 1e-6) return TrendPrediction(0f, null, PredictionStatus.PLATEAU)

        val slope = ((n * sumXY - sumX * sumY) / denom).toFloat()

        if (abs(slope) < 0.01f) return TrendPrediction(slope, null, PredictionStatus.PLATEAU)

        val isLosing = goalWeight < currentWeight
        val slopeGoesRight = (isLosing && slope < 0) || (!isLosing && slope > 0)

        if (!slopeGoesRight) return TrendPrediction(slope, null, PredictionStatus.WRONG_DIRECTION)

        val daysToGoal = ((goalWeight - currentWeight) / slope).toInt()
        if (daysToGoal < 0 || daysToGoal > 3650) {
            return TrendPrediction(slope, null, PredictionStatus.WRONG_DIRECTION)
        }

        return TrendPrediction(slope, daysToGoal, PredictionStatus.ON_TRACK)
    }

    fun calculateMilestones(
        allData: List<Pair<Long, Float>>,
        goalWeight: Float,
        interval: Float
    ): MilestoneInfo {
        val sorted = allData.sortedBy { it.first }
        val startWeight = sorted.first().second
        val currentWeight = sorted.last().second
        val isLosing = goalWeight < startWeight

        val milestones = mutableListOf<Float>()
        val safeInterval = interval.coerceAtLeast(0.5f)

        var m = if (isLosing) startWeight - safeInterval else startWeight + safeInterval
        while (if (isLosing) m > goalWeight + 0.01f else m < goalWeight - 0.01f) {
            milestones.add(m)
            m += if (isLosing) -safeInterval else safeInterval
        }
        milestones.add(goalWeight)

        val achieved = milestones.count {
            if (isLosing) currentWeight <= it + 0.01f else currentWeight >= it - 0.01f
        }

        val totalDiff = abs(startWeight - goalWeight)
        val progress = if (totalDiff < 0.1f) 1f
        else (abs(startWeight - currentWeight) / totalDiff).coerceIn(0f, 1f)

        val next = milestones.firstOrNull {
            if (isLosing) currentWeight > it + 0.01f else currentWeight < it - 0.01f
        }

        return MilestoneInfo(startWeight, goalWeight, currentWeight, milestones, achieved, next, progress)
    }

    fun calculateWeeklyChanges(allData: List<Pair<Long, Float>>): List<WeeklyChange> {
        if (allData.size < 2) return emptyList()

        val sorted = allData.sortedBy { it.first }
        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())

        val weeklyAvgs = sorted
            .groupBy { (ts, _) ->
                cal.timeInMillis = ts
                cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.WEEK_OF_YEAR)
            }
            .entries
            .sortedBy { it.key }
            .map { (_, records) ->
                val avg = records.map { it.second }.average().toFloat()
                val firstDate = records.minOf { it.first }
                firstDate to avg
            }

        if (weeklyAvgs.size < 2) return emptyList()

        return weeklyAvgs.zipWithNext { (_, prevAvg), (date, curAvg) ->
            WeeklyChange(
                weekLabel = dateFormat.format(Date(date)),
                avgWeight = curAvg,
                change = curAvg - prevAvg
            )
        }.takeLast(12)
    }
}
