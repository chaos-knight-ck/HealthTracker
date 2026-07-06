package com.example.healthtracker.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LineChart(
    data: List<Pair<Long, Float>>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    label: String = "kg"
) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    if (data.size < 2) return

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        val paddingLeft = 50f
        val paddingRight = 16f
        val paddingTop = 16f
        val paddingBottom = 40f
        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingTop - paddingBottom

        val values = data.map { it.second }
        val minVal = values.min() - 0.5f
        val maxVal = values.max() + 0.5f
        val range = (maxVal - minVal).coerceAtLeast(1f)

        fun xPos(index: Int) = paddingLeft + (index.toFloat() / (data.size - 1)) * chartWidth
        fun yPos(value: Float) = paddingTop + (1f - (value - minVal) / range) * chartHeight

        val gridLines = 4
        for (i in 0..gridLines) {
            val y = paddingTop + (i.toFloat() / gridLines) * chartHeight
            val value = maxVal - (i.toFloat() / gridLines) * range
            drawLine(gridColor, Offset(paddingLeft, y), Offset(size.width - paddingRight, y), strokeWidth = 1f)
            drawText(
                textMeasurer,
                "%.1f".format(value),
                topLeft = Offset(0f, y - 8f),
                style = TextStyle(fontSize = 10.sp, color = labelColor)
            )
        }

        val path = Path()
        data.forEachIndexed { i, (_, value) ->
            val x = xPos(i)
            val y = yPos(value)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

        data.forEachIndexed { i, (_, value) ->
            drawCircle(lineColor, 4f, Offset(xPos(i), yPos(value)))
        }

        val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())
        val labelStep = (data.size / 5).coerceAtLeast(1)
        data.forEachIndexed { i, (timestamp, _) ->
            if (i % labelStep == 0 || i == data.size - 1) {
                drawText(
                    textMeasurer,
                    dateFormat.format(Date(timestamp)),
                    topLeft = Offset(xPos(i) - 12f, size.height - paddingBottom + 8f),
                    style = TextStyle(fontSize = 10.sp, color = labelColor)
                )
            }
        }
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

            drawRect(barColor, Offset(x, y), androidx.compose.ui.geometry.Size(barWidth, barHeight))

            drawText(
                textMeasurer,
                label,
                topLeft = Offset(x, size.height - paddingBottom + 8f),
                style = TextStyle(fontSize = 10.sp, color = labelColor)
            )

            drawText(
                textMeasurer,
                value.toString(),
                topLeft = Offset(x, y - 16f),
                style = TextStyle(fontSize = 10.sp, color = labelColor)
            )
        }
    }
}
