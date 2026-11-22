package com.sadd.blocker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sadd.blocker.ui.PinViewModel
import com.sadd.blocker.ui.theme.ChipMint
import com.sadd.blocker.ui.theme.ChipYellow
import com.sadd.blocker.ui.theme.SaddSpacing

/**
 * PIN و امنیت.
 * PIN فقط به‌صورت هش PBKDF2 با salt تصادفی در DataStore ذخیره می‌شود، هرگز plaintext.
 * تغییر و حذف PIN نیازمند PIN فعلی است.
 * بعد از ۵ تلاش اشتباه، PIN موقتاً قفل می‌شود (مدت قفل در پیام خطا گفته می‌شود).
 */
@Composable
fun PinScreen(
    viewModel: PinViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var oldPin by rememberSaveable { mutableStateOf("") }
    var newPin by rememberSaveable { mutableStateOf("") }
    var confirmPin by rememberSaveable { mutableStateOf("") }

    if (uiState.proPromptVisible) {
        ProPromptDialog(
            text = "قفل PIN در سد پرو است.",
            onGoToPro = {
                viewModel.dismissProPrompt()
                onNavigate("pro")
            },
            onDismiss = { viewModel.dismissProPrompt() },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SaddSpacing.Large),
    ) {
        SaddTopBar(
            "PIN و امنیت",
            "PIN از تنظیمات محافظت‌شده و پایان سشن Strict محافظت می‌کند.",
            onBack = onBack,
        )
        MessageBar(message = uiState.message, onDismiss = { viewModel.dismissMessage() })
        SaddCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(SaddSpacing.Large),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // چیپ قفل: تنظیم‌شده = مینت، تنظیم‌نشده = زرد.
                SaddIconChip(
                    icon = Icons.Filled.Lock,
                    color = if (uiState.pinSet) ChipMint else ChipYellow,
                )
                Spacer(Modifier.width(SaddSpacing.Medium))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (uiState.pinSet) "PIN تنظیم شده است." else "هنوز PIN تنظیم نشده است.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(SaddSpacing.Small))
                    Text(
                        "بعد از ۵ تلاش اشتباه، PIN موقتاً قفل می‌شود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.height(SaddSpacing.Large))
        if (uiState.pinSet) {
            SaddTextField(
                value = oldPin,
                onValueChange = { oldPin = it },
                label = "PIN فعلی",
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            )
            Spacer(Modifier.height(SaddSpacing.Small))
        }
        SaddTextField(
            value = newPin,
            onValueChange = { newPin = it },
            label = if (uiState.pinSet) "PIN جدید" else "PIN جدید (۴ تا ۸ رقم)",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        )
        Spacer(Modifier.height(SaddSpacing.Small))
        SaddTextField(
            value = confirmPin,
            onValueChange = { confirmPin = it },
            label = "تکرار PIN جدید",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        )
        Spacer(Modifier.height(SaddSpacing.Medium))
        SaddButton(
            text = when {
                uiState.pinSet && uiState.isPro -> "تغییر PIN"
                uiState.pinSet -> "تغییر PIN (پرو)"
                uiState.isPro -> "تنظیم PIN"
                else -> "تنظیم PIN (پرو)"
            },
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
        )
        if (uiState.pinSet) {
            Spacer(Modifier.height(SaddSpacing.Small))
            SaddButton(
                text = "حذف PIN",
                onClick = {
                    viewModel.removePin(oldPin)
                    oldPin = ""
                },
                style = SaddButtonStyle.SECONDARY,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(SaddSpacing.Large))
        Text(
            "PIN با PBKDF2 و salt تصادفی هش می‌شود و هیچ‌جا به‌صورت متن ساده نگه داشته نمی‌شود.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(SaddSpacing.Large))
    }
}
