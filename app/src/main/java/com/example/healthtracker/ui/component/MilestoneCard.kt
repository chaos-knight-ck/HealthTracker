package com.example.healthtracker.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.healthtracker.ui.theme.Green40
import com.example.healthtracker.ui.theme.MinionBlue
import com.example.healthtracker.util.MilestoneInfo
import kotlin.math.abs

@Composable
fun MilestoneCard(
    info: MilestoneInfo,
    modifier: Modifier = Modifier
) {
    val isLosing = info.goalWeight < info.startWeight

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("里程碑进度", style = MaterialTheme.typography.titleSmall)
                Text(
                    "${info.achievedCount} / ${info.milestones.size} 已完成",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { info.overallProgress },
                modifier = Modifier.fillMaxWidth(),
                color = if (isLosing) Green40 else MinionBlue,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "起始 %.1f kg".format(info.startWeight),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "目标 %.1f kg".format(info.goalWeight),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            if (info.nextMilestone != null) {
                val diff = abs(info.currentWeight - info.nextMilestone)
                Text(
                    "下一个: %.1f kg（还差 %.1f kg）".format(info.nextMilestone, diff),
                    style = MaterialTheme.typography.bodySmall,
                    color = MinionBlue
                )
            } else {
                Text(
                    "所有里程碑已达成!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Green40
                )
            }
        }
    }
}
