/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazePerformanceMode
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.materials.HazeMaterials

/** Backdrop the glass chrome blurs; provided by MainActivity around the main content. */
val LocalAppleHazeState = staticCompositionLocalOf<HazeState?> { null }

/** Colours for the floating translucent "glass" chrome (tab bar, mini player). */
object AppleChrome {
    @Composable
    @ReadOnlyComposable
    fun isDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

    @Composable
    @ReadOnlyComposable
    fun glassFill(): Color = if (isDark()) Color(0xFF252528).copy(alpha = 0.86f) else Color(0xFFF9F9FB).copy(alpha = 0.86f)

    @Composable
    @ReadOnlyComposable
    fun glassBorder(): Color = if (isDark()) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

    /** Solid iOS bar colour shared by the tab bar and the mini player. */
    @Composable
    @ReadOnlyComposable
    fun barFill(): Color = if (isDark()) Color(0xFF1D1D1F).copy(alpha = 0.97f) else Color(0xFFF7F7F8).copy(alpha = 0.97f)

    /** Hairline between bars and content. */
    @Composable
    @ReadOnlyComposable
    fun hairline(): Color = if (isDark()) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.16f)

    /** Grey for inactive tab icons and secondary labels. */
    @Composable
    @ReadOnlyComposable
    fun inactive(): Color = if (isDark()) Color(0xFF8E8E93) else Color(0xFF6E6E73)

    @Composable
    @ReadOnlyComposable
    fun selectedFill(): Color = if (isDark()) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)
}

/**
 * Translucent glass surface with a hairline edge and a soft drop shadow.
 * Content scrolling underneath shows through the fill.
 */
fun Modifier.appleGlass(
    shape: Shape,
    fill: Color,
    border: Color,
    elevation: Dp = 10.dp,
): Modifier =
    this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.12f),
            spotColor = Color.Black.copy(alpha = 0.18f),
        ).clip(shape)
        .background(fill)
        .border(0.5.dp, border, shape)

/**
 * Frosted glass for floating chrome (tab bar, mini player): real backdrop blur of the content
 * behind it when a [LocalAppleHazeState] is provided, otherwise a translucent tint.
 */
@Composable
fun Modifier.appleChromeGlass(
    shape: Shape,
    elevation: Dp = 8.dp,
): Modifier {
    val hazeState = LocalAppleHazeState.current
    val border = AppleChrome.glassBorder()
    if (hazeState == null) {
        return appleGlass(shape = shape, fill = AppleChrome.glassFill(), border = border, elevation = elevation)
    }
    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = 0.12f),
            spotColor = Color.Black.copy(alpha = 0.18f),
        ).clip(shape)
        .hazeBlur(
            input = HazeInput.Backdrop(hazeState),
            style = ModerateBarBlur(MaterialTheme.colorScheme.surface),
            performanceMode = HazePerformanceMode.Performance,
        ).border(0.5.dp, border, shape)
}

/**
 * iOS frosted bar for the tab bar and mini player: the page scrolling underneath is blurred and tinted
 * with the bar colour. Without a haze state (or on Android 11 and older) it is the solid bar colour.
 */
@Composable
fun Modifier.appleFrostedBar(): Modifier {
    val hazeState = LocalAppleHazeState.current ?: return background(AppleChrome.barFill())
    val tint = if (AppleChrome.isDark()) Color(0xFF1D1D1F) else Color(0xFFF7F7F8)
    return hazeBlur(
        input = HazeInput.Backdrop(hazeState),
        style = ModerateBarBlur(tint),
        // Blurs a downscaled copy of the backdrop: much cheaper per frame, looks the same behind a tinted bar.
        performanceMode = HazePerformanceMode.Performance,
    )
}

/**
 * Apple-like frost for the tab bar and mini player: Haze's "regular" material (lets more colour
 * through than "thick") combined with Performance mode at the call sites keeps each frame cheap.
 * Only the documented stock material is used here, so this compiles on every Haze 2.x release.
 */
@Composable
private fun ModerateBarBlur(tint: Color) = HazeMaterials.regular(containerColor = tint)
/** Edge-to-edge frosted bar (top app bar): content scrolling underneath is blurred. */
@Composable
fun Modifier.appleBarGlass(): Modifier {
    // Frosted large-title bar, like Apple Music when content scrolls under it. Uses the cheap
    // Performance mode; without blur support it falls back to the solid page colour.
    val hazeState = LocalAppleHazeState.current ?: return background(MaterialTheme.colorScheme.background)
    return hazeBlur(
        input = HazeInput.Backdrop(hazeState),
        style = HazeMaterials.thick(containerColor = MaterialTheme.colorScheme.background),
        performanceMode = HazePerformanceMode.Performance,
    )
}
