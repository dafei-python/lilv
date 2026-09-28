package com.dushishiyi.lilv.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    primaryContainer = BrandGreenDark,
    onPrimaryContainer = Color.White,
    secondary = BrandGoldDeep,
    onSecondary = Color.White,
    tertiary = BrandGold,
    onTertiary = Color(0xFF2A1F08),
    background = BrandSurface,
    onBackground = BrandOnSurface,
    surface = BrandSurface,
    onSurface = BrandOnSurface,
    surfaceVariant = BrandSurfaceVariant,
    onSurfaceVariant = BrandOnSurfaceVariant,
    outline = Color(0xFFB6BAB5),
    error = RateUp,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = BrandGreenNight,
    onPrimary = Color(0xFF003824),
    primaryContainer = BrandGreenDark,
    onPrimaryContainer = Color(0xFFA6E6CB),
    secondary = BrandGoldNight,
    onSecondary = Color(0xFF3A2D0A),
    tertiary = BrandGoldNight,
    onTertiary = Color(0xFF3A2D0A),
    background = BrandSurfaceNight,
    onBackground = BrandOnSurfaceNight,
    surface = BrandSurfaceNight,
    onSurface = BrandOnSurfaceNight,
    surfaceVariant = BrandSurfaceVariantNight,
    onSurfaceVariant = BrandOnSurfaceVariantNight,
    outline = Color(0xFF6B726B),
    error = Color(0xFFFFB4A2),
    onError = Color(0xFF690000),
)

/**
 * 应用主题。
 * - 暗色：跟随系统
 * - 动态色彩：Android 12+ 启用（Material You），低于 12 fallback 品牌色
 */
@Composable
fun LilvTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LilvTypography,
        shapes = LilvShapes,
        content = content,
    )
}
