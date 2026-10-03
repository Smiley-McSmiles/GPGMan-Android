package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.GpgViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenerateKeyDialog(
    viewModel: GpgViewModel,
    onDismiss: () -> Unit
) {
    var algorithmType by remember { mutableStateOf("Ed25519") } // "Ed25519", "RSA", "NIST_P256", "NIST_P384", "NIST_P521", "BrainpoolP256"
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var bits by remember { mutableIntStateOf(3072) }
    var passphrase by remember { mutableStateOf("") }
    var confirmPassphrase by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var expiryDays by remember { mutableStateOf<Int?>(null) } // null = never

    var errorMsg by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Generate GPG Key Pair",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_generate_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Algorithm Selection
                Text(
                    text = "Key Algorithm & Curve",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "Ed25519" to "Ed25519 / Cv25519",
                        "RSA" to "RSA",
                        "NIST_P256" to "NIST P-256",
                        "NIST_P384" to "NIST P-384",
                        "NIST_P521" to "NIST P-521",
                        "BRAINPOOL_P256" to "Brainpool P-256"
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = (algorithmType == type),
                            onClick = { algorithmType = type },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.testTag("algo_chip_${type.lowercase()}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Informational banner about selected algorithm
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (algorithmType) {
                                "Ed25519" -> "Edwards-curve Ed25519 (Signing) + Montgomery Cv25519 / X25519 (ECDH Encryption). Modern, fast, and timing-attack resistant."
                                "NIST_P256" -> "ECDSA (Signing) + ECDH (Encryption) on NIST P-256 (secp256r1 curve, 128-bit security level)."
                                "NIST_P384" -> "ECDSA + ECDH on NIST P-384 (secp384r1 curve, 192-bit security level)."
                                "NIST_P521" -> "ECDSA + ECDH on NIST P-521 (secp521r1 curve, 256-bit security level)."
                                "BRAINPOOL_P256" -> "ECDSA + ECDH on European BSI Brainpool P-256r1 curve."
                                else -> "RSA asymmetric algorithm. Compatible with all OpenPGP clients worldwide."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Full Name *") },
                    placeholder = { Text("e.g. Satoshi Nakamoto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("key_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    placeholder = { Text("e.g. satoshi@bitcoin.org") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().testTag("key_email_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Comment
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Comment / Identifier (Optional)") },
                    placeholder = { Text("e.g. Work Laptop, Personal Git") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("key_comment_input")
                )

                // Key Size (only for RSA)
                if (algorithmType == "RSA") {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("RSA Key Size", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2048, 3072, 4096).forEach { size ->
                            FilterChip(
                                selected = (bits == size),
                                onClick = { bits = size },
                                label = { Text("$size-bit") },
                                modifier = Modifier.testTag("key_size_${size}")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Expiry
                Text("Key Expiration", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = (expiryDays == 30),
                        onClick = { expiryDays = 30 },
                        label = { Text("30 Days") },
                        modifier = Modifier.testTag("key_expiry_30")
                    )
                    FilterChip(
                        selected = (expiryDays == 90),
                        onClick = { expiryDays = 90 },
                        label = { Text("90 Days") },
                        modifier = Modifier.testTag("key_expiry_90")
                    )
                    FilterChip(
                        selected = (expiryDays == 365),
                        onClick = { expiryDays = 365 },
                        label = { Text("1 Year") },
                        modifier = Modifier.testTag("key_expiry_365")
                    )
                    FilterChip(
                        selected = (expiryDays == 730),
                        onClick = { expiryDays = 730 },
                        label = { Text("2 Years") },
                        modifier = Modifier.testTag("key_expiry_730")
                    )
                    FilterChip(
                        selected = (expiryDays == null),
                        onClick = { expiryDays = null },
                        label = { Text("Never") },
                        modifier = Modifier.testTag("key_expiry_never")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Passphrase
                Text(
                    text = "Passphrase Protection (Recommended)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Passphrase") },
                    placeholder = { Text("Leave empty for no passphrase") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("key_passphrase_input")
                )

                if (passphrase.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassphrase,
                        onValueChange = { confirmPassphrase = it },
                        label = { Text("Confirm Passphrase") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth().testTag("key_confirm_passphrase_input")
                    )
                }

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMsg = "Full Name is required"
                                return@Button
                            }
                            if (passphrase.isNotEmpty() && passphrase != confirmPassphrase) {
                                errorMsg = "Passphrases do not match"
                                return@Button
                            }
                            viewModel.generateKey(
                                algorithmType = algorithmType,
                                name = name,
                                email = email,
                                comment = comment,
                                bits = bits,
                                passphrase = passphrase,
                                expiryDays = expiryDays,
                                onSuccess = onDismiss
                            )
                        },
                        modifier = Modifier.testTag("submit_generate_key")
                    ) {
                        Text("Generate Key")
                    }
                }
            }
        }
    }
}
