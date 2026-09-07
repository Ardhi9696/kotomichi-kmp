package com.kotomichi.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable

enum class KotomichiTopBarVariant {
    Medium, Small, CenterAlignedSmall
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KotomichiTopBar(
    title: String,
    variant: KotomichiTopBarVariant = KotomichiTopBarVariant.Small,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    when (variant) {
        KotomichiTopBarVariant.Medium -> MediumTopAppBar(
            title = { Text(title, style = MaterialTheme.typography.headlineMedium) },
            navigationIcon = navigationIcon ?: {},
            actions = actions,
            scrollBehavior = scrollBehavior
        )
        KotomichiTopBarVariant.Small -> TopAppBar(
            title = { Text(title, style = MaterialTheme.typography.titleLarge) },
            navigationIcon = navigationIcon ?: {},
            actions = actions,
            scrollBehavior = scrollBehavior
        )
        KotomichiTopBarVariant.CenterAlignedSmall -> CenterAlignedTopAppBar(
            title = { Text(title, style = MaterialTheme.typography.titleMedium) },
            navigationIcon = navigationIcon ?: {},
            actions = actions,
            scrollBehavior = scrollBehavior
        )
    }
}