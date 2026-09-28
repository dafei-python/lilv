package com.dushishiyi.lilv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dushishiyi.lilv.ui.theme.RateDown
import com.dushishiyi.lilv.ui.theme.RateUp

/**
 * 涨跌小药丸：根据 [direction] 决定红/绿色，展示 [text]。
 * - 国内习惯：红涨绿跌
 */
@Composable
fun RatePill(
    text: String,
    direction: RateDirection,
    modifier: Modifier = Modifier,
) {
    val color = when (direction) {
        RateDirection.UP -> RateUp
        RateDirection.DOWN -> RateDown
        RateDirection.NEUTRAL -> MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

enum class RateDirection { UP, DOWN, NEUTRAL }
