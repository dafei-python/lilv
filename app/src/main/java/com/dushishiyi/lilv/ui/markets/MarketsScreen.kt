package com.dushishiyi.lilv.ui.markets

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
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.Paid
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dushishiyi.lilv.R
import com.dushishiyi.lilv.data.MetalQuoteDto
import com.dushishiyi.lilv.ui.RatesViewModel
import com.dushishiyi.lilv.ui.components.SimpleLineChart
import com.dushishiyi.lilv.ui.theme.RateDown
import com.dushishiyi.lilv.ui.theme.RateNeutral
import com.dushishiyi.lilv.ui.theme.RateUp

private val GoldTint = Color(0xFFD4AF37)
private val SilverTint = Color(0xFF9AA5B1)

@Composable
fun MarketsScreen(viewModel: RatesViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.markets_title),
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
                val metals = s.data.metals
                item {
                    Text(
                        text = "${stringResource(R.string.last_updated)}  ${s.fetchedAt}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
                if (metals == null ||
                    (metals.gold == null && metals.silver == null && metals.fx == null)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.markets_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                } else {
                    metals.gold?.let {
                        item {
                            Spacer(Modifier.height(8.dp))
                            MetalCard(
                                quote = it,
                                title = stringResource(R.string.markets_gold_title),
                                icon = Icons.Rounded.MonetizationOn,
                                iconTint = GoldTint,
                                priceDigits = 2,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                    metals.silver?.let {
                        item {
                            Spacer(Modifier.height(12.dp))
                            MetalCard(
                                quote = it,
                                title = stringResource(R.string.markets_silver_title),
                                icon = Icons.Rounded.Paid,
                                iconTint = SilverTint,
                                priceDigits = 0,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                    metals.fx?.let {
                        item {
                            Spacer(Modifier.height(12.dp))
                            MetalCard(
                                quote = it,
                                title = stringResource(R.string.markets_fx_title),
                                icon = Icons.Rounded.CurrencyExchange,
                                iconTint = MaterialTheme.colorScheme.primary,
                                priceDigits = 4,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                    item {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.markets_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单个行情卡片：名称 + 最新价 + 日涨跌 + 近半年涨跌幅 + 半年走势图。
 * 红涨绿跌（国内习惯），折线颜色跟随近半年方向。
 */
@Composable
private fun MetalCard(
    quote: MetalQuoteDto,
    title: String,
    icon: ImageVector,
    iconTint: Color,
    priceDigits: Int,
    modifier: Modifier = Modifier,
) {
    val hist = quote.history
    val first = hist.firstOrNull()?.close ?: quote.price
    val last = hist.lastOrNull()?.close ?: quote.price
    val halfYearPct = if (first != 0.0) (last - first) / first * 100.0 else 0.0
    val trendColor = when {
        halfYearPct > 0.005 -> RateUp
        halfYearPct < -0.005 -> RateDown
        else -> RateNeutral
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 名称 + 数据日期
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconTint)
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                if (quote.asOf.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.markets_as_of, quote.asOf),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            // 最新价 + 日涨跌
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "%.${priceDigits}f".format(quote.price),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (quote.unit.isNotEmpty()) {
                    Text(
                        text = "  ${quote.unit}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                val changeStr = "%.${priceDigits}f".format(quote.change)
                ChangePill(
                    changeText = if (quote.change != 0.0) {
                        "$changeStr  ${"%+.2f".format(quote.changePct)}%"
                    } else {
                        "${"%+.2f".format(quote.changePct)}%"
                    },
                    positive = quote.change > 0.0,
                    negative = quote.change < 0.0,
                )
            }

            Spacer(Modifier.height(6.dp))
            // 近半年涨跌 + 区间
            Row {
                Text(
                    text = stringResource(R.string.markets_half_year) +
                        "  ${"%+.2f".format(halfYearPct)}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = trendColor,
                    fontWeight = FontWeight.SemiBold,
                )
                if (quote.high != quote.low) {
                    Text(
                        text = "　${stringResource(R.string.markets_range)}  " +
                            "${"%.${priceDigits}f".format(quote.low)} ~ ${"%.${priceDigits}f".format(quote.high)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            if (hist.size >= 2) {
                SimpleLineChart(
                    points = hist.map { it.close.toFloat() },
                    lineColor = trendColor,
                    dotColor = trendColor,
                    height = 108.dp,
                    valueDigits = priceDigits,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        hist.first().date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        stringResource(R.string.markets_chart_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        hist.last().date,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangePill(changeText: String, positive: Boolean, negative: Boolean) {
    val color = when {
        positive -> RateUp
        negative -> RateDown
        else -> RateNeutral
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = changeText,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
