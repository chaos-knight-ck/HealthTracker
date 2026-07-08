package com.example.healthtracker.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthtracker.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

enum class TimeRange(val label: String, val days: Int) {
    MONTH_1("1月", 30),
    MONTH_3("3月", 90),
    MONTH_6("6月", 180),
    YEAR_1("1年", 365),
    ALL("全部", Int.MAX_VALUE)
}

private data class ChartStats(
    val min: Float,
    val max: Float,
    val avg: Float,
    val change: Float,
    val count: Int
)

@Composable
fun InteractiveWeightChart(
    allData: List<Pair<Long, Float>>,
    goalWeight: Float? = null,
    modifier: Modifier = Modifier
) {
    var selectedRange by remember { mutableStateOf(TimeRange.YEAR_1) }
    var scale by remember { mutableFloatStateOf(1f) }
    var scrollX by remember { mutableFloatStateOf(0f) }
    var selectedIdx by remember { mutableIntStateOf(-1) }

    val filteredData = remember(allData, selectedRange) {
        val sorted = allData.sortedBy { it.first }
        if (selectedRange == TimeRange.ALL) sorted
        else {
            val cutoff = System.currentTimeMillis() - selectedRange.days.toLong() * 86_400_000
            sorted.filter { it.first >= cutoff }
        }
    }

    val movingAvg = remember(filteredData) {
        if (filteredData.size < 3) emptyList()
        else filteredData.mapIndexed { i, (date, _) ->
            val windowStart = date - 7L * 86_400_000
            val windowVals = filteredData
                .filter { it.first in windowStart..date }
                .map { it.second }
            date to windowVals.average().toFloat()
        }
    }

    val stats = remember(filteredData) {
        if (filteredData.size >= 2) {
            val w = filteredData.map { it.second }
            ChartStats(w.min(), w.max(), w.average().toFloat(), w.last() - w.first(), filteredData.size)
        } else null
    }

    LaunchedEffect(selectedRange) {
        scale = 1f
        scrollX = 0f
        selectedIdx = -1
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TimeRange.entries.forEach { range ->
                FilterChip(
                    selected = selectedRange == range,
                    onClick = { selectedRange = range },
                    label = { Text(range.label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(32.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (filteredData.size >= 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "── 实际  - - 7日均线",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))

            WeightChartCanvas(
                data = filteredData,
                movingAvg = movingAvg,
                goalWeight = goalWeight,
                scale = scale,
                scrollX = scrollX,
                selectedIdx = selectedIdx,
                onTransform = { newScale, newScrollX, maxScroll ->
                    scale = newScale.coerceIn(1f, 10f)
                    scrollX = newScrollX.coerceIn(0f, maxScroll.coerceAtLeast(0f))
                },
                onSelect = { selectedIdx = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    "至少需要2条记录才能显示图表 banana~",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (selectedIdx in filteredData.indices) {
            Spacer(Modifier.height(8.dp))
            val (ts, w) = filteredData[selectedIdx]
            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(Date(ts))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MinionYellowContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(dateStr, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "%.1f kg".format(w),
                        style = MaterialTheme.typography.titleMedium,
                        color = MinionYellowDark
                    )
                }
            }
        }

        if (stats != null) {
            Spacer(Modifier.height(8.dp))
            StatsRow(stats, goalWeight, filteredData.lastOrNull()?.second)
        }
    }
}

@Composable
private fun WeightChartCanvas(
    data: List<Pair<Long, Float>>,
    movingAvg: List<Pair<Long, Float>>,
    goalWeight: Float?,
    scale: Float,
    scrollX: Float,
    selectedIdx: Int,
    onTransform: (scale: Float, scrollX: Float, maxScroll: Float) -> Unit,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val lineColor = MinionYellowDark
    val avgLineColor = MinionBlue
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val selectedColor = Red400
    val goalLineColor = Green40.copy(alpha = 0.7f)
    val bgColor = MaterialTheme.colorScheme.surface

    var curScale by remember { mutableFloatStateOf(scale) }
    var curScrollX by remember { mutableFloatStateOf(scrollX) }
    var curSelectedIdx by remember { mutableIntStateOf(selectedIdx) }
    curScale = scale
    curScrollX = scrollX
    curSelectedIdx = selectedIdx

    Canvas(
        modifier = modifier
            .clipToBounds()
            .background(bgColor, RoundedCornerShape(8.dp))
            .pointerInput(data) {
                detectTapGestures(onTap = { offset ->
                    val pLeft = 55f
                    val pRight = 16f
                    val daWidth = size.width - pLeft - pRight
                    val virtualW = daWidth * curScale

                    var bestIdx = -1
                    var bestDist = Float.MAX_VALUE
                    data.forEachIndexed { i, _ ->
                        val t = if (data.size == 1) 0.5f else i.toFloat() / (data.size - 1)
                        val sx = pLeft + t * virtualW - curScrollX
                        val dist = abs(sx - offset.x)
                        if (dist < bestDist && dist < 40f) {
                            bestDist = dist
                            bestIdx = i
                        }
                    }
                    onSelect(if (bestIdx == curSelectedIdx) -1 else bestIdx)
                })
            }
            .pointerInput(data) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val pLeft = 55f
                    val pRight = 16f
                    val daWidth = size.width - pLeft - pRight

                    val oldScale = curScale
                    val newScale = (oldScale * zoom).coerceIn(1f, 10f)

                    var newScrollX = curScrollX
                    if (newScale != oldScale) {
                        val cx = centroid.x - pLeft
                        val focusFrac = (curScrollX + cx) / (daWidth * oldScale)
                        newScrollX = focusFrac * daWidth * newScale - cx
                    }
                    newScrollX -= pan.x

                    val maxScroll = daWidth * (newScale - 1f)
                    onTransform(newScale, newScrollX, maxScroll)
                }
            }
    ) {
        val paddingLeft = 55f
        val paddingRight = 16f
        val paddingTop = 24f
        val paddingBottom = 44f
        val dataAreaWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingTop - paddingBottom
        val virtualWidth = dataAreaWidth * scale

        val values = data.map { it.second }
        val valuesWithGoal = if (goalWeight != null) values + goalWeight else values
        val minVal = valuesWithGoal.min() - 1f
        val maxVal = valuesWithGoal.max() + 1f
        val range = (maxVal - minVal).coerceAtLeast(1f)

        fun normX(index: Int): Float =
            if (data.size == 1) 0.5f else index.toFloat() / (data.size - 1)

        fun screenX(t: Float): Float = paddingLeft + t * virtualWidth - scrollX
        fun screenY(value: Float): Float = paddingTop + (1f - (value - minVal) / range) * chartHeight
        fun isVisible(sx: Float): Boolean = sx >= paddingLeft - 30f && sx <= size.width - paddingRight + 30f

        // -- grid --
        val gridCount = 5
        for (i in 0..gridCount) {
            val y = paddingTop + (i.toFloat() / gridCount) * chartHeight
            val v = maxVal - (i.toFloat() / gridCount) * range
            drawLine(gridColor, Offset(paddingLeft, y), Offset(size.width - paddingRight, y), 0.5f)
            drawText(
                textMeasurer, "%.1f".format(v),
                topLeft = Offset(2f, y - 7f),
                style = TextStyle(fontSize = 9.sp, color = labelColor)
            )
        }

        // -- goal weight line (dashed, green) --
        if (goalWeight != null) {
            val goalY = screenY(goalWeight)
            drawLine(
                goalLineColor,
                Offset(paddingLeft, goalY),
                Offset(size.width - paddingRight, goalY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
            )
            drawText(
                textMeasurer, "目标 %.1f".format(goalWeight),
                topLeft = Offset(size.width - paddingRight - 60f, goalY - 16f),
                style = TextStyle(fontSize = 9.sp, color = goalLineColor)
            )
        }

        // -- moving average (dashed) --
        if (movingAvg.size >= 2) {
            val avgPath = Path()
            var started = false
            movingAvg.forEachIndexed { i, (_, v) ->
                val sx = screenX(normX(i))
                val sy = screenY(v)
                if (isVisible(sx) || (i > 0 && isVisible(screenX(normX(i - 1))))) {
                    if (!started) { avgPath.moveTo(sx, sy); started = true }
                    else avgPath.lineTo(sx, sy)
                } else if (started) {
                    avgPath.lineTo(sx, sy)
                    started = false
                }
            }
            drawPath(
                avgPath,
                avgLineColor.copy(alpha = 0.5f),
                style = Stroke(
                    width = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                )
            )
        }

        // -- main line --
        val mainPath = Path()
        data.forEachIndexed { i, (_, v) ->
            val sx = screenX(normX(i))
            val sy = screenY(v)
            if (i == 0) mainPath.moveTo(sx, sy) else mainPath.lineTo(sx, sy)
        }
        drawPath(mainPath, lineColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))

        // -- dots and value labels --
        var lastLabelX = -200f
        val minLabelGap = 48f

        data.forEachIndexed { i, (_, v) ->
            val sx = screenX(normX(i))
            if (!isVisible(sx)) return@forEachIndexed
            val sy = screenY(v)

            val isSel = i == selectedIdx
            drawCircle(
                color = if (isSel) selectedColor else lineColor,
                radius = if (isSel) 6f else 3.5f,
                center = Offset(sx, sy)
            )

            if (isSel || sx - lastLabelX >= minLabelGap) {
                drawText(
                    textMeasurer,
                    "%.1f".format(v),
                    topLeft = Offset(sx - 14f, sy - 18f),
                    style = TextStyle(
                        fontSize = 8.sp,
                        color = if (isSel) selectedColor else labelColor
                    )
                )
                if (!isSel) lastLabelX = sx
            }
        }

        // -- date labels on x-axis --
        val totalDays = if (data.size >= 2)
            (data.last().first - data.first().first) / 86_400_000f else 1f
        val dateFormat = when {
            totalDays / scale <= 90 -> SimpleDateFormat("M/d", Locale.getDefault())
            else -> SimpleDateFormat("yy/M", Locale.getDefault())
        }
        var lastDateX = -200f
        val minDateGap = 72f
        data.forEachIndexed { i, (ts, _) ->
            val sx = screenX(normX(i))
            if (isVisible(sx) && sx - lastDateX >= minDateGap) {
                drawText(
                    textMeasurer,
                    dateFormat.format(Date(ts)),
                    topLeft = Offset(sx - 14f, size.height - paddingBottom + 8f),
                    style = TextStyle(fontSize = 9.sp, color = labelColor)
                )
                lastDateX = sx
            }
        }

        // -- zoom hint --
        if (scale > 1f) {
            val pct = "${(100f / scale).roundToInt()}%"
            drawText(
                textMeasurer, pct,
                topLeft = Offset(size.width - paddingRight - 36f, 4f),
                style = TextStyle(fontSize = 9.sp, color = labelColor.copy(alpha = 0.6f))
            )
        }
    }
}

@Composable
private fun StatsRow(stats: ChartStats, goalWeight: Float? = null, currentWeight: Float? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("变化", "%+.1f kg".format(stats.change),
                if (stats.change > 0) Red400 else Green40)
            StatItem("最低", "%.1f".format(stats.min), MinionBlue)
            StatItem("最高", "%.1f".format(stats.max), MinionYellowDark)
            StatItem("平均", "%.1f".format(stats.avg),
                MaterialTheme.colorScheme.onSurface)
            if (goalWeight != null && currentWeight != null) {
                val distance = currentWeight - goalWeight
                StatItem("距目标", "%+.1f".format(distance),
                    if (abs(distance) < 0.5f) Green40 else MinionBlue)
            } else {
                StatItem("记录", "${stats.count}条",
                    MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.titleSmall, color = color)
    }
}

@Composable
fun BarChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.secondary
) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    if (data.isEmpty()) return

    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        val paddingLeft = 50f
        val paddingRight = 16f
        val paddingTop = 16f
        val paddingBottom = 40f
        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingTop - paddingBottom

        val maxVal = (data.maxOf { it.second }).coerceAtLeast(100).toFloat()
        val barWidth = (chartWidth / data.size) * 0.6f
        val gap = (chartWidth / data.size) * 0.4f

        data.forEachIndexed { i, (label, value) ->
            val barHeight = (value / maxVal) * chartHeight
            val x = paddingLeft + i * (barWidth + gap) + gap / 2
            val y = paddingTop + chartHeight - barHeight

            drawRect(
                barColor,
                Offset(x, y),
                androidx.compose.ui.geometry.Size(barWidth, barHeight)
            )

            drawText(
                textMeasurer, label,
                topLeft = Offset(x, size.height - paddingBottom + 8f),
                style = TextStyle(fontSize = 10.sp, color = labelColor)
            )

            drawText(
                textMeasurer, value.toString(),
                topLeft = Offset(x, y - 16f),
                style = TextStyle(fontSize = 10.sp, color = labelColor)
            )
        }
    }
}
