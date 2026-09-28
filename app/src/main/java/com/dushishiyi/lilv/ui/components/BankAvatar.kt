package com.dushishiyi.lilv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dushishiyi.lilv.data.BankMeta

/**
 * 银行头像：圆形 + 品牌色背景 + 银行简称首字。
 */
@Composable
fun BankAvatar(meta: BankMeta, modifier: Modifier = Modifier) {
    val bg = runCatching { Color(android.graphics.Color.parseColor(meta.colorHex)) }
        .getOrElse { MaterialTheme.colorScheme.primary }
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = meta.shortName.take(1),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
