package com.dushishiyi.lilv.ui.loan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dushishiyi.lilv.R
import com.dushishiyi.lilv.ui.RatesViewModel
import com.dushishiyi.lilv.ui.components.ChangesBanner
import com.dushishiyi.lilv.ui.components.SectionHeader

@Composable
fun LoanScreen(viewModel: RatesViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        // 顶部标题
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.loan_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = { viewModel.refresh() }) {
                    if (refreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(8.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                }
            }
        }

        when (val s = uiState) {
            is RatesViewModel.UiState.Loading -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
            is RatesViewModel.UiState.Error -> item {
                Text(
                    text = s.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }
            is RatesViewModel.UiState.Success -> {
                val data = s.data

                item {
                    Text(
                        text = "${stringResource(R.string.last_updated)}  ${s.fetchedAt}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                // 变动横幅（贷款/公积金）
                val loanChanges = s.changes.filter { it.scope == "lpr" || it.scope == "fund" }
                if (loanChanges.isNotEmpty()) {
                    item { ChangesBanner(changes = loanChanges) }
                }

                // ===== LPR 模块 =====
                item { SectionHeader(stringResource(R.string.loan_lpr_section), subtitle = "${data.lpr.current.date} 公布") }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        LprCard(
                            title = stringResource(R.string.lpr_1y),
                            value = data.lpr.current.lpr1y,
                            modifier = Modifier.weight(1f),
                        )
                        LprCard(
                            title = stringResource(R.string.lpr_5y),
                            value = data.lpr.current.lpr5y,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                // LPR 小字提示（紧跟 LPR 模块，灰色不显眼）
                item {
                    Text(
                        text = "LPR 每月 20 日公布（遇节假日顺延）。实际执行利率 = LPR ± 基点，各银行、各城市加点不同。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }

                // ===== 公积金贷款模块 =====
                item { Spacer(Modifier.height(8.dp)) }
                item { SectionHeader(stringResource(R.string.loan_fund_section), subtitle = "${data.fund.effectiveSince} 起执行") }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        FundGroup(
                            title = stringResource(R.string.fund_first),
                            below = data.fund.first5yBelow,
                            above = data.fund.first5yAbove,
                            highlight = true,
                            modifier = Modifier.weight(1f),
                        )
                        FundGroup(
                            title = stringResource(R.string.fund_second),
                            below = data.fund.second5yBelow,
                            above = data.fund.second5yAbove,
                            highlight = false,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LprCard(title: String, value: Double, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "%.2f%%".format(value),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * 公积金首套/二套对比卡：标签 + 5年以下/5年以上两档利率大数字。
 * highlight=true（首套）用主题色底突出，二套用中性色底。
 */
@Composable
private fun FundGroup(
    title: String,
    below: Double,
    above: Double,
    highlight: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg = if (highlight) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = bg,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(10.dp))
            FundRateLine(label = stringResource(R.string.fund_5y_below), value = below)
            Spacer(Modifier.height(8.dp))
            FundRateLine(label = stringResource(R.string.fund_5y_above), value = above)
        }
    }
}

@Composable
private fun FundRateLine(label: String, value: Double) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "%.3f%%".format(value),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
