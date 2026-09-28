package com.dushishiyi.lilv.data

import kotlinx.serialization.Serializable

/**
 * 利率变动条目：用于在首页 Banner 展示"上次→现在"的差异。
 */
@Serializable
data class RateChange(
    val scope: String,        // "deposit" / "lpr" / "fund"
    val title: String,        // "工行 1 年期"
    val oldValue: Double,
    val newValue: Double,
    val date: String,         // YYYY-MM-DD
) {
    /** 基点变化：1% = 100 BP。负值表示下调。 */
    val diffBp: Int get() = ((newValue - oldValue) * 100).toInt()

    val isLowered: Boolean get() = newValue < oldValue
    val isRaised: Boolean get() = newValue > oldValue

    fun formatDiff(): String {
        val bp = diffBp
        val arrow = when {
            bp > 0 -> "+"
            bp < 0 -> "−"
            else -> ""
        }
        val abs = kotlin.math.abs(bp)
        return "$arrow${abs}BP"
    }
}
