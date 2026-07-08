package com.example.healthtracker.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthtracker.ui.theme.Green40
import com.example.healthtracker.ui.theme.MinionYellowContainer
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
    var selectedIdx by remember { mutableIntStateOf(-1) }

    if (data.isEmpty()) return

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(data) {
                    detectTapGestures(onTap = { offset ->
                        val pL = 46f
                        val pR = 12f
                        val cW = size.width - pL - pR
                        val slotW = cW / data.size

                        var bestIdx = -1
                        var bestDist = Float.MAX_VALUE
                        data.forEachIndexed { i, _ ->
                            val cx = pL + i * slotW + slotW / 2f
                            val dist = abs(cx - offset.x)
                            if (dist < bestDist && dist < slotW) {
                                bestDist = dist
                                bestIdx = i
                            }
                        }
                        selectedIdx = if (bestIdx == selectedIdx) -1 else bestIdx
                    })
                }
        ) {
            val paddingLeft = 46f
            val paddingRight = 12f
            val paddingTop = 20f
            val paddingBottom = 52f
            val chartWidth = size.width - paddingLeft - paddingRight
            val chartHeight = size.height - paddingTop - paddingBottom

            val maxAbs = data.maxOf { abs(it.change) }.coerceAtLeast(0.3f)
            val zeroY = paddingTop + chartHeight / 2f

            drawLine(zeroLineColor, Offset(paddingLeft, zeroY), Offset(size.width - paddingRight, zeroY), 1f)

            val slotWidth = chartWidth / data.size
            val barWidth = slotWidth * 0.55f
            val barOffset = (slotWidth - barWidth) / 2f

            data.forEachIndexed { i, wc ->
                val barHeight = (abs(wc.change) / maxAbs) * (chartHeight / 2f)
                val actualBarHeight = barHeight.coerceAtLeast(2f)
                val x = paddingLeft + i * slotWidth + barOffset
                val color = if (wc.change >= 0) gainColor else lossColor
                val isSel = i == selectedIdx

                if (wc.change >= 0) {
                    drawRect(
                        color.copy(alpha = if (isSel) 1f else 0.75f),
                        Offset(x, zeroY - actualBarHeight),
                        Size(barWidth, actualBarHeight)
                    )
                } else {
                    drawRect(
                        color.copy(alpha = if (isSel) 1f else 0.75f),
                        Offset(x, zeroY),
                        Size(barWidth, actualBarHeight)
                    )
                }

                if (isSel) {
                    drawRect(
                        color.copy(alpha = 0.15f),
                        Offset(x - barOffset / 2, paddingTop),
                        Size(slotWidth, chartHeight)
                    )
                }

                val valText = "%+.1f".format(wc.change)
                if (isSel) {
                    val labelY = if (wc.change >= 0)
                        (zeroY - actualBarHeight - 15f).coerceAtLeast(paddingTop)
                    else
                        (zeroY + actualBarHeight + 3f).coerceAtMost(size.height - paddingBottom - 12f)
                    drawText(
                        textMeasurer, valText,
                        topLeft = Offset(x - 4f, labelY),
                        style = TextStyle(fontSize = 10.sp, color = color)
                    )
                }

                val dateCx = paddingLeft + i * slotWidth + slotWidth / 2f
                val dateRow = if (slotWidth < 65f && i % 2 == 1) 1 else 0
                val dateY = size.height - paddingBottom + 8f + dateRow * 14f
                drawText(
                    textMeasurer, wc.weekLabel,
                    topLeft = Offset(dateCx - 12f, dateY),
                    style = TextStyle(
                        fontSize = 9.sp,
                        color = if (isSel) color else labelColor
                    )
                )
            }

            drawText(
                textMeasurer, "+%.1f".format(maxAbs),
                topLeft = Offset(0f, paddingTop - 2f),
                style = TextStyle(fontSize = 8.sp, color = labelColor)
            )
            drawText(
                textMeasurer, "0",
                topLeft = Offset(16f, zeroY - 6f),
                style = TextStyle(fontSize = 8.sp, color = labelColor)
            )
            drawText(
                textMeasurer, "-%.1f".format(maxAbs),
                topLeft = Offset(0f, paddingTop + chartHeight - 10f),
                style = TextStyle(fontSize = 8.sp, color = labelColor)
            )
        }

        if (selectedIdx in data.indices) {
            Spacer(Modifier.height(8.dp))
            val wc = data[selectedIdx]
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
                    Text(
                        "${wc.weekLabel} 周",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "均重 %.1f kg  变化 %+.1f kg".format(wc.avgWeight, wc.change),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (wc.change >= 0) Red400 else Green40
                    )
                }
            }
        }
    }
}
