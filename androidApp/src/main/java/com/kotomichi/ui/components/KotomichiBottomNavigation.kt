package com.kotomichi.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.kotomichi.app.R

enum class KotomichiDestination(val icon: ImageVector, @param:StringRes val labelRes: Int) {
    Home(Icons.Rounded.Home, R.string.nav_home),
    Belajar(Icons.AutoMirrored.Rounded.MenuBook, R.string.nav_belajar),
    Review(Icons.Rounded.Sync, R.string.nav_review),
    Progres(Icons.Rounded.CalendarMonth, R.string.nav_progres),
    Profil(Icons.Rounded.Person, R.string.nav_profil)
}

@Composable
fun KotomichiBottomNavigation(
    current: KotomichiDestination,
    onNavigate: (KotomichiDestination) -> Unit
) {
    NavigationBar {
        KotomichiDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == current,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = stringResource(destination.labelRes)
                    )
                },
                label = {
                    Text(
                        text = stringResource(destination.labelRes),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors()
            )
        }
    }
}