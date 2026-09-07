package com.kotomichi.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kotomichi.ui.theme.KotomichiSpacing

enum class KotomichiCardVariant {
    Elevated, Filled, Outlined
}

@Composable
private fun KotomichiCardBody(
    contentPadding: PaddingValues,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun KotomichiCard(
    variant: KotomichiCardVariant = KotomichiCardVariant.Filled,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(KotomichiSpacing.lg),
    content: @Composable ColumnScope.() -> Unit
) {
    when (variant) {
        KotomichiCardVariant.Elevated -> {
            if (onClick != null) {
                ElevatedCard(onClick = onClick, modifier = modifier) { KotomichiCardBody(contentPadding, content) }
            } else {
                ElevatedCard(modifier = modifier) { KotomichiCardBody(contentPadding, content) }
            }
        }
        KotomichiCardVariant.Filled -> {
            if (onClick != null) {
                Card(onClick = onClick, modifier = modifier) { KotomichiCardBody(contentPadding, content) }
            } else {
                Card(modifier = modifier) { KotomichiCardBody(contentPadding, content) }
            }
        }
        KotomichiCardVariant.Outlined -> {
            if (onClick != null) {
                OutlinedCard(onClick = onClick, modifier = modifier) { KotomichiCardBody(contentPadding, content) }
            } else {
                OutlinedCard(modifier = modifier) { KotomichiCardBody(contentPadding, content) }
            }
        }
    }
}