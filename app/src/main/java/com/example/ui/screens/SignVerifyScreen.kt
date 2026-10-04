package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GpgViewModel
import com.example.ui.components.CodeBlockView
import com.example.ui.components.ExportAscButton
import com.example.ui.components.VerificationResultCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignVerifyScreen(viewModel: GpgViewModel) {
    val context = LocalContext.current
    val secretKeys by viewModel.secretKeys.collectAsState()
    val lastSignedMessage by viewModel.lastSignedMessage.collectAsState()
    val lastVerifyResult by viewModel.lastVerifyResult.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Sign, 1: Verify

    // Sign State
    var textToSign by remember { mutableStateOf("") }
    var selectedSignerFp by remember {
        mutableStateOf(secretKeys.firstOrNull { it.isDefault }?.fingerprint ?: secretKeys.firstOrNull()?.fingerprint ?: "")
    }
    var keyDropdownExpanded by remember { mutableStateOf(false) }
    var passphrase by remember { mutableStateOf("") }
    var passphraseVisible by remember { mutableStateOf(false) }

    // Verify State
    var textToVerify by remember { mutableStateOf("") }

    // Keep default key selected if not set
    if (selectedSignerFp.isEmpty() && secretKeys.isNotEmpty()) {
        selectedSignerFp = secretKeys.firstOrNull { it.isDefault }?.fingerprint ?: secretKeys.first().fingerprint
    }

    val selectedSignerKey = secretKeys.firstOrNull { it.fingerprint == selectedSignerFp }

    Column(modifier = Modifier.fillMaxSize().testTag("sign_verify_screen")) {
        // Tab Row
        PrimaryTabRow(selectedTabIndex = activeTab) {
            Tab(
                selected = (activeTab == 0),
                onClick = { activeTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Message", fontWeight = FontWeight.SemiBold)
                    }
                },
                modifier = Modifier.testTag("tab_sign")
            )
            Tab(
                selected = (activeTab == 1),
                onClick = { activeTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify Signature", fontWeight = FontWeight.SemiBold)
                    }
                },
                modifier = Modifier.testTag("tab_verify")
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (activeTab == 0) {
                // ================= SIGN TAB =================
                Text(
                    text = "Signer Secret Key:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (secretKeys.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "No private keys found in your keyring. Go to Keys tab and generate a keypair first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable { keyDropdownExpanded = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = selectedSignerKey?.name ?: "Select Signer Key",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${selectedSignerKey?.keyIdHex ?: ""} • ${selectedSignerKey?.email ?: ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = keyDropdownExpanded,
                            onDismissRequest = { keyDropdownExpanded = false }
                        ) {
                            secretKeys.forEach { key ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("${key.name} (${key.keyIdHex})", fontWeight = FontWeight.SemiBold)
                                            if (key.email.isNotEmpty()) {
                                                Text(key.email, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedSignerFp = key.fingerprint
                                        keyDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Text to sign
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Message to Digitally Sign:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                textToSign = clip.getItemAt(0).text?.toString() ?: ""
                            }
                        },
                        modifier = Modifier.size(32.dp).testTag("paste_sign_text")
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = textToSign,
                    onValueChange = { textToSign = it },
                    placeholder = { Text("Enter message, announcement, code hash, or release notes to sign...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .testTag("sign_input_text")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Passphrase
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Signer Key Passphrase") },
                    placeholder = { Text("Leave empty if key has no passphrase") },
                    singleLine = true,
                    visualTransformation = if (passphraseVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passphraseVisible = !passphraseVisible }) {
                            Icon(
                                imageVector = if (passphraseVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("sign_passphrase_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.signText(textToSign, selectedSignerFp, passphrase)
                    },
                    enabled = textToSign.isNotBlank() && selectedSignerFp.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().testTag("submit_sign_button")
                ) {
                    Icon(Icons.Default.Draw, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Cleartext Signature")
                }

                // Output signed message
                lastSignedMessage?.let { signed ->
                    Spacer(modifier = Modifier.height(20.dp))
                    CodeBlockView(
                        text = signed,
                        title = "CLEARTEXT SIGNED MESSAGE",
                        maxHeight = 220
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportAscButton(
                            fileName = "signed_message.asc",
                            content = { signed },
                            label = "Export .asc",
                            modifier = Modifier.weight(1f).testTag("export_signed_asc_button")
                        )
                        OutlinedButton(
                            onClick = { shareText(context, "PGP Signed Message", signed) },
                            modifier = Modifier.weight(1f).testTag("share_signed_message")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share")
                        }
                    }
                }

            } else {
                // ================= VERIFY TAB =================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Paste Signed Message:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        IconButton(
                            onClick = {
                                val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    textToVerify = clip.getItemAt(0).text?.toString() ?: ""
                                }
                            },
                            modifier = Modifier.size(32.dp).testTag("paste_verify_text")
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(16.dp))
                        }
                        if (textToVerify.isNotEmpty()) {
                            IconButton(onClick = { textToVerify = "" }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = textToVerify,
                    onValueChange = { textToVerify = it },
                    placeholder = {
                        Text(
                            "-----BEGIN PGP SIGNED MESSAGE-----\nHash: SHA512\n...\n-----BEGIN PGP SIGNATURE-----\n...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("verify_input_text")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.verifySignature(textToVerify) },
                    enabled = textToVerify.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("submit_verify_button")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verify Signature")
                }

                // Verification Result
                lastVerifyResult?.let { result ->
                    Spacer(modifier = Modifier.height(20.dp))
                    VerificationResultCard(result = result)

                    if (!result.isKeyFoundInKeyring && result.keyIdHex.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { viewModel.fetchKeyserverKey(result.keyIdHex) },
                            modifier = Modifier.fillMaxWidth().testTag("fetch_missing_signer_key")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Search Keyserver for ${result.keyIdHex}")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
