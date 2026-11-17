package com.sadd.blocker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sadd.blocker.ui.PinViewModel

/**
 * PIN و امنیت — فاز ۲.
 * PIN فقط به‌صورت SHA-256(salt + PIN) در DataStore ذخیره می‌شود، هرگز plaintext.
 * تغییر و حذف PIN نیازمند PIN فعلی است.
 */
@Composable
fun PinScreen(
    viewModel: PinViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        ScreenHeader(
            "PIN و امنیت",
            "PIN از تنظیمات محافظت‌شده و پایان سشن Strict محافظت می‌کند.",
            onBack = onBack,
        )
        MessageBar(message = uiState.message, onDismiss = { viewModel.dismissMessage() })
        Text(
            if (uiState.pinSet) "PIN تنظیم شده است." else "هنوز PIN تنظیم نشده است.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(16.dp))
        if (uiState.pinSet) {
            OutlinedTextField(
                value = oldPin,
                onValueChange = { oldPin = it },
                label = { Text("PIN فعلی") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = newPin,
            onValueChange = { newPin = it },
            label = { Text(if (uiState.pinSet) "PIN جدید" else "PIN جدید (۴ تا ۸ رقم)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = confirmPin,
            onValueChange = { confirmPin = it },
            label = { Text("تکرار PIN جدید") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                if (uiState.pinSet) {
                    viewModel.changePin(oldPin, newPin, confirmPin)
                } else {
                    viewModel.setPin(newPin, confirmPin)
                }
                oldPin = ""
                newPin = ""
                confirmPin = ""
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.pinSet) "تغییر PIN" else "تنظیم PIN")
        }
        if (uiState.pinSet) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    viewModel.removePin(oldPin)
                    oldPin = ""
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("حذف PIN")
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "PIN با SHA-256 و salt ذخیره می‌شود و هیچ‌جا به‌صورت متن ساده نگه داشته نمی‌شود.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(16.dp))
    }
}
