package com.sadd.blocker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sadd.blocker.ui.DashboardViewModel

/**
 * داشبورد اسکلت فاز ۱: وضعیت سشن (از Room، با باقی‌مانده محاسبه‌شده از endEpochMs)،
 * تعداد قوانین فعال و دکمه‌های شروع/پایان سشن. جزئیات کامل در فاز ۲ و ۵.
 */
@Composable
fun DashboardScreen(viewModel: DashboardViewModel, onNavigate: (String) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("سد", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            if (uiState.sessionActive) "سشن فعال است" else "سشنی فعال نیست",
            style = MaterialTheme.typography.titleMedium,
        )
        if (uiState.sessionActive) {
            Text(
                "باقی‌مانده: ${uiState.remainingText}",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text("قوانین فعال: ${uiState.activeRulesCount}")
        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.startQuickSession() }) {
            Text("شروع سشن ۱ ساعته")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onNavigate("rules") }) {
            Text("مدیریت کلمات")
        }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { onNavigate("sessions") }) {
            Text("سشن‌های فعال")
        }
    }
}
