/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.metrolist.music.R

/**
 * Inter (SIL Open Font License) is the closest freely licensed match for Apple's SF Pro:
 * "Inter" for text sizes like SF Pro Text, "Inter Display" for big titles like SF Pro Display.
 * Characters Inter doesn't have (Hindi, Japanese, etc.) fall back to the system font automatically.
 */
val InterText =
    FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_semibold, FontWeight.SemiBold),
        Font(R.font.inter_bold, FontWeight.Bold),
    )

val InterDisplay =
    FontFamily(
        Font(R.font.inter_display_semibold, FontWeight.Normal),
        Font(R.font.inter_display_semibold, FontWeight.Medium),
        Font(R.font.inter_display_semibold, FontWeight.SemiBold),
        Font(R.font.inter_display_bold, FontWeight.Bold),
        Font(R.font.inter_display_bold, FontWeight.ExtraBold),
        Font(R.font.inter_display_bold, FontWeight.Black),
    )

private fun style(
    family: FontFamily,
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    tracking: TextUnit,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = tracking,
)

/**
 * Apple-style type scale: bold, tightly tracked titles and slightly tightened body text.
 * Sizes stay close to Material's so existing layouts keep fitting.
 */
val AppleTypography =
    Typography(
        displayLarge = style(InterDisplay, FontWeight.Bold, 57, 64, (-1.2).sp),
        displayMedium = style(InterDisplay, FontWeight.Bold, 45, 52, (-0.9).sp),
        displaySmall = style(InterDisplay, FontWeight.Bold, 36, 43, (-0.6).sp),
        // Large Title / Title 1 / Title 2
        headlineLarge = style(InterDisplay, FontWeight.Bold, 34, 41, (-0.5).sp),
        headlineMedium = style(InterDisplay, FontWeight.Bold, 28, 34, (-0.4).sp),
        headlineSmall = style(InterDisplay, FontWeight.Bold, 22, 28, (-0.3).sp),
        // Title 3 / Headline / Subheadline
        titleLarge = style(InterText, FontWeight.SemiBold, 20, 25, (-0.4).sp),
        titleMedium = style(InterText, FontWeight.SemiBold, 16, 21, (-0.3).sp),
        titleSmall = style(InterText, FontWeight.SemiBold, 14, 19, (-0.15).sp),
        // Body / Callout / Footnote
        bodyLarge = style(InterText, FontWeight.Normal, 16, 21, (-0.3).sp),
        bodyMedium = style(InterText, FontWeight.Normal, 14, 19, (-0.15).sp),
        bodySmall = style(InterText, FontWeight.Normal, 12, 16, 0.sp),
        // Buttons, chips, captions
        labelLarge = style(InterText, FontWeight.Medium, 14, 19, (-0.15).sp),
        labelMedium = style(InterText, FontWeight.Medium, 12, 16, 0.sp),
        labelSmall = style(InterText, FontWeight.Medium, 11, 13, 0.05.sp),
    )
