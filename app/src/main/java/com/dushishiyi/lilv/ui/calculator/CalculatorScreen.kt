package com.dushishiyi.lilv.ui.calculator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dushishiyi.lilv.R
import kotlin.math.pow

private enum class CalcType { COMMERCIAL, FUND, COMBINED }
private enum class CalcMethod { EQUAL_INTEREST, EQUAL_PRINCIPAL }

/**
 * 贷款计算器折叠面板（嵌在贷款页公积金模块下方）。
 * 默认收起只占一行，点开后在固定高度卡片内完成输入与明细查看，
 * 不把贷款页撑得过长。
 *
 * @param initialCommercialRate 商贷默认年利率，取最新 LPR 5 年期以上
 * @param initialFundRate 公积金默认年利率，取首套 5 年以上
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorPanel(
    initialCommercialRate: String = "3.50",
    initialFundRate: String = "2.60",
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var calcType by remember { mutableStateOf(CalcType.COMMERCIAL) }
    var method by remember { mutableStateOf(CalcMethod.EQUAL_INTEREST) }

    var totalAmount by remember { mutableStateOf("100") }        // 万元
    var fundAmount by remember { mutableStateOf("80") }          // 组合贷：公积金部分
    var commercialAmount by remember { mutableStateOf("40") }    // 组合贷：商贷部分
    var years by remember { mutableStateOf("30") }
    var commercialRate by remember { mutableStateOf(initialCommercialRate) }
    var fundRate by remember { mutableStateOf(initialFundRate) }

    var result by remember { mutableStateOf<CalcResult?>(null) }

    fun doCalc(): CalcResult? = runCalculation(
        type = calcType,
        method = method,
        totalAmount = totalAmount.toDoubleOrNull() ?: 0.0,
        fundAmount = fundAmount.toDoubleOrNull() ?: 0.0,
        commercialAmount = commercialAmount.toDoubleOrNull() ?: 0.0,
        years = years.toIntOrNull() ?: 0,
        commercialRate = commercialRate.toDoubleOrNull() ?: 0.0,
        fundRate = fundRate.toDoubleOrNull() ?: 0.0,
    )

    // 已有结果时，任何参数变化都自动重算，避免展示过期结果
    LaunchedEffect(calcType, method, totalAmount, fundAmount, commercialAmount, years, commercialRate, fundRate) {
        if (result != null) result = doCalc()
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 折叠头：整行可点
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Calculate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(12.dp))
                Text(
                    text = stringResource(R.string.calc_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(if (expanded) 180f else 0f),
                )
            }

            AnimatedVisibility(visible = expanded) {
                val resultValue = result
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        // 无结果时随内容自适应（短）；出结果后固定高度，输入区/明细区各自滚动
                        .then(if (resultValue != null) Modifier.height(700.dp) else Modifier),
                ) {
                    if (resultValue != null) {
                        // ===== 有结果：输入区滚动 + 按钮 + 结果明细区 =====
                        CalculatorInputs(
                            modifier = Modifier.weight(1f),
                            calcType = calcType, onCalcType = { calcType = it },
                            totalAmount = totalAmount, onTotalAmount = { totalAmount = it },
                            fundAmount = fundAmount, onFundAmount = { fundAmount = it },
                            commercialAmount = commercialAmount, onCommercialAmount = { commercialAmount = it },
                            years = years, onYears = { years = it },
                            commercialRate = commercialRate, onCommercialRate = { commercialRate = it },
                            fundRate = fundRate, onFundRate = { fundRate = it },
                            method = method, onMethod = { method = it },
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { result = doCalc() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.calc_action))
                        }
                        Column(modifier = Modifier.weight(1.2f).padding(top = 12.dp)) {
                            ResultCard(resultValue)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = stringResource(R.string.calc_schedule_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp)),
                            ) {
                                Column(Modifier.fillMaxSize()) {
                                    ScheduleHeaderRow(
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                                    )
                                    LazyColumn(modifier = Modifier.weight(1f)) {
                                        itemsIndexed(resultValue.schedule, key = { _, row -> row.index }) { idx, row ->
                                            val bg = if (idx % 2 == 0) {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                            } else {
                                                MaterialTheme.colorScheme.surface
                                            }
                                            ScheduleRowItem(
                                                row = row,
                                                modifier = Modifier
                                                    .background(bg)
                                                    .then(
                                                        if (idx == resultValue.schedule.lastIndex) {
                                                            Modifier.clip(
                                                                RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                                                            )
                                                        } else Modifier,
                                                    ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // ===== 无结果：卡片随输入内容自适应，按钮紧跟其后 =====
                        CalculatorInputs(
                            modifier = Modifier,
                            calcType = calcType, onCalcType = { calcType = it },
                            totalAmount = totalAmount, onTotalAmount = { totalAmount = it },
                            fundAmount = fundAmount, onFundAmount = { fundAmount = it },
                            commercialAmount = commercialAmount, onCommercialAmount = { commercialAmount = it },
                            years = years, onYears = { years = it },
                            commercialRate = commercialRate, onCommercialRate = { commercialRate = it },
                            fundRate = fundRate, onFundRate = { fundRate = it },
                            method = method, onMethod = { method = it },
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { result = doCalc() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.calc_action))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalculatorInputs(
    calcType: CalcType,
    onCalcType: (CalcType) -> Unit,
    totalAmount: String,
    onTotalAmount: (String) -> Unit,
    fundAmount: String,
    onFundAmount: (String) -> Unit,
    commercialAmount: String,
    onCommercialAmount: (String) -> Unit,
    years: String,
    onYears: (String) -> Unit,
    commercialRate: String,
    onCommercialRate: (String) -> Unit,
    fundRate: String,
    onFundRate: (String) -> Unit,
    method: CalcMethod,
    onMethod: (CalcMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Text(
            stringResource(R.string.calc_type_preset),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = calcType == CalcType.COMMERCIAL,
                onClick = { onCalcType(CalcType.COMMERCIAL) },
                label = { Text(stringResource(R.string.calc_type_commercial)) },
            )
            FilterChip(
                selected = calcType == CalcType.FUND,
                onClick = { onCalcType(CalcType.FUND) },
                label = { Text(stringResource(R.string.calc_type_fund)) },
            )
            FilterChip(
                selected = calcType == CalcType.COMBINED,
                onClick = { onCalcType(CalcType.COMBINED) },
                label = { Text(stringResource(R.string.calc_type_combined)) },
            )
        }
        Spacer(Modifier.height(12.dp))

        when (calcType) {
            CalcType.COMBINED -> {
                OutlinedTextField(
                    value = fundAmount,
                    onValueChange = { onFundAmount(it.filter { c -> c.isDigit() || c == '.' }) },
                    label = { Text(stringResource(R.string.calc_combined_fund_part)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = commercialAmount,
                    onValueChange = { onCommercialAmount(it.filter { c -> c.isDigit() || c == '.' }) },
                    label = { Text(stringResource(R.string.calc_combined_commercial_part)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
            else -> {
                OutlinedTextField(
                    value = totalAmount,
                    onValueChange = { onTotalAmount(it.filter { c -> c.isDigit() || c == '.' }) },
                    label = { Text(stringResource(R.string.calc_amount)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = years,
            onValueChange = { onYears(it.filter { c -> c.isDigit() }) },
            label = { Text(stringResource(R.string.calc_years)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
        if (calcType == CalcType.COMMERCIAL || calcType == CalcType.COMBINED) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = commercialRate,
                onValueChange = { onCommercialRate(it.filter { c -> c.isDigit() || c == '.' }) },
                label = { Text("商贷${stringResource(R.string.calc_rate)}") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
        if (calcType == CalcType.FUND || calcType == CalcType.COMBINED) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = fundRate,
                onValueChange = { onFundRate(it.filter { c -> c.isDigit() || c == '.' }) },
                label = { Text("公积金${stringResource(R.string.calc_rate)}") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.calc_method),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.height(6.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = method == CalcMethod.EQUAL_INTEREST,
                onClick = { onMethod(CalcMethod.EQUAL_INTEREST) },
                shape = SegmentedButtonDefaults.itemShape(0, 2),
            ) { Text(stringResource(R.string.calc_method_equal_interest)) }
            SegmentedButton(
                selected = method == CalcMethod.EQUAL_PRINCIPAL,
                onClick = { onMethod(CalcMethod.EQUAL_PRINCIPAL) },
                shape = SegmentedButtonDefaults.itemShape(1, 2),
            ) { Text(stringResource(R.string.calc_method_equal_principal)) }
        }
    }
}

@Composable
private fun ResultCard(r: CalcResult) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (r.monthlyDecrease != null) {
                // 等额本金：月供逐月递减
                ResultRow(
                    label = stringResource(R.string.calc_result_first_month),
                    value = "¥ %,.2f".format(r.monthlyFirst),
                    emphasize = true,
                )
                ResultRow(stringResource(R.string.calc_result_last_month), "¥ %,.2f".format(r.monthlyLast))
                ResultRow(stringResource(R.string.calc_result_decrease), "¥ %,.2f".format(r.monthlyDecrease))
            } else {
                // 等额本息：月供固定
                ResultRow(
                    label = stringResource(R.string.calc_result_monthly),
                    value = "¥ %,.2f".format(r.monthlyFirst),
                    emphasize = true,
                )
            }
            Spacer(Modifier.height(8.dp))
            ResultRow(stringResource(R.string.calc_result_total_interest), "¥ %,.2f".format(r.totalInterest))
            ResultRow(stringResource(R.string.calc_result_total), "¥ %,.2f".format(r.totalRepay))
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String, emphasize: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = value,
            style = if (emphasize) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun ScheduleHeaderRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        HeaderCell(stringResource(R.string.calc_schedule_index), 0.5f)
        HeaderCell(stringResource(R.string.calc_schedule_payment), 1f)
        HeaderCell(stringResource(R.string.calc_schedule_principal), 1f)
        HeaderCell(stringResource(R.string.calc_schedule_interest), 1f)
        HeaderCell(stringResource(R.string.calc_schedule_remaining), 1.1f)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HeaderCell(text: String, weight: Float) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(weight),
    )
}

/**
 * 表格数据行。样式/颜色提升到行级只取一次；
 * 文字已在计算时预格式化，滚动重组零格式化开销。
 */
@Composable
private fun ScheduleRowItem(row: ScheduleRow, modifier: Modifier = Modifier) {
    val style: TextStyle = MaterialTheme.typography.labelSmall
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(row.indexText, Modifier.weight(0.5f), style = style, color = color)
        Text(row.payment, Modifier.weight(1f), style = style, color = color)
        Text(row.principal, Modifier.weight(1f), style = style, color = color)
        Text(row.interest, Modifier.weight(1f), style = style, color = color)
        Text(row.remaining, Modifier.weight(1.1f), style = style, color = color)
    }
}

// ===== 计算逻辑 =====

// 显示用行：文字在计算时一次性格式化，滚动重组零开销
private data class ScheduleRow(
    val index: Int,
    val indexText: String,
    val payment: String,
    val principal: String,
    val interest: String,
    val remaining: String,
)

// 计算用内部数值行
private data class NumRow(
    val payment: Double,
    val principal: Double,
    val interest: Double,
    val remaining: Double,
)

private data class CalcResult(
    val monthlyFirst: Double,
    val monthlyLast: Double,
    val monthlyDecrease: Double?,       // 仅等额本金：每月递减额
    val totalInterest: Double,
    val totalRepay: Double,
    val schedule: List<ScheduleRow>,
)

// compute() 的数值结果，schedule 为数值行，交由 runCalculation 统一格式化
private data class NumResult(
    val monthlyFirst: Double,
    val monthlyLast: Double,
    val monthlyDecrease: Double?,
    val totalInterest: Double,
    val totalRepay: Double,
    val nums: List<NumRow>,
)

private fun NumResult.toCalcResult(schedule: List<ScheduleRow>) = CalcResult(
    monthlyFirst, monthlyLast, monthlyDecrease, totalInterest, totalRepay, schedule,
)

private fun List<NumRow>.toDisplay(): List<ScheduleRow> =
    mapIndexed { i, r ->
        ScheduleRow(
            index = i + 1,
            indexText = "${i + 1}",
            payment = "%,.0f".format(r.payment),
            principal = "%,.0f".format(r.principal),
            interest = "%,.0f".format(r.interest),
            remaining = "%,.0f".format(r.remaining),
        )
    }

private fun runCalculation(
    type: CalcType,
    method: CalcMethod,
    totalAmount: Double,
    fundAmount: Double,
    commercialAmount: Double,
    years: Int,
    commercialRate: Double,
    fundRate: Double,
): CalcResult? {
    val n = years * 12
    if (n <= 0) return null

    val wan = 10_000.0  // 万元换算成元

    return when (type) {
        CalcType.COMMERCIAL ->
            compute(totalAmount * wan, commercialRate / 100.0 / 12.0, n, method).let { r ->
                r.toCalcResult(r.nums.toDisplay())
            }
        CalcType.FUND ->
            compute(totalAmount * wan, fundRate / 100.0 / 12.0, n, method).let { r ->
                r.toCalcResult(r.nums.toDisplay())
            }
        CalcType.COMBINED -> {
            val c1 = compute(fundAmount * wan, fundRate / 100.0 / 12.0, n, method)
            val c2 = compute(commercialAmount * wan, commercialRate / 100.0 / 12.0, n, method)
            // 两部分期数相同（同年限），逐期相加合并明细
            val merged = (0 until n).map { i ->
                val a = c1.nums.getOrNull(i)
                val b = c2.nums.getOrNull(i)
                NumRow(
                    payment = (a?.payment ?: 0.0) + (b?.payment ?: 0.0),
                    principal = (a?.principal ?: 0.0) + (b?.principal ?: 0.0),
                    interest = (a?.interest ?: 0.0) + (b?.interest ?: 0.0),
                    remaining = (a?.remaining ?: 0.0) + (b?.remaining ?: 0.0),
                )
            }
            CalcResult(
                monthlyFirst = c1.monthlyFirst + c2.monthlyFirst,
                monthlyLast = c1.monthlyLast + c2.monthlyLast,
                monthlyDecrease = (c1.monthlyDecrease ?: 0.0) + (c2.monthlyDecrease ?: 0.0),
                totalInterest = c1.totalInterest + c2.totalInterest,
                totalRepay = c1.totalRepay + c2.totalRepay,
                schedule = merged.toDisplay(),
            )
        }
    }
}

private fun compute(principal: Double, monthlyRate: Double, n: Int, method: CalcMethod): NumResult {
    if (principal <= 0) return NumResult(0.0, 0.0, null, 0.0, 0.0, emptyList())

    val nums = ArrayList<NumRow>(n)
    var remaining = principal

    when (method) {
        CalcMethod.EQUAL_INTEREST -> {
            // 等额本息：月供固定 = P × r × (1+r)^n / ((1+r)^n - 1)
            // 第 i 期：利息 = 剩余本金 × r，本金 = 月供 − 利息（月供不变，本金逐月递增）
            val monthly = if (monthlyRate == 0.0) {
                principal / n
            } else {
                val pow = (1 + monthlyRate).pow(n)
                principal * monthlyRate * pow / (pow - 1)
            }
            for (i in 1..n) {
                val interest = remaining * monthlyRate
                val p = (monthly - interest).coerceAtMost(remaining)
                remaining = (remaining - p).coerceAtLeast(0.0)
                nums += NumRow(p + interest, p, interest, remaining)
            }
            val totalRepay = nums.sumOf { it.payment }
            return NumResult(
                monthlyFirst = monthly,
                monthlyLast = monthly,
                monthlyDecrease = null,
                totalInterest = totalRepay - principal,
                totalRepay = totalRepay,
                nums = nums,
            )
        }
        CalcMethod.EQUAL_PRINCIPAL -> {
            // 等额本金：每月本金固定 = P / n
            // 第 i 期：利息 = 剩余本金 × r，月供 = 固定本金 + 利息（逐月递减）
            val monthlyPrincipal = principal / n
            for (i in 1..n) {
                val interest = remaining * monthlyRate
                remaining = (remaining - monthlyPrincipal).coerceAtLeast(0.0)
                nums += NumRow(monthlyPrincipal + interest, monthlyPrincipal, interest, remaining)
            }
            val totalRepay = nums.sumOf { it.payment }
            return NumResult(
                monthlyFirst = nums.first().payment,
                monthlyLast = nums.last().payment,
                monthlyDecrease = monthlyPrincipal * monthlyRate,
                totalInterest = totalRepay - principal,
                totalRepay = totalRepay,
                nums = nums,
            )
        }
    }
}
