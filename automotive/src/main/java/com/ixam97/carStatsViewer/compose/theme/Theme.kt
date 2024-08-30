package com.ixam97.carStatsViewer.compose.theme

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val themedBrands = listOf(
    "Polestar",
    "VolvoCars",
    // "Toy Vehicle"
)

val clubColorsDark = darkColors(
    primary = clubBlue,
    secondary = clubBlue,
    background = Color.Black,
    onBackground = Color.White,
    surface = clubNightVariant,
    onSurface = Color.White,
    onPrimary = Color.White
)

val clubColorsLight = lightColors(
    primary = clubBlueDeep,
    secondary = clubBlueDeep,
    secondaryVariant = clubBlueDeep,
    background = clubLight,
    onBackground = Color.Black,
    surface = clubMedium,
    onSurface = Color.Black,
    onPrimary = Color.Black
)

val polestarColors = darkColors(
    primary = polestarOrange,
    secondary = polestarOrange,
    background = Color.Black,
    onBackground = Color.White,
    surface = polestarSurface,
    onSurface = Color.White,
    onPrimary = Color.White
)

val volvoColors = darkColors(
    primary = volvoBlue,
    secondary = volvoBlue,
    background = Color.Black,
    onBackground = Color.White,
    surface = polestarSurface,
    onSurface = Color.White,
    onPrimary = Color.White
)

private var pActiveElementBrush: Brush? = null
private var pHeaderLineBrush: Brush? = null

data class CarThemeBrushes(
    var activeElementBrush: Brush = CarTheme.solidBrush,
    var headerLineBrush: Brush = CarTheme.solidBrush
) {
    fun updateBrushesFrom(others: CarThemeBrushes) {
        activeElementBrush = others.activeElementBrush
        headerLineBrush = others.headerLineBrush
    }
}

object CarTheme {

    val solidBrush = Brush.linearGradient(listOf(
            darkColors().primary,
            darkColors().primary
        ))

    val brushes: CarThemeBrushes
        @Composable
        @ReadOnlyComposable
        get() = LocalBrushes.current

    val buttonCornerRadius = 20.dp
    val buttonPaddingValues = PaddingValues(horizontal = 40.dp, vertical = 20.dp)

}

internal val LocalBrushes = staticCompositionLocalOf { CarThemeBrushes() }

@Composable
fun CarTheme(carMake: String? = null, content: @Composable () -> Unit) {

    val typography: Typography
    val colors: Colors
    val brushes: CarThemeBrushes

    when (carMake) {
        "Polestar" -> {
            typography = defaultPolestarTypography
            colors = polestarColors
            brushes = CarThemeBrushes(
                Brush.horizontalGradient(listOf(colors.primary, colors.primary)),
                Brush.horizontalGradient(listOf(colors.primary, colors.primary))
            )

        }
        "VolvoCars" -> {
            colors = volvoColors
            typography = defaultTypography
            brushes = CarThemeBrushes(
                Brush.horizontalGradient(listOf(colors.primary, colors.primary)),
                Brush.horizontalGradient(listOf(colors.primary, colors.primary))
            )
        }
        "Orange" -> {
            colors = polestarColors
            typography = defaultTypography
            brushes = CarThemeBrushes(
                Brush.horizontalGradient(listOf(colors.primary, colors.primary)),
                Brush.horizontalGradient(listOf(colors.primary, colors.primary))
            )
        }
        else -> {
            colors = clubColorsDark
            typography = defaultTypography
            brushes = CarThemeBrushes(
                Brush.horizontalGradient(listOf(clubBlue, clubViolet)),
                Brush.horizontalGradient(listOf(clubVioletDark, clubViolet, clubBlue, clubBlueDark))
            )
        }
    }

    val rememberedBrushes = remember {
        brushes.copy()
    }.apply { updateBrushesFrom(brushes) }

    CompositionLocalProvider(
        LocalBrushes provides rememberedBrushes
    ) {
        MaterialTheme(
            colors = colors,
            typography = typography,
            shapes = Shapes,
            content = content
        )
    }



}

@Composable
fun PolestarTheme(content: @Composable () -> Unit) {
    val colors = polestarColors
    MaterialTheme(
        colors = colors,
        typography = polestarTypography,
        shapes = Shapes,
        content = content
    )
}