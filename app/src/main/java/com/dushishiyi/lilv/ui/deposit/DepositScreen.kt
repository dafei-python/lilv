package com.dushishiyi.lilv.ui.deposit

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dushishiyi.lilv.R
import com.dushishiyi.lilv.data.BankCatalog
import com.dushishiyi.lilv.data.BankRatesDto
import com.dushishiyi.lilv.data.DepositTerm
import com.dushishiyi.lilv.data.RateChange
import com.dushishiyi.lilv.ui.RatesViewModel
import com.dushishiyi.lilv.ui.components.BankAvatar
import com.dushishiyi.lilv.ui.components.ChangesBanner
import com.dushishiyi.lilv.ui.components.RateDirection
import com.dushishiyi.lilv.ui.components.RatePill
import com.dushishiyi.lilv.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositScreen(viewModel: RatesViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()

    var selectedTerm by remember { mutableStateOf(DepositTerm.T1Y) }
    // 横向滑动切换期限：左滑→更长期限，右滑→更短期限
    var dragAccum by remember { mutableFloatStateOf(0f) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, delta ->
                        dragAccum += delta
                        change.consume()
                    },
                    onDragEnd = {
                        val terms = DepositTerm.all
                        val idx = terms.indexOf(selectedTerm)
                        if (dragAccum < -120f && idx < terms.lastIndex) {
                            selectedTerm = terms[idx + 1]
                        } else if (dragAccum > 120f && idx > 0) {
                            selectedTerm = terms[idx - 1]
                        }
                        dragAccum = 0f
                    },
                    onDragCancel = { dragAccum = 0f },
                )
            },
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // 顶部标题栏
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.deposit_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = { viewModel.refresh() }) {
                    if (refreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(24.dp).height(24.dp),
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
                LoadingState()
            }
            is RatesViewModel.UiState.Error -> item {
                ErrorState(message = s.message, onRetry = { viewModel.refresh() })
            }
            is RatesViewModel.UiState.Success -> {
                val data = s.data
                // 五大行挂牌日期通常一致；一致时合并展示，避免每行重复
                val listingDates = data.deposit.banks.map { it.updatedAt }.distinct()
                val showRowDate = listingDates.size > 1

                // 更新时间
                item {
                    Text(
                        text = "${stringResource(R.string.last_updated)}  ${s.fetchedAt}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                // 整存整取提示（+ 合并的挂牌日期）
                item {
                    val hint = if (showRowDate) {
                        stringResource(R.string.deposit_term_hint)
                    } else {
                        "${stringResource(R.string.deposit_term_hint)} · 挂牌 ${listingDates.firstOrNull().orEmpty()}"
                    }
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                // 变动横幅
                val depositChanges = s.changes.filter { it.scope == "deposit" }
                if (depositChanges.isNotEmpty()) {
                    item { ChangesBanner(changes = depositChanges) }
                }

                // 期限 Chip 横向滚动
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(DepositTerm.all, key = { it.key }) { term ->
                            FilterChip(
                                selected = selectedTerm == term,
                                onClick = { selectedTerm = term },
                                label = { Text(term.label) },
                            )
                        }
                    }
                }

                // 银行利率列表：按当前期限利率降序；唯一最高者显示「最高」标记
                val changeMap = depositChanges.associateBy { it.title }
                val sortedBanks = data.deposit.banks
                    .sortedByDescending { selectedTerm.dtoSelector(it.rates) }
                val maxRate = sortedBanks.maxOfOrNull { selectedTerm.dtoSelector(it.rates) }
                val maxCount = sortedBanks.count { selectedTerm.dtoSelector(it.rates) == maxRate }
                items(sortedBanks, key = { it.code }) { bank ->
                    DepositBankRow(
                        bank = bank,
                        term = selectedTerm,
                        change = findChange(changeMap, bank, selectedTerm),
                        showDate = showRowDate,
                        isTop = maxCount == 1 && selectedTerm.dtoSelector(bank.rates) == maxRate,
                    )
                }

                // 利率调整间隔提示（小字，不显眼）
                item {
                    Text(
                        text = stringResource(R.string.deposit_adjust_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DepositBankRow(
    bank: BankRatesDto,
    term: DepositTerm,
    change: RateChange?,
    showDate: Boolean,
    isTop: Boolean,
) {
    val meta = BankCatalog.byCode(bank.code)
    val rate = term.dtoSelector(bank.rates)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (meta != null) BankAvatar(meta)

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = meta?.shortName ?: bank.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    if (isTop) {
                        Surface(
                            modifier = Modifier.padding(start = 6.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                        ) {
                            Text(
                                text = stringResource(R.string.deposit_top_rate),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                if (showDate) {
                    Text(
                        text = "挂牌 ${bank.updatedAt}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = "%.2f%%".format(rate),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (change != null) {
                RatePill(
                    text = change.formatDiff(),
                    direction = when {
                        change.isRaised -> RateDirection.UP
                        change.isLowered -> RateDirection.DOWN
                        else -> RateDirection.NEUTRAL
                    },
                )
            } else {
                Text(
                    text = stringResource(R.string.no_change),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

private fun findChange(
    map: Map<String, RateChange>,
    bank: BankRatesDto,
    term: DepositTerm,
): RateChange? {
    val meta = BankCatalog.byCode(bank.code) ?: return null
    val key = "${meta.shortName} ${term.label}"
    return map[key]
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = onRetry) {
            Text(stringResource(R.string.refresh))
        }
    }
}
