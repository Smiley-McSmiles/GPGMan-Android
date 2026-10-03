package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.GpgViewModel
import com.example.ui.components.PatternLockView

@Composable
fun AppLockScreen(viewModel: GpgViewModel) {
    val lockType = remember { viewModel.getLockType() } // "PIN", "PASSWORD", "PATTERN"
    var input by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var patternError by remember { mutableStateOf(false) }

    fun doUnlock(secret: String) {
        val success = viewModel.unlockApp(secret)
        if (!success) {
            patternError = true
            errorMsg = when (lockType) {
                "PASSWORD" -> "Incorrect password. Please try again."
                "PATTERN" -> "Incorrect pattern. Please try again."
                else -> "Incorrect PIN. Please try again."
            }
            input = ""
        } else {
            patternError = false
            errorMsg = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .testTag("app_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Glowing Shield Lock Badge
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (lockType) {
                        "PATTERN" -> Icons.Default.GridOn
                        "PASSWORD" -> Icons.Default.VpnKey
                        else -> Icons.Default.Lock
                    },
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Prominent, high-contrast, beautiful title
            Text(
                text = "GPGMan Locked",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = when (lockType) {
                    "PASSWORD" -> "Enter Master Password to access keyring and vault"
                    "PATTERN" -> "Drag across dots to draw your unlock pattern"
                    else -> "Enter Master PIN to access keyring and vault"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (lockType == "PATTERN") {
                // Continuous Drag Android Pattern Lock
                PatternLockView(
                    sizeDp = 270.dp,
                    isError = patternError,
                    onPatternChange = {
                        patternError = false
                        errorMsg = null
                    },
                    onPatternComplete = { patternList ->
                        val patternString = patternList.joinToString(",")
                        doUnlock(patternString)
                    },
                    testTag = "app_lock_pattern_view"
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Release finger to unlock",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // PIN or PASSWORD Field with ImeAction.Done (Single tap keyboard unlock)
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        errorMsg = null
                    },
                    label = { Text(if (lockType == "PASSWORD") "Master Password" else "Master PIN") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (lockType == "PASSWORD") KeyboardType.Password else KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            doUnlock(input)
                        }
                    ),
                    trailingIcon = {
                        if (lockType == "PASSWORD") {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .testTag("app_lock_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { doUnlock(input) },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .testTag("app_lock_unlock_button")
                ) {
                    Text("Unlock")
                }
            }

            AnimatedVisibility(visible = errorMsg != null) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = errorMsg ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
