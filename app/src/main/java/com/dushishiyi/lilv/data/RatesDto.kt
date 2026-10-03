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
    val reference: ReferenceSection? = null,
    val metals: MetalsSection? = null,
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

@Serializable
data class ReferenceSection(
    val deposit1y: Double,                 // 五大行 1 年定存均值（自动）
    val treasury10y: Double,               // 10 年期国债到期收益率（每日自动）
    val insuranceCap: Double,              // 普通型寿险预定利率上限（监管值）
    val wealthManagement: Double,          // 银行业理财平均年化（随官方报告自动）
    val csi300_10y_annualized: Double,     // 沪深 300 近 10 年年化（每日自动）
    val updatedDescription: String = "",
    val updatedAt: String = "",
)

/**
 * 贵金属与美元汇率（第三 Tab「行情」）。
 * 黄金/白银来自上海黄金交易所，美元中间价来自中国外汇交易中心，
 * 东方财富为备源；各品种含近半年日线，每日自动更新。
 */
@Serializable
data class MetalsSection(
    val updatedAt: String = "",
    val gold: MetalQuoteDto? = null,
    val silver: MetalQuoteDto? = null,
    val fx: MetalQuoteDto? = null,
)

@Serializable
data class MetalQuoteDto(
    val name: String,
    val price: Double,
    val unit: String = "",
    val change: Double = 0.0,
    val changePct: Double = 0.0,
    val high: Double = 0.0,
    val low: Double = 0.0,
    val asOf: String = "",
    val source: String = "",
    val history: List<MetalPointDto> = emptyList(),
)

@Serializable
data class MetalPointDto(
    val date: String,
    val close: Double,
)
