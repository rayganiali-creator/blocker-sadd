package com.sadd.blocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.ui.screens.AboutScreen
import com.sadd.blocker.ui.screens.AppsScreen
import com.sadd.blocker.ui.screens.BackupScreen
import com.sadd.blocker.ui.screens.DashboardScreen
import com.sadd.blocker.ui.screens.DelayScreen
import com.sadd.blocker.ui.screens.DiagnosticsScreen
import com.sadd.blocker.ui.screens.KeywordsScreen
import com.sadd.blocker.ui.screens.ModesScreen
import com.sadd.blocker.ui.screens.PinScreen
import com.sadd.blocker.ui.screens.SchedulingScreen
import com.sadd.blocker.ui.screens.SessionsScreen
import com.sadd.blocker.ui.screens.WebsitesScreen
import com.sadd.blocker.ui.theme.BlockerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * MainActivity — ناوبری کامل ۱۲ صفحه + تم روشن/تیره از DataStore.
 * نوار پایین ۵ مقصد اصلی را دارد؛ صفحه‌های ثانویه از داشبورد باز می‌شوند
 * و دکمه بازگشت خودشان را دارند.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsStore: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkTheme by settingsStore.darkTheme.collectAsStateWithLifecycle(initialValue = true)
            BlockerTheme(darkTheme = darkTheme) {
                BlockerNav()
            }
        }
    }
}

private data class BottomDestination(val route: String, val label: String)

@Composable
fun BlockerNav() {
    val navController = rememberNavController()
    val bottomDestinations = listOf(
        BottomDestination("dashboard", "داشبورد"),
        BottomDestination("apps", "اپ‌ها"),
        BottomDestination("websites", "وب"),
        BottomDestination("rules", "کلمات"),
        BottomDestination("sessions", "سشن‌ها"),
    )
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                for (destination in bottomDestinations) {
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            when (destination.route) {
                                "dashboard" -> Icon(Icons.Filled.Home, contentDescription = null)
                                "apps" -> Icon(Icons.Filled.Apps, contentDescription = null)
                                "websites" -> Icon(Icons.Filled.Language, contentDescription = null)
                                "rules" -> Icon(Icons.Filled.TextFields, contentDescription = null)
                                "sessions" -> Icon(Icons.Filled.Timer, contentDescription = null)
                            }
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding),
        ) {
            composable("dashboard") {
                DashboardScreen(onNavigate = { navController.navigate(it) })
            }
            composable("apps") {
                AppsScreen(onNavigate = { navController.navigate(it) })
            }
            composable("websites") {
                WebsitesScreen(onNavigate = { navController.navigate(it) })
            }
            composable("rules") {
                KeywordsScreen(onNavigate = { navController.navigate(it) })
            }
            composable("sessions") {
                SessionsScreen(onNavigate = { navController.navigate(it) })
            }
            composable("scheduling") {
                SchedulingScreen(
                    onNavigate = { navController.navigate(it) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("modes") {
                ModesScreen(
                    onNavigate = { navController.navigate(it) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("delay") {
                DelayScreen(
                    onNavigate = { navController.navigate(it) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("pin") {
                PinScreen(
                    onNavigate = { navController.navigate(it) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("diagnostics") {
                DiagnosticsScreen(
                    onNavigate = { navController.navigate(it) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("backup") {
                BackupScreen(
                    onNavigate = { navController.navigate(it) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("about") {
                AboutScreen(
                    onNavigate = { navController.navigate(it) },
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
