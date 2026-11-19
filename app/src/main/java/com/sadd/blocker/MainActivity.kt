package com.sadd.blocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sadd.blocker.data.datastore.AppTheme
import com.sadd.blocker.data.datastore.ColorTheme
import com.sadd.blocker.data.datastore.SettingsStore
import com.sadd.blocker.ui.screens.AboutScreen
import com.sadd.blocker.ui.screens.AppsScreen
import com.sadd.blocker.ui.screens.BackupScreen
import com.sadd.blocker.ui.screens.DashboardScreen
import com.sadd.blocker.ui.screens.DelayScreen
import com.sadd.blocker.ui.screens.DiagnosticsScreen
import com.sadd.blocker.ui.screens.KeywordsScreen
import com.sadd.blocker.ui.screens.ModesScreen
import com.sadd.blocker.ui.screens.OnboardingScreen
import com.sadd.blocker.ui.screens.PinScreen
import com.sadd.blocker.ui.screens.SchedulingScreen
import com.sadd.blocker.ui.screens.SectionRulesScreen
import com.sadd.blocker.ui.screens.SessionsScreen
import com.sadd.blocker.ui.screens.WebsitesScreen
import com.sadd.blocker.ui.screens.rememberPressScale
import com.sadd.blocker.ui.theme.BlockerTheme
import com.sadd.blocker.ui.theme.NavyDeep
import com.sadd.blocker.ui.theme.SaddBackdrop
import com.sadd.blocker.ui.theme.SaddGlass
import com.sadd.blocker.ui.theme.SaddSpacing
import com.sadd.blocker.ui.theme.Sunny
import com.sadd.blocker.ui.theme.saddGlassSurfaceColor
import com.sadd.blocker.ui.theme.saddSpecularBorderBrush
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
        // مهاجرت یک‌باره تاخیر پیش‌فرض به فوری (نصب‌های قدیمی با ۵ ثانیه ذخیره‌شده).
        lifecycleScope.launch { settingsStore.migrateDelayDefaultIfNeeded() }
        setContent {
            val theme by settingsStore.theme.collectAsStateWithLifecycle(initialValue = AppTheme.DARK)
            val colorTheme by settingsStore.colorTheme.collectAsStateWithLifecycle(initialValue = ColorTheme.SADD)
            // مقصد شروع به راه‌اندازی وابسته است؛ null یعنی هنوز از DataStore خوانده نشده
            // و نباید با مقدار پیش‌فرض اشتباه (ویزارد برای کاربر قدیمی) گراف ساخته شود.
            val onboardingDone by settingsStore.onboardingDone
                .collectAsStateWithLifecycle<Boolean?>(initialValue = null)
            BlockerTheme(theme = theme, colorTheme = colorTheme) {
                if (onboardingDone != null) {
                    BlockerNav(onboardingDone = onboardingDone == true)
                }
            }
        }
    }
}

private data class BottomDestination(val route: String, val label: String, val icon: ImageVector)

@Composable
fun BlockerNav(onboardingDone: Boolean) {
    val navController = rememberNavController()
    val bottomDestinations = listOf(
        BottomDestination("dashboard", "داشبورد", Icons.Filled.Home),
        BottomDestination("apps", "اپ‌ها", Icons.Filled.Apps),
        BottomDestination("websites", "وب", Icons.Filled.Language),
        BottomDestination("rules", "کلمات", Icons.Filled.TextFields),
        BottomDestination("sessions", "سشن‌ها", Icons.Filled.Timer),
    )
    // پس‌زمینه Liquid Glass زیر همه صفحه‌ها؛ Scaffold شفاف است تا گرادیان دیده شود.
    SaddBackdrop {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                // ویزارد راه‌اندازی تمام‌صفحه است؛ نوار پایین روی آن نشان داده نمی‌شود.
                if (currentDestination?.route != "onboarding") {
                    // نوار پایین قرصی Liquid Glass: ظرف شناور شیشه‌ای مات با
                    // حاشیه درخشان؛ آیتم انتخاب‌شده قرص زرد توپر (Sunny) با
                    // آیکون/متن تیره دارد و هنگام فشار نرم فشرده می‌شود.
                    val barShape = RoundedCornerShape(28.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SaddSpacing.Large, vertical = SaddSpacing.Medium)
                            .shadow(
                                elevation = 12.dp,
                                shape = barShape,
                                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            )
                            .clip(barShape)
                            .background(saddGlassSurfaceColor())
                            .border(SaddGlass.BorderWidth, saddSpecularBorderBrush(), barShape)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        for (destination in bottomDestinations) {
                            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                            NavItem(
                                destination = destination,
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
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = if (onboardingDone) "dashboard" else "onboarding",
                modifier = Modifier.padding(padding),
            ) {
            composable("dashboard") {
                DashboardScreen(onNavigate = { navController.navigate(it) })
            }
            composable("onboarding") {
                OnboardingScreen(
                    onFinish = {
                        navController.navigate("dashboard") {
                            popUpTo("onboarding") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable("apps") {
                AppsScreen(onNavigate = { navController.navigate(it) })
            }
            composable("section_rules") {
                SectionRulesScreen(
                    onNavigate = { navController.navigate(it) },
                    onBack = { navController.popBackStack() },
                )
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
}

/** یک آیتم نوار پایین: قرص زرد توپر برای انتخاب‌شده؛ هنگام فشار نرم فشرده می‌شود. */
@Composable
private fun NavItem(
    destination: BottomDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemShape = RoundedCornerShape(28.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressScale by rememberPressScale(interactionSource)
    Box(
        modifier = modifier
            .scale(pressScale)
            .then(
                if (selected) {
                    Modifier.shadow(
                        elevation = 10.dp,
                        shape = itemShape,
                        ambientColor = Sunny.copy(alpha = 0.65f),
                        spotColor = Sunny.copy(alpha = 0.65f),
                    )
                } else {
                    Modifier
                },
            )
            .clip(itemShape)
            .background(
                if (selected) Sunny else Color.Transparent
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = SaddSpacing.Small, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = destination.icon,
                contentDescription = null,
                tint = if (selected) {
                    NavyDeep
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                destination.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) {
                    NavyDeep
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
