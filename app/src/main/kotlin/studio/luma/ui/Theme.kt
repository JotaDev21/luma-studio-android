package studio.luma.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF111418)
val Panel = Color(0xFF1C2026)
val Muted = Color(0xFFA0A6AF)
val Acid = Color(0xFFF0AB83)
val Lilac = Color(0xFFB9B4D8)
val Coral = Color(0xFFFAA88B)

@Composable fun LumaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Acid, onPrimary = Ink, secondary = Lilac,
        background = Ink, surface = Panel, onBackground = Color(0xFFF5F4F0), onSurface = Color(0xFFF5F4F0),
        surfaceVariant = Color(0xFF28282E), onSurfaceVariant = Muted), content = content)
}
