/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.metrolist.music.R
import kotlin.math.roundToInt

private val GlassFill = Color(0xFF2C2C2E).copy(alpha = 0.72f)
private val GlassEdge = Color.White.copy(alpha = 0.14f)
private val GlassDivider = Color.White.copy(alpha = 0.12f)
private val GlassText = Color.White
private val GlassSecondary = Color.White.copy(alpha = 0.6f)
private val SleepRed = Color(0xFFFA2D48)

/**
 * Apple-style sleep timer: a translucent frosted panel over a blurred player.
 * Tap a time to start right away, "End of Song" to stop after this track, or Custom for any length.
 * Touch and hold a time to make it the default.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppleSleepTimerDialog(
    isActive: Boolean,
    defaultMinutes: Int,
    onStart: (minutes: Int) -> Unit,
    onEndOfSong: () -> Unit,
    onTurnOff: () -> Unit,
    onSetDefault: (minutes: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Blur the player behind the panel (Android 12+), and keep the dim light so it feels like glass.
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.let { window ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    window.attributes = window.attributes.also { it.blurBehindRadius = 64 }
                    window.setDimAmount(0.25f)
                } else {
                    window.setDimAmount(0.55f)
                }
            }
        }

        val view = LocalView.current
        val tick = { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) }
        var showCustom by remember { mutableStateOf(false) }
        var customMinutes by remember { mutableFloatStateOf(defaultMinutes.toFloat().coerceIn(5f, 180f)) }
        val presets = listOf(15, 30, 45, 60, 90, 120)
        val shape = RoundedCornerShape(22.dp)

        Column(
            modifier =
                Modifier
                    .padding(horizontal = 28.dp)
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(GlassFill)
                    .border(0.5.dp, GlassEdge, shape),
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 12.dp, start = 16.dp, end = 16.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.bedtime),
                    contentDescription = null,
                    tint = SleepRed,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    text = stringResource(R.string.sleep_timer),
                    color = GlassText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.sleep_timer_subtitle),
                    color = GlassSecondary,
                    fontSize = 13.sp,
                )
            }

            HorizontalDivider(thickness = 0.5.dp, color = GlassDivider)

            if (isActive) {
                SleepRow(
                    label = stringResource(R.string.sleep_timer_turn_off),
                    color = SleepRed,
                    onClick = {
                        tick()
                        onTurnOff()
                    },
                )
                HorizontalDivider(thickness = 0.5.dp, color = GlassDivider)
            }

            presets.forEach { minutes ->
                SleepRow(
                    label =
                        if (minutes % 60 == 0) {
                            pluralStringResource(R.plurals.sleep_timer_hours, minutes / 60, minutes / 60)
                        } else {
                            pluralStringResource(R.plurals.minute, minutes, minutes)
                        },
                    badge = if (minutes == defaultMinutes) stringResource(R.string.sleep_timer_default_badge) else null,
                    onClick = {
                        tick()
                        onStart(minutes)
                    },
                    onLongClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onSetDefault(minutes)
                    },
                )
                HorizontalDivider(thickness = 0.5.dp, color = GlassDivider, modifier = Modifier.padding(start = 20.dp))
            }

            SleepRow(
                label = stringResource(R.string.end_of_song),
                onClick = {
                    tick()
                    onEndOfSong()
                },
            )
            HorizontalDivider(thickness = 0.5.dp, color = GlassDivider, modifier = Modifier.padding(start = 20.dp))

            SleepRow(
                label = stringResource(R.string.sleep_timer_custom),
                trailing = if (showCustom) "" else "›",
                onClick = { showCustom = !showCustom },
            )

            AnimatedVisibility(
                visible = showCustom,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 14.dp)) {
                    Text(
                        text = pluralStringResource(R.plurals.minute, customMinutes.roundToInt(), customMinutes.roundToInt()),
                        color = GlassText,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Slider(
                        value = customMinutes,
                        onValueChange = { customMinutes = it },
                        valueRange = 5f..180f,
                        steps = (180 - 5) / 5 - 1,
                        colors =
                            SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = SleepRed,
                                inactiveTrackColor = Color.White.copy(alpha = 0.18f),
                                activeTickColor = Color.Transparent,
                                inactiveTickColor = Color.Transparent,
                            ),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GlassPill(
                            label = stringResource(R.string.set_as_default),
                            filled = false,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                tick()
                                onSetDefault(customMinutes.roundToInt())
                            },
                        )
                        GlassPill(
                            label = stringResource(R.string.sleep_timer_start),
                            filled = true,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                tick()
                                onStart(customMinutes.roundToInt())
                            },
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = GlassDivider)
            Text(
                text = stringResource(R.string.sleep_timer_hold_hint),
                color = GlassSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SleepRow(
    label: String,
    onClick: () -> Unit,
    color: Color = GlassText,
    badge: String? = null,
    trailing: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .heightIn(min = 48.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f),
        )
        if (badge != null) {
            Text(
                text = badge,
                color = SleepRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        if (!trailing.isNullOrEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(text = trailing, color = GlassSecondary, fontSize = 20.sp)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GlassPill(
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .clip(shape)
                .background(if (filled) SleepRed else Color.White.copy(alpha = 0.12f))
                .combinedClickable(onClick = onClick)
                .heightIn(min = 44.dp)
                .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}
