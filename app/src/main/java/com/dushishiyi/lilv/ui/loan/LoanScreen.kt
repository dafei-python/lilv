package com.dushishiyi.lilv.ui.loan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.dushishiyi.lilv.ui.components.SimpleLineChart

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
                Column {
                    Text(
                        text = stringResource(R.string.loan_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "央行 LPR · 公积金贷款",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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

                // LPR 当前值
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

                // LPR 历史走势
                item { SectionHeader(stringResource(R.string.loan_history_section), subtitle = "近 ${data.lpr.history.size} 期") }
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 1.dp,
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            val series1y = data.lpr.history.map { it.lpr1y.toFloat() }.reversed()
                            val series5y = data.lpr.history.map { it.lpr5y.toFloat() }.reversed()
                            SimpleLineChart(points = series5y)
                            Text(
                                text = "5 年期 LPR 走势",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                            )
                        }
                    }
                }

                // 公积金贷款
                item { SectionHeader(stringResource(R.string.loan_fund_section), subtitle = "${data.fund.effectiveSince} 起执行") }
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 1.dp,
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            FundRow(stringResource(R.string.fund_first), data.fund.first5yBelow, data.fund.first5yAbove)
                            FundRow(stringResource(R.string.fund_second), data.fund.second5yBelow, data.fund.second5yAbove)
                        }
                    }
                }

                // 典型房贷参考
                item { SectionHeader(stringResource(R.string.loan_mortgage_section)) }
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        tonalElevation = 1.dp,
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            MortgageRow(stringResource(R.string.mortgage_first), data.mortgageReference.firstFloorMin, data.mortgageReference.firstFloorMax)
                            MortgageRow(stringResource(R.string.mortgage_second), data.mortgageReference.secondFloorMin, data.mortgageReference.secondFloorMax)
                            Text(
                                text = data.mortgageReference.note,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "LPR 每月 20 日公布（遇节假日顺延）。LPR 是基准利率，实际执行利率 = LPR ± 基点，各银行、各城市加点不同。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    )
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

@Composable
private fun FundRow(name: String, below: Double, above: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "5 年以下 %.3f%%".format(below),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.2f),
        )
        Text(
            text = "5 年以上 %.3f%%".format(above),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.2f),
        )
    }
}

@Composable
private fun MortgageRow(name: String, min: Double, max: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "%.2f%% ~ %.2f%%".format(min, max),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.5f),
        )
    }
}
