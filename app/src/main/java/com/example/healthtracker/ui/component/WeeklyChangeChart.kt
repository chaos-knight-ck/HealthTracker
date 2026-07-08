package com.example.healthtracker.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthtracker.ui.theme.Green40
import com.example.healthtracker.ui.theme.Red400
import com.example.healthtracker.util.WeeklyChange
import kotlin.math.abs

@Composable
fun WeeklyChangeChart(
    data: List<WeeklyChange>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val gainColor = Red400
    val lossColor = Green40
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val zeroLineColor = labelColor.copy(alpha = 0.5f)

    if (data.isEmpty()) return

    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        val paddingLeft = 46f
        val paddingRight = 12f
        val paddingTop = 18f
        val paddingBottom = 40f
        val chartWidth = size.width - paddingLeft - paddingRight
        val chartHeight = size.height - paddingTop - paddingBottom

        val maxAbs = data.maxOf { abs(it.change) }.coerceAtLeast(0.3f)
        val zeroY = paddingTop + chartHeight / 2f

        drawLine(zeroLineColor, Offset(paddingLeft, zeroY), Offset(size.width - paddingRight, zeroY), 1f)

        val barWidth = (chartWidth / data.size) * 0.6f
        val gap = (chartWidth / data.size) * 0.4f

        data.forEachIndexed { i, wc ->
            val barHeight = (abs(wc.change) / maxAbs) * (chartHeight / 2f)
            val x = paddingLeft + i * (barWidth + gap) + gap / 2
            val color = if (wc.change >= 0) gainColor else lossColor

            if (wc.change >= 0) {
                drawRect(color, Offset(x, zeroY - barHeight), Size(barWidth, barHeight))
                drawText(
                    textMeasurer, "%+.1f".format(wc.change),
                    topLeft = Offset(x - 2f, zeroY - barHeight - 14f),
                    style = TextStyle(fontSize = 8.sp, color = color)
                )
            } else {
                drawRect(color, Offset(x, zeroY), Size(barWidth, barHeight))
                drawText(
                    textMeasurer, "%+.1f".format(wc.change),
                    topLeft = Offset(x - 2f, zeroY + barHeight + 2f),
                    style = TextStyle(fontSize = 8.sp, color = color)
                )
            }

            drawText(
                textMeasurer, wc.weekLabel,
                topLeft = Offset(x - 2f, size.height - paddingBottom + 6f),
                style = TextStyle(fontSize = 8.sp, color = labelColor)
            )
        }

        drawText(
            textMeasurer, "+%.1f".format(maxAbs),
            topLeft = Offset(0f, paddingTop - 4f),
            style = TextStyle(fontSize = 8.sp, color = labelColor)
        )
        drawText(
            textMeasurer, "0",
            topLeft = Offset(16f, zeroY - 6f),
            style = TextStyle(fontSize = 8.sp, color = labelColor)
        )
        drawText(
            textMeasurer, "-%.1f".format(maxAbs),
            topLeft = Offset(0f, size.height - paddingBottom - 10f),
            style = TextStyle(fontSize = 8.sp, color = labelColor)
        )
    }
}
