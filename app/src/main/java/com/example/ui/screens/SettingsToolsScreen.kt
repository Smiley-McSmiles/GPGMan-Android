package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.GpgViewModel
import com.example.ui.components.PatternLockView
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.TertiaryEmerald
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsToolsScreen(viewModel: GpgViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allKeys by viewModel.allKeys.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    var showLockDialog by remember { mutableStateOf(false) }
    var showBackupPrivateKeysDialog by remember { mutableStateOf(false) }
    var showAuditLogsDialog by remember { mutableStateOf(false) }
    var showGitHubDialog by remember { mutableStateOf(false) }
    var showKeyserverDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val hasLock = viewModel.hasMasterPin()
    val lockType = viewModel.getLockType()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("settings_tools_screen")
    ) {
        Text(
            text = "Settings & Security",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "App security, key backup, and cryptographic tools",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Security Section
        Text(
            text = "APP SECURITY & LOCK",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (lockType) {
                                "PATTERN" -> Icons.Default.GridOn
                                "PASSWORD" -> Icons.Default.VpnKey
                                else -> Icons.Default.Lock
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Master Lock ($lockType)", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = if (hasLock) "Protected with $lockType lock" else "Disabled (No lock set)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = hasLock,
                        onCheckedChange = { showLockDialog = true },
                        modifier = Modifier.testTag("toggle_pin_switch")
                    )
                }

                if (hasLock) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showLockDialog = true },
                            modifier = Modifier.weight(1f).testTag("change_lock_button")
                        ) {
                            Text("Change Lock")
                        }
                        Button(
                            onClick = { viewModel.lockAppNow() },
                            modifier = Modifier.weight(1f).testTag("lock_app_now_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lock Now")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Key Tools Section
        Text(
            text = "KEY MANAGEMENT & BACKUPS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column {
                SettingsActionRow(
                    icon = Icons.Default.Code,
                    title = "Fetch GitHub Public Keys",
                    subtitle = "Import GPG keys for any GitHub developer (@username)",
                    onClick = { showGitHubDialog = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                SettingsActionRow(
                    icon = Icons.Default.Search,
                    title = "Keyserver Query (keys.openpgp.org)",
                    subtitle = "Search public directory by email or Key ID",
                    onClick = { showKeyserverDialog = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                SettingsActionRow(
                    icon = Icons.Default.Backup,
                    title = "Backup Keyring Public Keys",
                    subtitle = "Export all ${allKeys.size} public key(s) in ASCII armored format",
                    onClick = {
                        val bundle = allKeys.joinToString("\n\n") { it.armoredPublicKey }
                        shareText(context, "GPGMan Keyring Public Keys Backup", bundle)
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                SettingsActionRow(
                    icon = Icons.Default.VpnKey,
                    title = "Backup Keyring Private Keys (Secret Keys)",
                    subtitle = "Export all secret key(s) with security confirmation",
                    onClick = { showBackupPrivateKeysDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Audit & System Section
        Text(
            text = "AUDIT & ABOUT",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column {
                SettingsActionRow(
                    icon = Icons.Default.History,
                    title = "Security Audit Log",
                    subtitle = "${auditLogs.size} recorded cryptographic operations",
                    onClick = { showAuditLogsDialog = true }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                SettingsActionRow(
                    icon = Icons.Default.Info,
                    title = "About GPGMan & Attributions",
                    subtitle = "Attributions • OpenPGP • Hardware KeyStore",
                    onClick = { showAboutDialog = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(72.dp))
    }

    // Lock Setup Dialog (PIN / Password / Pattern)
    if (showLockDialog) {
        SetLockDialog(
            hasLock = hasLock,
            currentType = lockType,
            onDismiss = { showLockDialog = false },
            onSaveLock = { secret, type ->
                viewModel.setMasterLock(secret, type)
                showLockDialog = false
            }
        )
    }

    // Backup Private Keys Dialog (Security Confirmation)
    if (showBackupPrivateKeysDialog) {
        AlertDialog(
            onDismissRequest = { showBackupPrivateKeysDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = AccentAmber) },
            title = { Text("Backup Secret Keys") },
            text = {
                Column {
                    Text(
                        text = "SECURITY WARNING: Exporting your private keys will produce ASCII-armored secret key blocks. Anyone with access to these keys (and their passphrases) can decrypt confidential communications and sign documents in your name.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Ensure you store this export strictly in an encrypted, offline backup location.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val secretBundle = viewModel.getSecretKeysBackupBundle()
                            if (secretBundle.isBlank()) {
                                viewModel.postMessage("No private keys found in keyring to backup.", isError = true)
                            } else {
                                shareText(context, "GPGMan Keyring Secret Keys Backup", secretBundle)
                            }
                            showBackupPrivateKeysDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_export_secret_keys")
                ) {
                    Text("Export Secret Keys")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackupPrivateKeysDialog = false }) { Text("Cancel") }
            }
        )
    }

    // GitHub Dialog
    if (showGitHubDialog) {
        var user by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showGitHubDialog = false },
            title = { Text("Fetch from GitHub") },
            text = {
                Column {
                    Text("Enter GitHub username to download and import their public GPG keys:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = user,
                        onValueChange = { user = it },
                        label = { Text("GitHub Username") },
                        placeholder = { Text("e.g. torvalds") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("github_fetch_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.fetchGitHubKeys(user) { showGitHubDialog = false }
                    },
                    enabled = user.isNotBlank()
                ) {
                    Text("Fetch Keys")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGitHubDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Keyserver Dialog
    if (showKeyserverDialog) {
        var query by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showKeyserverDialog = false },
            title = { Text("Keyserver Lookup") },
            text = {
                Column {
                    Text("Search keys.openpgp.org by email or Key ID:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Email or Key ID") },
                        placeholder = { Text("e.g. user@example.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("keyserver_fetch_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.fetchKeyserverKey(query) { showKeyserverDialog = false }
                    },
                    enabled = query.isNotBlank()
                ) {
                    Text("Search")
                }
            },
            dismissButton = {
                TextButton(onClick = { showKeyserverDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Audit Logs Dialog
    if (showAuditLogsDialog) {
        Dialog(onDismissRequest = { showAuditLogsDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(540.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Security Audit Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { viewModel.clearAuditLogs() }) {
                            Text("Clear", color = ErrorRed)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (auditLogs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No audit events recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        val dateFormat = remember { SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()) }
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            auditLogs.forEach { log ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val icon = when (log.status) {
                                        "SUCCESS" -> Icons.Default.CheckCircle to TertiaryEmerald
                                        "WARNING" -> Icons.Default.Warning to AccentAmber
                                        else -> Icons.Default.Error to ErrorRed
                                    }
                                    Icon(icon.first, contentDescription = null, tint = icon.second, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(log.details, style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            text = "${dateFormat.format(Date(log.timestamp))} • ${log.action}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            }
                        }
                    }
                }
            }
        }
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            icon = { Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("About GPGMan") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "A full-featured, secure OpenPGP (GnuPG) Manager for Android.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "PROJECT ATTRIBUTIONS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "WOOSAH (Lead Architect & Maintainer)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gemini 3.8 Flash - Lead Engineer",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("• OpenPGP Engine: Bouncy Castle OpenPGP v1.78.1", style = MaterialTheme.typography.bodySmall)
                    Text("• Supported Algorithms: Ed25519, Cv25519 (X25519), NIST P-256/384/521, Brainpool, RSA", style = MaterialTheme.typography.bodySmall)
                    Text("• Hardware Security: Android KeyStore AES-256-GCM hardware-backed encryption for all secret keys", style = MaterialTheme.typography.bodySmall)
                    Text("• Database: Room Local Database (100% offline, zero cloud tracking)", style = MaterialTheme.typography.bodySmall)
                    Text("• License: MIT License", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SetLockDialog(
    hasLock: Boolean,
    currentType: String,
    onDismiss: () -> Unit,
    onSaveLock: (secret: String, type: String) -> Unit
) {
    var selectedType by remember { mutableStateOf(currentType) } // "PIN", "PASSWORD", "PATTERN"
    var secret by remember { mutableStateOf("") }
    var confirmSecret by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val patternNodes = remember { mutableStateListOf<Int>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hasLock) "Configure Master Lock" else "Set Master Lock") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Select your preferred security lock method to restrict access to GPGMan.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Lock Type Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = (selectedType == "PIN"),
                        onClick = { selectedType = "PIN"; errorMsg = null },
                        label = { Text("PIN") },
                        modifier = Modifier.testTag("lock_type_pin")
                    )
                    FilterChip(
                        selected = (selectedType == "PASSWORD"),
                        onClick = { selectedType = "PASSWORD"; errorMsg = null },
                        label = { Text("Password") },
                        modifier = Modifier.testTag("lock_type_password")
                    )
                    FilterChip(
                        selected = (selectedType == "PATTERN"),
                        onClick = { selectedType = "PATTERN"; errorMsg = null },
                        label = { Text("Pattern") },
                        modifier = Modifier.testTag("lock_type_pattern")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (selectedType) {
                    "PATTERN" -> {
                        Text(
                            text = if (patternNodes.isEmpty()) "Drag across at least 3 dots to draw pattern:"
                            else "Pattern drawn (${patternNodes.size} dots connected)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (patternNodes.size >= 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            PatternLockView(
                                sizeDp = 240.dp,
                                isError = errorMsg != null,
                                onPatternChange = {
                                    patternNodes.clear()
                                    patternNodes.addAll(it)
                                    errorMsg = null
                                },
                                onPatternComplete = { pattern ->
                                    patternNodes.clear()
                                    patternNodes.addAll(pattern)
                                    if (pattern.size < 3) {
                                        errorMsg = "Pattern must connect at least 3 dots"
                                    } else {
                                        errorMsg = null
                                    }
                                },
                                testTag = "setup_pattern_lock_view"
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { patternNodes.clear(); errorMsg = null },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Pattern")
                        }
                    }
                    "PASSWORD" -> {
                        OutlinedTextField(
                            value = secret,
                            onValueChange = { secret = it; errorMsg = null },
                            label = { Text("New Password") },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth().testTag("new_password_input")
                        )
                        if (secret.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = confirmSecret,
                                onValueChange = { confirmSecret = it; errorMsg = null },
                                label = { Text("Confirm Password") },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth().testTag("confirm_password_input")
                            )
                        }
                    }
                    else -> { // PIN
                        OutlinedTextField(
                            value = secret,
                            onValueChange = { secret = it; errorMsg = null },
                            label = { Text("New PIN (4-8 digits)") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth().testTag("new_pin_input")
                        )
                        if (secret.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = confirmSecret,
                                onValueChange = { confirmSecret = it; errorMsg = null },
                                label = { Text("Confirm PIN") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth().testTag("confirm_pin_input")
                            )
                        }
                    }
                }

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedType == "PATTERN") {
                        if (patternNodes.size < 3) {
                            errorMsg = "Pattern must contain at least 3 nodes"
                            return@Button
                        }
                        onSaveLock(patternNodes.joinToString(","), "PATTERN")
                    } else {
                        if (secret.isEmpty() && hasLock) {
                            // Remove lock
                            onSaveLock("", selectedType)
                            return@Button
                        }
                        if (secret.isNotEmpty() && secret != confirmSecret) {
                            errorMsg = "Inputs do not match"
                            return@Button
                        }
                        onSaveLock(secret, selectedType)
                    }
                }
            ) {
                Text(
                    if (hasLock && selectedType != "PATTERN" && secret.isEmpty()) "Remove Lock"
                    else "Save Lock"
                )
            }
        },
        dismissButton = {
            Row {
                if (hasLock) {
                    TextButton(onClick = { onSaveLock("", selectedType) }) {
                        Text("Remove Lock", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
