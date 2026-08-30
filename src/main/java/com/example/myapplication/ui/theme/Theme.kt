package com.example.myapplication.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val WarmLightColorScheme = lightColorScheme(
    primary = TerracottaPrimary,
    onPrimary = OnTerracottaPrimary,
    primaryContainer = TerracottaPrimaryContainer,
    onPrimaryContainer = OnTerracottaContainer,

    secondary = CaramelSecondary,
    onSecondary = Color.White,
    secondaryContainer = CaramelSecondaryContainer,
    onSecondaryContainer = WarmTextPrimary,

    tertiary = SageGreen,
    onTertiary = Color.White,
    tertiaryContainer = SageGreenContainer,
    onTertiaryContainer = OnSageGreenContainer,

    background = WarmBackground,
    onBackground = WarmTextPrimary,

    surface = WarmSurface,
    onSurface = WarmTextPrimary,
    surfaceVariant = WarmSurfaceContainer,
    onSurfaceVariant = WarmTextSecondary,

    outline = WarmBorder,
    outlineVariant = WarmBorderLight,

    error = CoralDanger,
    onError = Color.White,
    errorContainer = CoralDangerContainer,
    onErrorContainer = OnCoralDangerContainer
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = WarmLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // 启用浅色状态栏与导航栏（图标变深，与燕麦暖白背景自然融合）
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}