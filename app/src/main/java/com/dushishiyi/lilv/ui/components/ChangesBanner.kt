package com.dushishiyi.lilv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dushishiyi.lilv.data.RateChange
import com.dushishiyi.lilv.ui.theme.RateDown
import com.dushishiyi.lilv.ui.theme.RateUp

/**
 * 变动横幅：首页顶部展示"工行 1 年期 0.95% → 0.90%（−5BP）"等。
 * 多条变动时按行展示，最多 3 条避免喧宾夺主。
 */
@Composable
fun ChangesBanner(
    changes: List<RateChange>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    if (changes.isEmpty()) return
    val display = changes.take(3)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        display.forEach { c ->
            val raised = c.isRaised
            val color = if (raised) RateUp else RateDown
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (raised) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.padding(end = 2.dp),
                )
                Text(
                    text = c.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "%.2f%% → %.2f%%".format(c.oldValue, c.newValue),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = c.formatDiff(),
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
