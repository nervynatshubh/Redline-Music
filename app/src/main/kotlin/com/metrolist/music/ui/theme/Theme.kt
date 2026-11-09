/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

val DefaultThemeColor = Color(0xFFED5564)

/** Apple Music accent ("system pink/red"). */
val AppleMusicAccent = Color(0xFFFA2D48)

/**
 * Light-mode accent: a deeper Apple red. #FA2D48 on white is only 3.8:1, below the 4.5:1 that small
 * red text (See All, artist names, Play/Shuffle) needs; #D60F32 reaches 5.3:1 and reads as the same red.
 */
val AppleMusicAccentLight = Color(0xFFD60F32)

/** Light-mode secondary text: iOS grey darkened from 3.4:1 to 5.1:1 on white for readability. */
private val AppleSecondaryLight = Color(0xFF6E6E73)

/** iOS-style palette: plain white/black backgrounds, grey grouped fills, red accent, grey secondary text. */
val AppleLightColorScheme =
    lightColorScheme(
        primary = AppleMusicAccentLight,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFE3E7),
        onPrimaryContainer = Color(0xFF5C0012),
        inversePrimary = Color(0xFFFF6B81),
        secondary = AppleSecondaryLight,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFE9E9EE),
        onSecondaryContainer = Color.Black,
        tertiary = AppleMusicAccentLight,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFFFE3E7),
        onTertiaryContainer = Color(0xFF5C0012),
        background = Color.White,
        onBackground = Color.Black,
        surface = Color.White,
        onSurface = Color.Black,
        surfaceVariant = Color(0xFFF2F2F7),
        onSurfaceVariant = AppleSecondaryLight,
        surfaceTint = Color.Transparent,
        inverseSurface = Color(0xFF1C1C1E),
        inverseOnSurface = Color.White,
        error = Color(0xFFFF3B30),
        onError = Color.White,
        outline = Color(0xFFC6C6C8),
        outlineVariant = Color(0xFFE5E5EA),
        surfaceBright = Color.White,
        surfaceDim = Color(0xFFF2F2F7),
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color.White,
        surfaceContainer = Color.White,
        surfaceContainerHigh = Color(0xFFF2F2F7),
        surfaceContainerHighest = Color(0xFFE5E5EA),
    )

val AppleDarkColorScheme =
    darkColorScheme(
        primary = AppleMusicAccent,
        onPrimary = Color.White,
        primaryContainer = Color(0xFF4A0B16),
        onPrimaryContainer = Color(0xFFFFD9DE),
        inversePrimary = Color(0xFFB0102A),
        secondary = Color(0xFF8D8D93),
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFF2C2C2E),
        onSecondaryContainer = Color.White,
        tertiary = AppleMusicAccent,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFF4A0B16),
        onTertiaryContainer = Color(0xFFFFD9DE),
        background = Color.Black,
        onBackground = Color.White,
        surface = Color.Black,
        onSurface = Color.White,
        surfaceVariant = Color(0xFF1C1C1E),
        onSurfaceVariant = Color(0xFF8D8D93),
        surfaceTint = Color.Transparent,
        inverseSurface = Color(0xFFF2F2F7),
        inverseOnSurface = Color.Black,
        error = Color(0xFFFF453A),
        onError = Color.White,
        outline = Color(0xFF38383A),
        outlineVariant = Color(0xFF2C2C2E),
        surfaceBright = Color(0xFF2C2C2E),
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF1C1C1E),
        surfaceContainer = Color.Black,
        surfaceContainerHigh = Color(0xFF1C1C1E),
        surfaceContainerHighest = Color(0xFF2C2C2E),
    )

@Composable
fun MetrolistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Determine if system dynamic colors should be used (Android S+ and default theme color)
    val useSystemDynamicColor = (themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    // Select the appropriate color scheme generation method
    val baseColorScheme = if (useSystemDynamicColor) {
        // Use standard Material 3 dynamic color functions for system wallpaper colors
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // Use materialKolor only when a specific seed color is provided
        rememberDynamicColorScheme(
            seedColor = themeColor, // themeColor is guaranteed non-default here
            isDark = darkTheme,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.TonalSpot // Keep existing style
        )
    }

    // Apply pureBlack modification if needed, similar to original logic
    val colorScheme = remember(baseColorScheme, pureBlack, darkTheme) {
        if (darkTheme && pureBlack) {
            baseColorScheme.pureBlack(true)
        } else {
            baseColorScheme
        }
    }

    // The app always uses the Apple Music palette; the computed Material scheme is kept only as a fallback reference.
    @Suppress("UNUSED_VARIABLE")
    val materialScheme = colorScheme
    MaterialTheme(
        colorScheme = if (darkTheme) AppleDarkColorScheme else AppleLightColorScheme,
        typography = AppleTypography,
        content = content,
    )
}

fun Bitmap.extractThemeColor(): Color = Color(
    Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .rankedColors(1, DefaultThemeColor.toArgb())
        .first()
)

internal fun Palette.rankedColors(
    desiredColorCount: Int,
    fallbackColor: Int,
): List<Int> = Score.score(
    swatches.associate { it.rgb to it.population },
    desiredColorCount,
    fallbackColor,
    true,
)

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
