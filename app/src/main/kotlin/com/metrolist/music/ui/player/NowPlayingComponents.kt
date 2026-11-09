/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.utils.makeTimeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Colours of the full-screen "now playing" UI. It always sits on a dark, artwork-tinted background. */
object NowPlayingColors {
    val Primary = Color.White
    val Secondary = Color.White.copy(alpha = 0.62f)
    val Tertiary = Color.White.copy(alpha = 0.45f)
    val Track = Color.White.copy(alpha = 0.2f)
    val Fill = Color.White.copy(alpha = 0.6f)
    val ButtonFill = Color.White.copy(alpha = 0.14f)
    val ButtonPressed = Color.White.copy(alpha = 0.12f)
}

/**
 * Frosted-glass surface: a translucent white fill that is brighter at the top, with a thin
 * specular edge. On the soft blurred background this reads as glass without a live backdrop blur.
 */
fun Modifier.nowPlayingGlass(
    shape: Shape,
    fillAlpha: Float = 0.12f,
): Modifier =
    this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = (fillAlpha + 0.06f).coerceAtMost(1f)),
                    Color.White.copy(alpha = (fillAlpha - 0.04f).coerceAtLeast(0.03f)),
                ),
            ),
            shape,
        ).border(
            width = 0.8.dp,
            brush =
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.36f), Color.White.copy(alpha = 0.06f)),
                ),
            shape = shape,
        )

/** Monochrome dark scheme used for content shown on top of the now-playing background (e.g. the queue). */
@Composable
fun rememberNowPlayingGlassColorScheme(): ColorScheme =
    remember {
        val base = Color(0xFF1C1C1E)
        val raised = Color(0xFF2C2C2E)
        darkColorScheme(
            primary = Color.White,
            onPrimary = Color.Black,
            primaryContainer = Color.White.copy(alpha = 0.18f),
            onPrimaryContainer = Color.White,
            inversePrimary = Color.White.copy(alpha = 0.5f),
            secondary = Color.White.copy(alpha = 0.7f),
            onSecondary = Color.Black,
            secondaryContainer = Color.White.copy(alpha = 0.12f),
            onSecondaryContainer = Color.White,
            tertiary = Color.White.copy(alpha = 0.7f),
            onTertiary = Color.Black,
            tertiaryContainer = Color.White.copy(alpha = 0.12f),
            onTertiaryContainer = Color.White,
            background = base,
            onBackground = Color.White,
            surface = base,
            onSurface = Color.White,
            surfaceVariant = Color.White.copy(alpha = 0.1f),
            onSurfaceVariant = Color.White.copy(alpha = 0.6f),
            surfaceContainerLowest = base,
            surfaceContainerLow = base,
            surfaceContainer = raised,
            surfaceContainerHigh = raised,
            surfaceContainerHighest = Color(0xFF3A3A3C),
            outline = Color.White.copy(alpha = 0.3f),
            outlineVariant = Color.White.copy(alpha = 0.12f),
        )
    }

private data class NowPlayingArtworkTones(
    val base: Color,
    val blurred: ImageBitmap?,
)

private val DefaultArtworkTones = NowPlayingArtworkTones(base = Color(0xFF26262E), blurred = null)

/** Separable box blur on a small pixel buffer; three passes approximate a gaussian. */
private fun boxBlur(pixels: IntArray, width: Int, height: Int, radius: Int) {
    val tmp = IntArray(pixels.size)
    fun pass(src: IntArray, dst: IntArray, horizontal: Boolean) {
        val outer = if (horizontal) height else width
        val inner = if (horizontal) width else height
        for (o in 0 until outer) {
            for (i in 0 until inner) {
                var a = 0
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                for (k in -radius..radius) {
                    val j = (i + k).coerceIn(0, inner - 1)
                    val c = if (horizontal) src[o * width + j] else src[j * width + o]
                    a += (c ushr 24) and 0xFF
                    r += (c shr 16) and 0xFF
                    g += (c shr 8) and 0xFF
                    b += c and 0xFF
                    count++
                }
                val idx = if (horizontal) o * width + i else i * width + o
                dst[idx] = ((a / count) shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
            }
        }
    }
    pass(pixels, tmp, horizontal = true)
    pass(tmp, pixels, horizontal = false)
}

private fun Bitmap.toArtworkTones(): NowPlayingArtworkTones {
    val size = 32
    val small = Bitmap.createScaledBitmap(this, size, size, true)
    val pixels = IntArray(size * size)
    small.getPixels(pixels, 0, size, 0, 0, size, size)
    repeat(3) { boxBlur(pixels, size, size, radius = 3) }
    val blurred = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)

    val palette = Palette.from(small).maximumColorCount(12).generate()
    val baseSwatch = palette.darkMutedSwatch ?: palette.darkVibrantSwatch ?: palette.dominantSwatch
    val base =
        baseSwatch?.let { swatch ->
            val hsl = FloatArray(3)
            ColorUtils.colorToHSL(swatch.rgb, hsl)
            hsl[1] = hsl[1].coerceAtMost(0.7f)
            hsl[2] = hsl[2].coerceIn(0.12f, 0.3f)
            Color(ColorUtils.HSLToColor(hsl))
        } ?: DefaultArtworkTones.base
    return NowPlayingArtworkTones(base = base, blurred = blurred.asImageBitmap())
}

/**
 * Apple-style animated background: the artwork, pre-blurred once on a tiny bitmap, drawn huge and
 * slowly rotating in two layers. Motion only changes layer transforms (no recomposition or redraw),
 * so it stays smooth and cheap on every Android version. Motion runs only while [animate] is true.
 */
@Composable
fun NowPlayingBackground(
    thumbnailUrl: String?,
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var tones by remember { mutableStateOf(DefaultArtworkTones) }

    LaunchedEffect(thumbnailUrl) {
        if (thumbnailUrl == null) return@LaunchedEffect
        val extracted =
            withContext(Dispatchers.Default) {
                runCatching {
                    val request =
                        ImageRequest
                            .Builder(context)
                            .data(thumbnailUrl)
                            .size(96, 96)
                            .allowHardware(false)
                            .memoryCacheKey("now_playing_bg_$thumbnailUrl")
                            .build()
                    context.imageLoader.execute(request).image?.toBitmap()?.toArtworkTones()
                }.getOrNull()
            }
        if (extracted != null) tones = extracted
    }

    val baseColor by animateColorAsState(tones.base, tween(durationMillis = 1000), label = "bgBase")

    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        var last = 0L
        while (isActive) {
            withFrameNanos { now ->
                // ~30fps is plenty for this very slow motion and halves the work.
                if (last == 0L) {
                    last = now
                } else if (now - last >= 33_000_000L) {
                    phase += (now - last) / 1_000_000_000f
                    last = now
                }
            }
        }
    }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(baseColor),
    ) {
        val side = max(maxWidth.value, maxHeight.value).dp * 1.6f

        Crossfade(
            targetState = tones.blurred,
            animationSpec = tween(durationMillis = 900),
            label = "blurredArtwork",
            modifier = Modifier.fillMaxSize(),
        ) { bitmap ->
            if (bitmap != null) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.Low,
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .requiredSize(side)
                                .graphicsLayer {
                                    rotationZ = phase * 6f
                                    val s = 1f + 0.06f * sin(phase * 0.35f)
                                    scaleX = s
                                    scaleY = s
                                },
                    )
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.Low,
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .requiredSize(side)
                                .graphicsLayer {
                                    alpha = 0.55f
                                    rotationZ = 180f - phase * 9f
                                    translationX = size.width * 0.12f * cos(phase * 0.22f)
                                    translationY = size.height * 0.10f * sin(phase * 0.18f)
                                    scaleX = 1.25f
                                    scaleY = 1.25f
                                },
                    )
                }
            }
        }

        // Darken toward the bottom so white controls stay readable.
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.12f),
                            0.5f to Color.Black.copy(alpha = 0.22f),
                            1f to Color.Black.copy(alpha = 0.45f),
                        ),
                    ),
        )
    }
}

@Composable
fun NowPlayingGrabber(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .size(width = 38.dp, height = 5.dp)
                .background(Color.White.copy(alpha = 0.38f), CircleShape),
    )
}

/**
 * Thin progress bar with no thumb. Dragging is relative (touching does not jump), and the bar
 * thickens while held.
 */
@Composable
fun NowPlayingProgressBar(
    value: () -> Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onDraggingChange: (Boolean) -> Unit = {},
) {
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 0f
    // The position is read only while drawing or dragging, so playback progress redraws the bar
    // without recomposing the surrounding controls.
    val currentValue by rememberUpdatedState(value)
    val fraction: () -> Float = {
        if (span > 0f) ((currentValue() - valueRange.start) / span).coerceIn(0f, 1f) else 0f
    }

    var dragging by remember { mutableStateOf(false) }
    // Latest Apple Music scrubber (iOS 16+): a soft rounded bar with no knob. While dragging it grows
    // taller, stretches to the edges and turns bright white.
    val barHeight by animateDpAsState(
        targetValue = if (dragging) 12.dp else 7.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "barHeight",
    )
    val horizontalInset by animateDpAsState(
        targetValue = if (dragging) 0.dp else 4.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "barInset",
    )
    val fillColor by animateColorAsState(
        targetValue = if (dragging) NowPlayingColors.Primary else NowPlayingColors.Fill,
        label = "barFill",
    )

    val currentOnChange by rememberUpdatedState(onValueChange)
    val currentOnFinished by rememberUpdatedState(onValueChangeFinished)
    val currentOnDragging by rememberUpdatedState(onDraggingChange)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .fillMaxWidth()
                .height(32.dp)
                .pointerInput(enabled, span, valueRange.start, isRtl) {
                    if (!enabled || span <= 0f) return@pointerInput
                    var dragFraction = 0f
                    fun finish() {
                        dragging = false
                        currentOnDragging(false)
                        currentOnFinished()
                    }
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragFraction = fraction()
                            dragging = true
                            currentOnDragging(true)
                        },
                        onDragEnd = { finish() },
                        onDragCancel = { finish() },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val delta = (if (isRtl) -dragAmount else dragAmount) / size.width
                            dragFraction = (dragFraction + delta).coerceIn(0f, 1f)
                            currentOnChange(valueRange.start + dragFraction * span)
                        },
                    )
                },
    ) {
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    // Springs never overshoot here, but keep padding/height safely non-negative anyway.
                    .padding(horizontal = horizontalInset.coerceAtLeast(0.dp))
                    .height(barHeight.coerceAtLeast(1.dp)),
        ) {
            val radius = CornerRadius(size.height / 2f)
            drawRoundRect(color = NowPlayingColors.Track, cornerRadius = radius)
            val fillWidth = size.width * fraction()
            if (fillWidth > 0f) {
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(if (isRtl) size.width - fillWidth else 0f, 0f),
                    size = Size(fillWidth, size.height),
                    cornerRadius = radius,
                )
            }
        }
    }
}

@Composable
fun NowPlayingTimeRow(
    positionMs: () -> Long,
    durationMs: Long?,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    val currentPosition by rememberUpdatedState(positionMs)
    val seconds by remember { derivedStateOf { currentPosition() / 1000L } }
    val shownMs = seconds * 1000L
    val color by animateColorAsState(
        targetValue = if (highlighted) NowPlayingColors.Primary else NowPlayingColors.Tertiary,
        label = "timeColor",
    )
    val style =
        androidx.compose.ui.text.TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFeatureSettings = "tnum",
            color = color,
        )
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
    ) {
        Text(
            text = makeTimeString(shownMs),
            style = style,
            maxLines = 1,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        if (!badge.isNullOrBlank()) {
            Text(
                text = badge,
                style = style.copy(fontSize = 10.sp, letterSpacing = 0.4.sp, color = NowPlayingColors.Secondary),
                maxLines = 1,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .nowPlayingGlass(RoundedCornerShape(6.dp), fillAlpha = 0.1f)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
        Text(
            modifier = Modifier.align(Alignment.CenterEnd),
            text =
                if (durationMs != null && durationMs > 0) {
                    "-" + makeTimeString((durationMs - shownMs).coerceAtLeast(0L))
                } else {
                    ""
                },
            style = style,
            maxLines = 1,
        )
    }
}

/** Borderless transport glyph that shrinks and shows a soft circle while pressed. */
@Composable
fun NowPlayingTransportButton(
    @DrawableRes icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonSize: Dp = 72.dp,
    iconSize: Dp = 38.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.84f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
        label = "transportScale",
    )
    val pressedFill by animateColorAsState(
        targetValue = if (pressed) NowPlayingColors.ButtonPressed else Color.Transparent,
        label = "transportFill",
    )
    // A light tick under the finger, like the iPhone's play/skip buttons.
    val view = LocalView.current

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(buttonSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.background(pressedFill, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        onClick()
                    },
                ),
    ) {
        Crossfade(targetState = icon, label = "transportIcon") { res ->
            Icon(
                painter = painterResource(res),
                contentDescription = contentDescription,
                tint = NowPlayingColors.Primary.copy(alpha = if (enabled) 1f else 0.35f),
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

/** Small translucent round button used next to the title (favourite, more). */
@Composable
fun NowPlayingCircleButton(
    @DrawableRes icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = NowPlayingColors.Primary,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(32.dp)
                .nowPlayingGlass(CircleShape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = tint.copy(alpha = if (enabled) tint.alpha else 0.35f),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
fun NowPlayingVolumeRow(
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    @DrawableRes lowIcon: Int,
    @DrawableRes highIcon: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var dragging by remember { mutableStateOf(false) }
    val iconTint by animateColorAsState(
        targetValue = if (dragging) NowPlayingColors.Primary else NowPlayingColors.Tertiary,
        label = "volumeIconTint",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            painter = painterResource(lowIcon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        NowPlayingProgressBar(
            value = { volume.coerceIn(0f, 1f) },
            valueRange = 0f..1f,
            onValueChange = onVolumeChange,
            onValueChangeFinished = {},
            enabled = enabled,
            onDraggingChange = { dragging = it },
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        Icon(
            painter = painterResource(highIcon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Bottom-bar toggle: plain glyph, or a solid white disc with a dark glyph while active. */
@Composable
fun NowPlayingBarButton(
    @DrawableRes icon: Int,
    contentDescription: String?,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // iOS 14 Apple Music: plain white glyph, red rounded tile with a white glyph when on.
    val tileColor by animateColorAsState(
        targetValue = if (active) Color(0xFFFA2D48) else Color.Transparent,
        label = "barTile",
    )
    val glyphColor by animateColorAsState(
        targetValue = if (active) Color.White else NowPlayingColors.Primary.copy(alpha = 0.9f),
        label = "barGlyph",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .size(44.dp)
                .padding(4.dp)
                .background(tileColor, RoundedCornerShape(8.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = if (enabled) glyphColor else glyphColor.copy(alpha = 0.3f),
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * Apple Music "Playing Next" mode button (Shuffle, Repeat, Autoplay, Automix): a wide pill that is
 * glass when off and solid white with a dark glyph when on. When unavailable it is greyed out, and a tap
 * runs [onUnavailableClick] (e.g. explaining that Autoplay must be turned off first).
 */
@Composable
fun NowPlayingQueueModeButton(
    @DrawableRes icon: Int,
    contentDescription: String?,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    available: Boolean = true,
    onUnavailableClick: () -> Unit = {},
    compact: Boolean = false,
) {
    val shape = RoundedCornerShape(if (compact) 7.dp else 12.dp)
    // Compact = iOS 14 queue toggles: a soft grey square when on, bare glyph when off.
    val fill by animateColorAsState(
        targetValue =
            when {
                compact && active && available -> Color.White.copy(alpha = 0.26f)
                compact -> Color.Transparent
                active && available -> NowPlayingColors.Primary
                else -> Color.White.copy(alpha = 0.1f)
            },
        label = "modeFill",
    )
    val glyph by animateColorAsState(
        targetValue =
            when {
                !available -> NowPlayingColors.Primary.copy(alpha = 0.3f)
                compact -> NowPlayingColors.Primary
                active -> Color.Black.copy(alpha = 0.8f)
                else -> NowPlayingColors.Primary
            },
        label = "modeGlyph",
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .then(if (compact) Modifier else Modifier.height(40.dp))
                .clip(shape)
                .background(fill, shape)
                .clickable(
                    role = Role.Switch,
                    onClick = if (available) onClick else onUnavailableClick,
                ),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = glyph,
            modifier = Modifier.size(if (compact) 18.dp else 22.dp),
        )
    }
}
