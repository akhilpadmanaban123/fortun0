package app.paisapilot.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class ChartSlice(val label: String, val value: Long, val color: Color)

/** Ring chart with whatever is passed as [center] drawn in the middle. */
@Composable
fun DonutChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    diameter: Dp = 132.dp,
    thickness: Dp = 18.dp,
    center: @Composable () -> Unit = {},
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val total = slices.sumOf { it.value }.coerceAtLeast(1)
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val stroke = thickness.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
            var start = -90f
            slices.forEach { slice ->
                val sweep = 360f * slice.value / total
                drawArc(slice.color, start, sweep, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
                start += sweep
            }
        }
        center()
    }
}

/** A gauge from 0 to [max], used for the financial health score. */
@Composable
fun ScoreRing(
    score: Int,
    max: Int,
    color: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 148.dp,
    thickness: Dp = 14.dp,
    center: @Composable () -> Unit = {},
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(diameter)) {
            val stroke = thickness.toPx()
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 135f, 270f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(color, 135f, 270f * score.coerceIn(0, max) / max, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        center()
    }
}

/**
 * One slim bar per day. Days after [todayIndex] have not happened yet and are drawn as
 * faint placeholders; [todayIndex] itself is drawn in [todayColor].
 */
@Composable
fun DailyBars(
    values: List<Long>,
    todayIndex: Int,
    color: Color,
    todayColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 110.dp,
) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Canvas(modifier.fillMaxWidth().height(height)) {
        val slot = size.width / values.size.coerceAtLeast(1)
        val barWidth = slot * 0.62f
        val radius = CornerRadius(barWidth / 2)
        val floor = 3.dp.toPx() // days with no spending still show a small stub
        values.forEachIndexed { index, value ->
            val barHeight = if (value > 0) (size.height * value / max).coerceAtLeast(floor) else floor
            drawRoundRect(
                color = when {
                    index > todayIndex || value == 0L -> track
                    index == todayIndex -> todayColor
                    else -> color
                },
                topLeft = Offset(index * slot + (slot - barWidth) / 2, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = radius,
            )
        }
    }
}

data class BarPair(val label: String, val first: Long, val second: Long)

/** Two bars side by side for each label, e.g. income next to spending for each month. */
@Composable
fun PairedBars(
    pairs: List<BarPair>,
    firstColor: Color,
    secondColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 130.dp,
) {
    val max = pairs.maxOfOrNull { maxOf(it.first, it.second) }?.coerceAtLeast(1) ?: 1
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        pairs.forEach { pair ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.height(height),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    listOf(pair.first to firstColor, pair.second to secondColor).forEach { (value, barColor) ->
                        Box(
                            Modifier
                                .width(12.dp)
                                // A sliver stays visible for zero so the month does not look missing.
                                .fillMaxHeight((value.toFloat() / max).coerceIn(0.02f, 1f))
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (value > 0) barColor else barColor.copy(alpha = 0.25f)),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(pair.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** A single bar split into coloured parts, for showing what a total is made of. */
@Composable
fun StackedBar(slices: List<ChartSlice>, modifier: Modifier = Modifier, height: Dp = 14.dp) {
    val visible = slices.filter { it.value > 0 }
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        visible.forEach { slice ->
            Box(Modifier.weight(slice.value.toFloat()).fillMaxHeight().background(slice.color))
        }
    }
}

/** Coloured dot, label and value; the key to a chart. */
@Composable
fun LegendItem(color: Color, label: String, value: String? = null, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (value != null) Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}
