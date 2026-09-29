package com.dushishiyi.lilv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dushishiyi.lilv.R
import com.dushishiyi.lilv.data.ReferenceSection

/**
 * 存款页底部「其他资产回报参考」可折叠卡片。
 * 默认收起，点击标题展开显示 5 行对比数据。
 */
@Composable
fun ReferenceCard(reference: ReferenceSection) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    ) {
        Column {
            // 标题行（点击展开/收起）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.reference_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(if (expanded) 180f else 0f),
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    ReferenceRow(
                        label = stringResource(R.string.reference_deposit1y),
                        rate = reference.deposit1y,
                        risk = stringResource(R.string.reference_risk_low),
                    )
                    ReferenceRow(
                        label = stringResource(R.string.reference_treasury10y),
                        rate = reference.treasury10y,
                        risk = stringResource(R.string.reference_risk_low),
                    )
                    ReferenceRow(
                        label = stringResource(R.string.reference_insurance),
                        rate = reference.insuranceCap,
                        risk = stringResource(R.string.reference_risk_low),
                    )
                    ReferenceRow(
                        label = stringResource(R.string.reference_wealth),
                        rate = reference.wealthManagement,
                        risk = stringResource(R.string.reference_risk_ml),
                    )
                    ReferenceRow(
                        label = stringResource(R.string.reference_csi300),
                        rate = reference.csi300_10y_annualized,
                        risk = stringResource(R.string.reference_risk_mh),
                    )

                    if (reference.updatedDescription.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${stringResource(R.string.reference_note_prefix)}：${reference.updatedDescription}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReferenceRow(label: String, rate: Double, risk: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${"%.2f".format(rate)}%",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            text = risk,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}
