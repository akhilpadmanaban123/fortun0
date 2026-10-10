package app.paisapilot.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF0B6E4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBFF0D8),
    onPrimaryContainer = Color(0xFF002116),
    secondary = Color(0xFF3F5BD9),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDFE3FF),
    onSecondaryContainer = Color(0xFF07164F),
    tertiary = Color(0xFF8A5A00),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE2B0),
    onTertiaryContainer = Color(0xFF2C1A00),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF6F8F6),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFF6F8F6),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFE3EAE5),
    onSurfaceVariant = Color(0xFF444B47),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEDF1EE),
    surfaceContainerHigh = Color(0xFFE7ECE8),
    surfaceContainerHighest = Color(0xFFE1E6E2),
    outline = Color(0xFF747B76),
    outlineVariant = Color(0xFFD5DCD7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FDDB6),
    onPrimary = Color(0xFF003827),
    primaryContainer = Color(0xFF00513A),
    onPrimaryContainer = Color(0xFFBFF0D8),
    secondary = Color(0xFFBAC3FF),
    onSecondary = Color(0xFF0B2596),
    secondaryContainer = Color(0xFF263FB8),
    onSecondaryContainer = Color(0xFFDFE3FF),
    tertiary = Color(0xFFFFC857),
    onTertiary = Color(0xFF482F00),
    tertiaryContainer = Color(0xFF674400),
    onTertiaryContainer = Color(0xFFFFE2B0),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F1412),
    onBackground = Color(0xFFE0E4E0),
    surface = Color(0xFF0F1412),
    onSurface = Color(0xFFE0E4E0),
    surfaceVariant = Color(0xFF3F4944),
    onSurfaceVariant = Color(0xFFBFC9C3),
    surfaceContainerLowest = Color(0xFF0A0F0D),
    surfaceContainerLow = Color(0xFF1A201D),
    surfaceContainer = Color(0xFF1C2320),
    surfaceContainerHigh = Color(0xFF262D2A),
    surfaceContainerHighest = Color(0xFF313835),
    outline = Color(0xFF89938D),
    outlineVariant = Color(0xFF333B37),
)

/** Colours with a fixed meaning in a money app, on top of the Material scheme. */
@Immutable
data class MoneyColors(
    val positive: Color,
    val negative: Color,
    val warning: Color,
    val heroStart: Color,
    val heroEnd: Color,
    val onHero: Color,
)

private val LightMoney = MoneyColors(
    positive = Color(0xFF0B7A4B),
    negative = Color(0xFFC62828),
    warning = Color(0xFFB26A00),
    heroStart = Color(0xFF0B6E4F),
    heroEnd = Color(0xFF064A3C),
    onHero = Color.White,
)

private val DarkMoney = MoneyColors(
    positive = Color(0xFF7FDDB6),
    negative = Color(0xFFFF8A80),
    warning = Color(0xFFFFC857),
    heroStart = Color(0xFF0E5C44),
    heroEnd = Color(0xFF07382D),
    onHero = Color.White,
)

val LocalMoneyColors = staticCompositionLocalOf { LightMoney }

/** Distinct, colour-blind-friendly hues for chart segments. */
val ChartPalette = listOf(
    Color(0xFF0B6E4F), Color(0xFF3F5BD9), Color(0xFFE08A00), Color(0xFFC2456B),
    Color(0xFF00838F), Color(0xFF7B52AB), Color(0xFF6D8B1F), Color(0xFF9E6B4A),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun PaisaPilotTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalMoneyColors provides if (darkTheme) DarkMoney else LightMoney,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            shapes = AppShapes,
            content = content,
        )
    }
}
