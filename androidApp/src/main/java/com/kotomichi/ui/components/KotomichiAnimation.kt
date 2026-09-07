package com.kotomichi.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

object KotomichiAnimation {
    val fast = tween<Float>(150)
    val medium = tween<Float>(300)
    val slow = tween<Float>(500)
    val mediumOffset = tween<IntOffset>(300)
    val emphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}