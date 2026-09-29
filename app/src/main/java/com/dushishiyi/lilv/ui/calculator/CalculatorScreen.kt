package com.dushishiyi.lilv.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dushishiyi.lilv.R
import kotlin.math.pow

private enum class CalcType { COMMERCIAL, FUND, COMBINED }
private enum class CalcMethod { EQUAL_INTEREST, EQUAL_PRINCIPAL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen() {
    var calcType by remember { mutableStateOf(CalcType.COMMERCIAL) }
    var method by remember { mutableStateOf(CalcMethod.EQUAL_INTEREST) }

    var totalAmount by remember { mutableStateOf("100") }        // 万元
    var fundAmount by remember { mutableStateOf("80") }          // 组合贷：公积金部分
    var commercialAmount by remember { mutableStateOf("40") }    // 组合贷：商贷部分
    var years by remember { mutableStateOf("30") }
    var commercialRate by remember { mutableStateOf("3.50") }    // LPR 5Y+ 默认
    var fundRate by remember { mutableStateOf("2.60") }          // 公积金 5Y+ 首套默认

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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.calc_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
        }

        // 贷款类型
        item {
            Text(stringResource(R.string.calc_type_preset), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = calcType == CalcType.COMMERCIAL,
                    onClick = { calcType = CalcType.COMMERCIAL },
                    label = { Text(stringResource(R.string.calc_type_commercial)) },
                )
                FilterChip(
                    selected = calcType == CalcType.FUND,
                    onClick = { calcType = CalcType.FUND },
                    label = { Text(stringResource(R.string.calc_type_fund)) },
                )
                FilterChip(
                    selected = calcType == CalcType.COMBINED,
                    onClick = { calcType = CalcType.COMBINED },
                    label = { Text(stringResource(R.string.calc_type_combined)) },
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // 金额输入
        item {
            when (calcType) {
                CalcType.COMBINED -> {
                    OutlinedTextField(
                        value = fundAmount,
                        onValueChange = { fundAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.calc_combined_fund_part)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = commercialAmount,
                        onValueChange = { commercialAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.calc_combined_commercial_part)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
                else -> {
                    OutlinedTextField(
                        value = totalAmount,
                        onValueChange = { totalAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.calc_amount)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // 年限
        item {
            OutlinedTextField(
                value = years,
                onValueChange = { years = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.calc_years)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            Spacer(Modifier.height(8.dp))
        }

        // 利率
        if (calcType == CalcType.COMMERCIAL || calcType == CalcType.COMBINED) {
            item {
                OutlinedTextField(
                    value = commercialRate,
                    onValueChange = { commercialRate = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("商贷${stringResource(R.string.calc_rate)}") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        if (calcType == CalcType.FUND || calcType == CalcType.COMBINED) {
            item {
                OutlinedTextField(
                    value = fundRate,
                    onValueChange = { fundRate = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("公积金${stringResource(R.string.calc_rate)}") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        // 还款方式
        item {
            Text(stringResource(R.string.calc_method), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = method == CalcMethod.EQUAL_INTEREST,
                    onClick = { method = CalcMethod.EQUAL_INTEREST },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text(stringResource(R.string.calc_method_equal_interest)) }
                SegmentedButton(
                    selected = method == CalcMethod.EQUAL_PRINCIPAL,
                    onClick = { method = CalcMethod.EQUAL_PRINCIPAL },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text(stringResource(R.string.calc_method_equal_principal)) }
            }
            Spacer(Modifier.height(16.dp))
        }

        // 计算按钮
        item {
            Button(
                onClick = { result = doCalc() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.calc_action))
            }
        }

        // 结果汇总 + 每期明细
        result?.let { r ->
            item {
                Spacer(Modifier.height(16.dp))
                ResultCard(r)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.calc_schedule_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                ScheduleHeader()
            }
            items(r.schedule, key = { it.index }) { row ->
                ScheduleRowItem(row)
            }
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
private fun ScheduleHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        ScheduleCell(stringResource(R.string.calc_schedule_index), 0.5f, bold = true)
        ScheduleCell(stringResource(R.string.calc_schedule_payment), 1f, bold = true)
        ScheduleCell(stringResource(R.string.calc_schedule_principal), 1f, bold = true)
        ScheduleCell(stringResource(R.string.calc_schedule_interest), 1f, bold = true)
        ScheduleCell(stringResource(R.string.calc_schedule_remaining), 1.1f, bold = true)
    }
}

@Composable
private fun ScheduleRowItem(row: ScheduleRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        ScheduleCell("${row.index}", 0.5f)
        ScheduleCell("%,.0f".format(row.payment), 1f)
        ScheduleCell("%,.0f".format(row.principal), 1f)
        ScheduleCell("%,.0f".format(row.interest), 1f)
        ScheduleCell("%,.0f".format(row.remaining), 1.1f)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ScheduleCell(
    text: String,
    weight: Float,
    bold: Boolean = false,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        color = if (bold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.weight(weight),
    )
}

// ===== 计算逻辑 =====

private data class ScheduleRow(
    val index: Int,          // 第 n 期（从 1 开始）
    val payment: Double,     // 当期月供
    val principal: Double,   // 当期本金
    val interest: Double,    // 当期利息
    val remaining: Double,   // 当期后剩余本金
)

private data class CalcResult(
    val monthlyFirst: Double,
    val monthlyLast: Double,
    val monthlyDecrease: Double?,       // 仅等额本金：每月递减额
    val totalInterest: Double,
    val totalRepay: Double,
    val schedule: List<ScheduleRow>,
)

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
            compute(totalAmount * wan, commercialRate / 100.0 / 12.0, n, method)
        CalcType.FUND ->
            compute(totalAmount * wan, fundRate / 100.0 / 12.0, n, method)
        CalcType.COMBINED -> {
            val c1 = compute(fundAmount * wan, fundRate / 100.0 / 12.0, n, method)
            val c2 = compute(commercialAmount * wan, commercialRate / 100.0 / 12.0, n, method)
            // 两部分期数相同（同年限），逐期相加合并明细
            val merged = (0 until n).map { i ->
                val a = c1.schedule.getOrNull(i)
                val b = c2.schedule.getOrNull(i)
                ScheduleRow(
                    index = i + 1,
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
                schedule = merged,
            )
        }
    }
}

private fun compute(principal: Double, monthlyRate: Double, n: Int, method: CalcMethod): CalcResult {
    if (principal <= 0) return CalcResult(0.0, 0.0, null, 0.0, 0.0, emptyList())

    val schedule = ArrayList<ScheduleRow>(n)
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
                schedule += ScheduleRow(i, p + interest, p, interest, remaining)
            }
            val totalRepay = schedule.sumOf { it.payment }
            return CalcResult(
                monthlyFirst = monthly,
                monthlyLast = monthly,
                monthlyDecrease = null,
                totalInterest = totalRepay - principal,
                totalRepay = totalRepay,
                schedule = schedule,
            )
        }
        CalcMethod.EQUAL_PRINCIPAL -> {
            // 等额本金：每月本金固定 = P / n
            // 第 i 期：利息 = 剩余本金 × r，月供 = 固定本金 + 利息（逐月递减）
            val monthlyPrincipal = principal / n
            for (i in 1..n) {
                val interest = remaining * monthlyRate
                remaining = (remaining - monthlyPrincipal).coerceAtLeast(0.0)
                schedule += ScheduleRow(i, monthlyPrincipal + interest, monthlyPrincipal, interest, remaining)
            }
            val totalRepay = schedule.sumOf { it.payment }
            return CalcResult(
                monthlyFirst = schedule.first().payment,
                monthlyLast = schedule.last().payment,
                monthlyDecrease = monthlyPrincipal * monthlyRate,
                totalInterest = totalRepay - principal,
                totalRepay = totalRepay,
                schedule = schedule,
            )
        }
    }
}
