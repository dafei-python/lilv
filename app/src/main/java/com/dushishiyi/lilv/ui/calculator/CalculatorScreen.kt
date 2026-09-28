package com.dushishiyi.lilv.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 24.dp),
    ) {
        Text(
            text = stringResource(R.string.calc_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "商贷 · 公积金 · 组合贷",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )

        Spacer(Modifier.height(12.dp))

        // 贷款类型
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

        OutlinedTextField(
            value = years,
            onValueChange = { years = it.filter { c -> c.isDigit() } },
            label = { Text(stringResource(R.string.calc_years)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        Spacer(Modifier.height(8.dp))

        if (calcType == CalcType.COMMERCIAL || calcType == CalcType.COMBINED) {
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
        if (calcType == CalcType.FUND || calcType == CalcType.COMBINED) {
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

        Button(
            onClick = {
                result = runCalculation(
                    type = calcType,
                    method = method,
                    totalAmount = totalAmount.toDoubleOrNull() ?: 0.0,
                    fundAmount = fundAmount.toDoubleOrNull() ?: 0.0,
                    commercialAmount = commercialAmount.toDoubleOrNull() ?: 0.0,
                    years = years.toIntOrNull() ?: 0,
                    commercialRate = commercialRate.toDoubleOrNull() ?: 0.0,
                    fundRate = fundRate.toDoubleOrNull() ?: 0.0,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.calc_action))
        }

        Spacer(Modifier.height(16.dp))

        result?.let { ResultCard(it) }
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
            ResultRow(
                label = stringResource(R.string.calc_result_monthly),
                value = "¥ %,.2f".format(r.monthlyFirst),
                emphasize = true,
            )
            if (r.monthlyDecrease != null) {
                ResultRow(stringResource(R.string.calc_result_first_month), "¥ %,.2f".format(r.monthlyFirst))
                ResultRow(stringResource(R.string.calc_result_decrease), "¥ %,.2f".format(r.monthlyDecrease))
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

// ===== 计算逻辑 =====

private data class CalcResult(
    val monthlyFirst: Double,           // 月供（等额本息）或首月月供（等额本金）
    val monthlyDecrease: Double? = null, // 仅等额本金：每月递减
    val totalInterest: Double,
    val totalRepay: Double,
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
        CalcType.COMMERCIAL -> {
            compute(totalAmount * wan, commercialRate / 100.0 / 12.0, n, method)
        }
        CalcType.FUND -> {
            compute(totalAmount * wan, fundRate / 100.0 / 12.0, n, method)
        }
        CalcType.COMBINED -> {
            val c1 = compute(fundAmount * wan, fundRate / 100.0 / 12.0, n, method)
            val c2 = compute(commercialAmount * wan, commercialRate / 100.0 / 12.0, n, method)
            CalcResult(
                monthlyFirst = c1.monthlyFirst + c2.monthlyFirst,
                monthlyDecrease = (c1.monthlyDecrease ?: 0.0) + (c2.monthlyDecrease ?: 0.0),
                totalInterest = c1.totalInterest + c2.totalInterest,
                totalRepay = c1.totalRepay + c2.totalRepay,
            )
        }
    }
}

private fun compute(principal: Double, monthlyRate: Double, n: Int, method: CalcMethod): CalcResult {
    if (principal <= 0) return CalcResult(0.0, null, 0.0, 0.0)
    return when (method) {
        CalcMethod.EQUAL_INTEREST -> {
            // 等额本息：月供 = P × r × (1+r)^n / ((1+r)^n - 1)
            val r = monthlyRate
            val monthly = if (r == 0.0) {
                principal / n
            } else {
                val pow = Math.pow(1 + r, n.toDouble())
                principal * r * pow / (pow - 1)
            }
            val totalRepay = monthly * n
            CalcResult(
                monthlyFirst = monthly,
                monthlyDecrease = null,
                totalInterest = totalRepay - principal,
                totalRepay = totalRepay,
            )
        }
        CalcMethod.EQUAL_PRINCIPAL -> {
            // 等额本金：每月本金 = P / n，首月月供 = P/n + P×r，每月递减 = (P/n)×r
            val monthlyPrincipal = principal / n
            val firstMonthly = monthlyPrincipal + principal * monthlyRate
            val decrease = monthlyPrincipal * monthlyRate
            // 总利息 = (n+1) × P × r / 2
            val totalInterest = (n + 1) * principal * monthlyRate / 2
            CalcResult(
                monthlyFirst = firstMonthly,
                monthlyDecrease = decrease,
                totalInterest = totalInterest,
                totalRepay = principal + totalInterest,
            )
        }
    }
}
