/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.math.abs

/**
 * iOS-style scrolling momentum. UIScrollView keeps 99.8% of its speed every millisecond
 * (velocity ~ e^(-2t)), so a flick glides longer and slows down more evenly than Android's
 * default spline fling. Compose's exponential decay uses e^(-4.2 * friction * t), so a
 * friction of 2.0 / 4.2 reproduces the iOS curve.
 */
private const val IosFrictionMultiplier = 2.0f / 4.2f

private class IosFlingBehavior(
    private val decay: DecayAnimationSpec<Float>,
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        if (abs(initialVelocity) <= 1f) return initialVelocity
        var velocityLeft = initialVelocity
        var lastValue = 0f
        AnimationState(initialValue = 0f, initialVelocity = initialVelocity).animateDecay(decay) {
            val delta = value - lastValue
            val consumed = scrollBy(delta)
            lastValue = value
            velocityLeft = velocity
            // Hit the start or end of the list: stop and hand the rest to the overscroll effect.
            if (abs(delta - consumed) > 0.5f) cancelAnimation()
        }
        return velocityLeft
    }
}

@Composable
fun rememberIosFlingBehavior(): FlingBehavior =
    remember {
        IosFlingBehavior(exponentialDecay(frictionMultiplier = IosFrictionMultiplier, absVelocityThreshold = 0.5f))
    }
