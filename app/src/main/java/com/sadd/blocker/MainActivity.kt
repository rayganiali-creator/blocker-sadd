package com.sadd.blocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sadd.blocker.ui.DashboardViewModel
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

/**
 * MainActivity — اسکلت ناوبری فاز ۱ با ۱۲ صفحه پرامپت.
 * تم پیش‌فرض تیره است؛ انتخاب روشن/تیره در فاز ۲ به DataStore وصل می‌شود.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlockerTheme(darkTheme = true) {
                BlockerNav()
            }
        }
    }
}

@Composable
fun BlockerNav() {
    val navController = rememberNavController()
    Scaffold { padding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding),
        ) {
            composable("dashboard") {
                val vm: DashboardViewModel = hiltViewModel()
                DashboardScreen(vm, onNavigate = { navController.navigate(it) })
            }
            composable("apps") { AppsScreen() }
            composable("websites") { WebsitesScreen() }
            composable("rules") { KeywordsScreen() }
            composable("sessions") { SessionsScreen() }
            composable("scheduling") { SchedulingScreen() }
            composable("modes") { ModesScreen() }
            composable("delay") { DelayScreen() }
            composable("pin") { PinScreen() }
            composable("diagnostics") { DiagnosticsScreen() }
            composable("backup") { BackupScreen() }
            composable("about") { AboutScreen() }
        }
    }
}
