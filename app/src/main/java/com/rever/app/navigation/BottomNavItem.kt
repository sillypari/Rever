package com.rever.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom navigation items
 */
enum class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    Plan(
        route = Screen.Plan.route,
        title = "Plan",
        icon = Icons.Outlined.CalendarMonth
    ),
    Subjects(
        route = Screen.Subjects.route,
        title = "Subjects",
        icon = Icons.Outlined.LibraryBooks
    ),
    Progress(
        route = Screen.Progress.route,
        title = "Progress",
        icon = Icons.Outlined.TrendingUp
    )
}
