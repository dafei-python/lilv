package com.dushishiyi.lilv.data

import kotlinx.serialization.Serializable

/**
 * 远端 rates.json 完整结构。
 *
 * 由 GitHub Actions 每日抓取央行/货币网/各行官网生成，
 * 部署在 GitHub Pages（如 https://lilv.dafei-python.cn/rates.json）。
 */
@Serializable
data class RatesDto(
    val generatedAt: String,
    val source: String = "",
    val deposit: DepositSection,
    val lpr: LprSection,
    val fund: FundSection,
    val mortgageReference: MortgageReferenceSection,
)

@Serializable
data class DepositSection(
    val banks: List<BankRatesDto>,
)

@Serializable
data class BankRatesDto(
    val code: String,        // icbc / abc / boc / ccb / psbc / ccb-boc...
    val name: String,        // 工商银行
    val updatedAt: String,   // 该行挂牌利率的日期，如 2025-05-20
    val rates: DepositRatesDto,
)

@Serializable
data class DepositRatesDto(
    val demand: Double,       // 活期
    val term3m: Double,
    val term6m: Double,
    val term1y: Double,
    val term2y: Double,
    val term3y: Double,
    val term5y: Double,
)

@Serializable
data class LprSection(
    val current: LprQuoteDto,
    val history: List<LprQuoteDto>,
)

@Serializable
data class LprQuoteDto(
    val date: String,         // YYYY-MM-DD
    val lpr1y: Double,
    val lpr5y: Double,
)

@Serializable
data class FundSection(
    val first5yBelow: Double,
    val first5yAbove: Double,
    val second5yBelow: Double,
    val second5yAbove: Double,
    val effectiveSince: String,
)

@Serializable
data class MortgageReferenceSection(
    val firstFloorMin: Double,
    val firstFloorMax: Double,
    val secondFloorMin: Double,
    val secondFloorMax: Double,
    val note: String,
)
