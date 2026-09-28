package com.dushishiyi.lilv.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * 品牌色：金融深绿 + 暖金。
 * 当 Android 12+ 启用动态色彩时，下列仅作为非动态设备的 fallback。
 */

// 浅色主题
val BrandGreen = Color(0xFF2E7D5B)
val BrandGreenDark = Color(0xFF1F5C42)
val BrandGold = Color(0xFFC9A86A)
val BrandGoldDeep = Color(0xFF9B7C45)

// 表面色（暖白，避免冷冰冰的纯白）
val BrandSurface = Color(0xFFFAFAF7)
val BrandSurfaceVariant = Color(0xFFEFF1EC)
val BrandOnSurface = Color(0xFF1A1C1A)
val BrandOnSurfaceVariant = Color(0xFF4D524D)

// 暗色主题
val BrandGreenNight = Color(0xFF7DC9A6)
val BrandGoldNight = Color(0xFFE2C68B)
val BrandSurfaceNight = Color(0xFF101413)
val BrandSurfaceVariantNight = Color(0xFF1C2220)
val BrandOnSurfaceNight = Color(0xFFE8EBE8)
val BrandOnSurfaceVariantNight = Color(0xFFB4BAB4)

// 利率涨跌色（红涨绿跌 — 国内习惯）
val RateUp = Color(0xFFD93B3B)        // 升（上调 → 利息多 → 存款人欢喜 / 借款人忧）
val RateDown = Color(0xFF2E7D5B)      // 降（下调）
val RateNeutral = Color(0xFF8A8F8A)
