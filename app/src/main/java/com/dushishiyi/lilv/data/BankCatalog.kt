package com.dushishiyi.lilv.data

/**
 * 银行目录与展示元数据。
 * 与 rates.json 中的 [BankRatesDto.code] 对应。
 */
data class BankMeta(
    val code: String,
    val shortName: String,    // 工行 / 农行 ...
    val fullName: String,     // 中国工商银行
    val colorHex: String,     // 品牌色，用于头像背景
)

object BankCatalog {

    /** 用户选定：工/农/中/建/邮储 五大行 */
    val all: List<BankMeta> = listOf(
        BankMeta("icbc", "工行", "中国工商银行", "#C4161C"),
        BankMeta("abc",  "农行", "中国农业银行", "#009877"),
        BankMeta("boc",  "中行", "中国银行",     "#A4001E"),
        BankMeta("ccb",  "建行", "中国建设银行", "#0066B3"),
        BankMeta("psbc", "邮储", "中国邮政储蓄银行", "#0F6F3E"),
    )

    fun byCode(code: String): BankMeta? = all.find { it.code == code }
}

/**
 * 存款期限。利率表列。
 */
enum class DepositTerm(val key: String, val label: String, val dtoSelector: (DepositRatesDto) -> Double) {
    T3M("term_3m", "3 月", { it.term3m }),
    T6M("term_6m", "6 月", { it.term6m }),
    T1Y("term_1y", "1 年", { it.term1y }),
    T2Y("term_2y", "2 年", { it.term2y }),
    T3Y("term_3y", "3 年", { it.term3y }),
    T5Y("term_5y", "5 年", { it.term5y });

    companion object {
        val all: List<DepositTerm> = listOf(T3M, T6M, T1Y, T2Y, T3Y, T5Y)
    }
}
