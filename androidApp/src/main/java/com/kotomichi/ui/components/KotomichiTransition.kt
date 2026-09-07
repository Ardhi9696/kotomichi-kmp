package com.kotomichi.ui.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import com.kotomichi.ui.theme.LocalKotomichiReducedMotion

object KotomichiTransition {
    val screenEnter: EnterTransition = fadeIn(KotomichiAnimation.medium) +
        slideInVertically(KotomichiAnimation.mediumOffset) { it / 8 }
    val screenExit: ExitTransition = fadeOut(KotomichiAnimation.fast)
    val cardResultFeedback: EnterTransition = fadeIn(KotomichiAnimation.fast) + scaleIn(initialScale = 0.95f, animationSpec = KotomichiAnimation.fast)
}

@Composable
fun KotomichiTransition.screenEnterTransition(): EnterTransition =
    if (LocalKotomichiReducedMotion.current) EnterTransition.None else screenEnter

@Composable
fun KotomichiTransition.screenExitTransition(): ExitTransition =
    if (LocalKotomichiReducedMotion.current) ExitTransition.None else screenExit

@Composable
fun KotomichiTransition.cardResultFeedbackTransition(): EnterTransition =
    if (LocalKotomichiReducedMotion.current) EnterTransition.None else cardResultFeedback