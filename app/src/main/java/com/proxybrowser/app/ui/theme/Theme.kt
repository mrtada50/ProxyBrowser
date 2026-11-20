package com.proxybrowser.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ألوان فاتحة: نيلي هادئ + أخضر للحماية (tertiary) + سماوي ثانوي
private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF0EA5E9),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF082F49),
    tertiary = Color(0xFF16A34A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDCFCE7),
    onTertiaryContainer = Color(0xFF052E16),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF111827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFEEF0F7),
    onSurfaceVariant = Color(0xFF4B5563),
    outline = Color(0xFF9CA3AF),
    outlineVariant = Color(0xFFE2E5EE),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF450A0A)
)

// ألوان داكنة: خلفية شبه سوداء مزرقة بدون سواد حاد
private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF7DD3FC),
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFF4ADE80),
    onTertiary = Color(0xFF052E16),
    tertiaryContainer = Color(0xFF14532D),
    onTertiaryContainer = Color(0xFFDCFCE7),
    background = Color(0xFF0B0D14),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF12151D),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF1E2230),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF6B7280),
    outlineVariant = Color(0xFF2A2F3D),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private fun isDarkScheme(surface: Color) = surface.luminance() < 0.5f

/** لون التحذير (كهرماني) — يُستخدم لحالة "البروكسي موقوف". */
@Composable
fun warningColor(): Color =
    if (isDarkScheme(MaterialTheme.colorScheme.surface)) Color(0xFFFBBF24) else Color(0xFFD97706)

@Composable
fun warningContainerColor(): Color =
    if (isDarkScheme(MaterialTheme.colorScheme.surface)) Color(0xFF3A2A06) else Color(0xFFFEF3C7)

@Composable
fun ProxyBrowserTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors

    // نخلي شريط الحالة وشريط التنقل بنفس لون الواجهة عشان يبان التطبيق متكامل
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colors.surface.toArgb()
            window.navigationBarColor = colors.surface.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        shapes = AppShapes,
        content = content
    )
}
