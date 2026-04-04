package com.dusy4.pingbox.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dusy4.pingbox.ui.screens.DebugScreen
import com.dusy4.pingbox.ui.screens.SettingsScreen
import com.dusy4.pingbox.ui.screens.RulesScreen
import com.dusy4.pingbox.ui.screens.HistoryScreen
import com.dusy4.pingbox.ui.theme.*

object Routes {
    const val SETTINGS = "settings"
    const val RULES = "rules"
    const val HISTORY = "history"
    const val DEBUG = "debug"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val iconFilled: ImageVector,
    val iconOutlined: ImageVector
)

@Composable
fun PingBoxApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navItems = remember {
        listOf(
            BottomNavItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
            BottomNavItem(Routes.RULES, "Rules", Icons.Filled.Tune, Icons.Outlined.Tune),
            BottomNavItem(Routes.HISTORY, "History", Icons.Filled.History, Icons.Outlined.History)
        )
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (currentRoute != Routes.DEBUG) {
                NavigationBar(
                    containerColor = Background.copy(alpha = 0.95f),
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(80.dp)
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .then(if (isSelected) Modifier.clip(CircleShape).background(CardBackground) else Modifier),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) item.iconFilled else item.iconOutlined,
                                        contentDescription = item.label,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            label = {
                                Text(text = item.label, style = MaterialTheme.typography.labelSmall)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Foreground,
                                selectedTextColor = Foreground,
                                unselectedIconColor = MutedForeground,
                                unselectedTextColor = MutedForeground,
                                indicatorColor = Background
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SETTINGS,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onNavigateToDebug = { navController.navigate(Routes.DEBUG) }
                )
            }
            composable(Routes.RULES) { RulesScreen() }
            composable(Routes.HISTORY) { HistoryScreen() }
            composable(Routes.DEBUG) {
                DebugScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
