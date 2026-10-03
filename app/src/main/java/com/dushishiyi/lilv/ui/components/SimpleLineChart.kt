package com.dushishiyi.lilv.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dushishiyi.lilv.ui.theme.RateDown
import com.dushishiyi.lilv.ui.theme.RateUp

/**
 * 简洁的折线图：用于 LPR 历史走势、贵金属/汇率近半年走势。
 * - 数据点 [points]：从左到右
 * - 自动归一化到画布
 * - 末点（最新）画一个高亮圆点
 *
 * 不引入图表库，依赖最少。
 */
@Composable
fun SimpleLineChart(
    points: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = RateDown,
    dotColor: Color = RateUp,
    height: Dp = 120.dp,
    valueDigits: Int = 2,
    showValueLabel: Boolean = true,
) {
    if (points.isEmpty()) return

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val maxV = points.max()
        val minV = points.min()
        val range = (maxV - minV).coerceAtLeast(0.01f)
        val pad = 8.dp.toPx()
        val w = size.width - pad * 2
        val h = size.height - pad * 2

        val stepX = if (points.size > 1) w / (points.size - 1) else 0f

        fun pointToOffset(index: Int, value: Float): Offset {
            val x = pad + index * stepX
            val normalized = (value - minV) / range
            val y = pad + (1f - normalized) * h
            return Offset(x, y)
        }

        // 横线网格（3 条）
        val gridColor = Color.Gray.copy(alpha = 0.2f)
        for (i in 0..2) {
            val y = pad + i * (h / 2)
            drawLine(
                color = gridColor,
                start = Offset(pad, y),
                end = Offset(pad + w, y),
                strokeWidth = 1f,
            )
        }

        // 折线
        val path = Path()
        points.forEachIndexed { i, v ->
            val offset = pointToOffset(i, v)
            if (i == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
        }
        drawPath(path = path, color = lineColor, style = Stroke(width = 2.5.dp.toPx()))

        // 末点高亮
        val last = points.last()
        val lastOffset = pointToOffset(points.size - 1, last)
        drawCircle(
            color = dotColor,
            radius = 5.dp.toPx(),
            center = lastOffset,
        )

        // 末值标注
        if (showValueLabel) {
            drawContext.canvas.nativeCanvas.apply {
                val label = "%.${valueDigits}f".format(last)
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(220, 80, 80, 80)
                    textSize = 11.sp.toPx()
                    isAntiAlias = true
                }
                val labelWidth = paint.measureText(label)
                // 末点贴近右缘时把文字收在点的左侧，避免裁切
                val labelX = if (lastOffset.x + labelWidth + 4.dp.toPx() > size.width) {
                    lastOffset.x - labelWidth - 8.dp.toPx()
                } else {
                    lastOffset.x - 16
                }
                drawText(label, labelX, lastOffset.y - 12, paint)
            }
        }
    }
}
