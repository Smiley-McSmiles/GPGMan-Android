package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
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
import com.example.ui.components.VerificationResultCard
import com.example.ui.components.copyToClipboard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EncryptDecryptScreen(viewModel: GpgViewModel) {
    val context = LocalContext.current
    val allKeys by viewModel.allKeys.collectAsState()
    val lastEncryptedText by viewModel.lastEncryptedText.collectAsState()
    val lastDecryptedResult by viewModel.lastDecryptedResult.collectAsState()

    var mainTab by remember { mutableIntStateOf(0) } // 0: Encrypt, 1: Decrypt
    var subMode by remember { mutableIntStateOf(0) } // 0: Text, 1: File

    // Encrypt State
    var textToEncrypt by remember { mutableStateOf("") }
    val selectedRecipients = remember { mutableStateListOf<String>() }
    var useSymmetricPassword by remember { mutableStateOf(false) }
    var symmetricPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // File Encrypt State
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedFileSize by remember { mutableStateOf(0L) }
    var selectedFileMime by remember { mutableStateOf("application/octet-stream") }
    var saveEncryptedFileToVault by remember { mutableStateOf(true) }
    var vaultItemTitle by remember { mutableStateOf("") }

    // Decrypt State
    var textToDecrypt by remember { mutableStateOf("") }
    var decryptPassphrase by remember { mutableStateOf("") }
    var decryptPassphraseVisible by remember { mutableStateOf(false) }

    // File Decrypt State
    var decryptFileUri by remember { mutableStateOf<Uri?>(null) }
    var decryptFileName by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileUri = uri
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    selectedFileName = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "file.dat" else "file.dat"
                    selectedFileSize = if (sizeIndex >= 0) cursor.getLong(sizeIndex) else 0L
                    vaultItemTitle = selectedFileName
                }
            }
            selectedFileMime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        }
    }

    val decryptFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            decryptFileUri = uri
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst()) {
                    decryptFileName = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "encrypted.gpg" else "encrypted.gpg"
                }
            }
            // Read file content
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bytes = stream.readBytes()
                    textToDecrypt = String(bytes, Charsets.UTF_8)
                }
            } catch (_: Exception) {}
        }
    }

    Column(modifier = Modifier.fillMaxSize().testTag("encrypt_decrypt_screen")) {
        // Main Tab Row: Encrypt vs Decrypt
        PrimaryTabRow(selectedTabIndex = mainTab) {
            Tab(
                selected = (mainTab == 0),
                onClick = { mainTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Encrypt", fontWeight = FontWeight.SemiBold)
                    }
                },
                modifier = Modifier.testTag("tab_encrypt")
            )
            Tab(
                selected = (mainTab == 1),
                onClick = { mainTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Decrypt", fontWeight = FontWeight.SemiBold)
                    }
                },
                modifier = Modifier.testTag("tab_decrypt")
            )
        }

        // Sub Mode: Text vs File
        SecondaryTabRow(selectedTabIndex = subMode) {
            Tab(
                selected = (subMode == 0),
                onClick = { subMode = 0 },
                text = { Text("Text / Message", fontSize = 12.sp) },
                modifier = Modifier.testTag("submode_text")
            )
            Tab(
                selected = (subMode == 1),
                onClick = { subMode = 1 },
                text = { Text("File / Document", fontSize = 12.sp) },
                modifier = Modifier.testTag("submode_file")
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (mainTab == 0) {
                // ================= ENCRYPT TAB =================
                if (subMode == 0) {
                    // --- Text Encryption ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Message to Encrypt:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        textToEncrypt = clip.getItemAt(0).text?.toString() ?: ""
                                    }
                                },
                                modifier = Modifier.size(32.dp).testTag("paste_encrypt_text_button")
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(16.dp))
                            }
                            if (textToEncrypt.isNotEmpty()) {
                                IconButton(onClick = { textToEncrypt = "" }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = textToEncrypt,
                        onValueChange = { textToEncrypt = it },
                        placeholder = { Text("Type or paste confidential message here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("encrypt_input_text")
                    )

                } else {
                    // --- File Encryption ---
                    Text(
                        text = "Select File to Encrypt:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { filePickerLauncher.launch("*/*") }
                            .testTag("pick_file_to_encrypt_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedFileName.isNotEmpty()) selectedFileName else "Tap to choose file",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (selectedFileSize > 0) "${selectedFileSize / 1024} KB • $selectedFileMime"
                                    else "Any format (PDF, Images, Zip, Docs...)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (selectedFileUri != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = saveEncryptedFileToVault,
                                onCheckedChange = { saveEncryptedFileToVault = it }
                            )
                            Text("Save encrypted copy into local Vault", style = MaterialTheme.typography.bodySmall)
                        }
                        if (saveEncryptedFileToVault) {
                            OutlinedTextField(
                                value = vaultItemTitle,
                                onValueChange = { vaultItemTitle = it },
                                label = { Text("Vault Item Title") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("vault_item_title_input")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Recipient Selection ---
                Text(
                    text = "Recipients (Keyring Public Keys):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (allKeys.isEmpty()) {
                    Text(
                        text = "No keys in keyring yet. You can use a symmetric password below, or import a public key in the Keys tab.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allKeys.forEach { key ->
                            val isSelected = selectedRecipients.contains(key.fingerprint)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selectedRecipients.remove(key.fingerprint)
                                    else selectedRecipients.add(key.fingerprint)
                                },
                                label = { Text("${key.name} (${key.keyIdHex.takeLast(8)})", fontSize = 11.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                } else null
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Symmetric Password Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = useSymmetricPassword,
                        onCheckedChange = { useSymmetricPassword = it },
                        modifier = Modifier.testTag("checkbox_symmetric_password")
                    )
                    Text(
                        text = "Also encrypt with Symmetric Passphrase (PBE)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (useSymmetricPassword) {
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = symmetricPassword,
                        onValueChange = { symmetricPassword = it },
                        label = { Text("Symmetric Password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("symmetric_password_input")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Encrypt Action Button
                Button(
                    onClick = {
                        val pass = if (useSymmetricPassword) symmetricPassword else ""
                        if (subMode == 0) {
                            viewModel.encryptText(
                                plainText = textToEncrypt,
                                recipientFingerprints = selectedRecipients.toList(),
                                symmetricPassphrase = pass
                            )
                        } else {
                            if (selectedFileUri != null) {
                                try {
                                    val bytes = context.contentResolver.openInputStream(selectedFileUri!!)?.use { it.readBytes() }
                                    if (bytes != null) {
                                        viewModel.encryptFile(
                                            fileBytes = bytes,
                                            fileName = selectedFileName,
                                            mimeType = selectedFileMime,
                                            recipientFingerprints = selectedRecipients.toList(),
                                            symmetricPassphrase = pass,
                                            saveToVault = saveEncryptedFileToVault,
                                            vaultTitle = vaultItemTitle
                                        )
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    },
                    enabled = (if (subMode == 0) textToEncrypt.isNotBlank() else selectedFileUri != null) &&
                            (selectedRecipients.isNotEmpty() || (useSymmetricPassword && symmetricPassword.isNotEmpty())),
                    modifier = Modifier.fillMaxWidth().testTag("submit_encrypt_button")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (subMode == 0) "Encrypt Message" else "Encrypt File")
                }

                // Output Armored Block
                lastEncryptedText?.let { encrypted ->
                    Spacer(modifier = Modifier.height(20.dp))
                    CodeBlockView(
                        text = encrypted,
                        title = "ENCRYPTED PGP MESSAGE",
                        maxHeight = 200
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { shareText(context, "Encrypted PGP Message", encrypted) },
                            modifier = Modifier.weight(1f).testTag("share_encrypted_text")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share")
                        }
                        Button(
                            onClick = {
                                viewModel.saveVaultNote(
                                    title = "Encrypted Note (${SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())})",
                                    category = "NOTE",
                                    content = textToEncrypt,
                                    recipientFingerprints = selectedRecipients.toList(),
                                    symmetricPassphrase = if (useSymmetricPassword) symmetricPassword else ""
                                )
                            },
                            modifier = Modifier.weight(1f).testTag("save_encrypted_to_vault")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to Vault")
                        }
                    }
                }

            } else {
                // ================= DECRYPT TAB =================
                if (subMode == 0) {
                    // Decrypt Text
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Paste Encrypted Message (PGP Armor):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        textToDecrypt = clip.getItemAt(0).text?.toString() ?: ""
                                    }
                                },
                                modifier = Modifier.size(32.dp).testTag("paste_decrypt_text_button")
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(16.dp))
                            }
                            if (textToDecrypt.isNotEmpty()) {
                                IconButton(onClick = { textToDecrypt = "" }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = textToDecrypt,
                        onValueChange = { textToDecrypt = it },
                        placeholder = {
                            Text(
                                "-----BEGIN PGP MESSAGE-----\n...\n-----END PGP MESSAGE-----",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("decrypt_input_text")
                    )

                } else {
                    // Decrypt File
                    Text(
                        text = "Pick Encrypted File (.gpg / .asc):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { decryptFilePickerLauncher.launch("*/*") }
                            .testTag("pick_file_to_decrypt_card"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (decryptFileName.isNotEmpty()) decryptFileName else "Choose encrypted file (.gpg / .asc)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (textToDecrypt.isNotEmpty()) "Loaded ${textToDecrypt.length} characters" else "Select file from storage",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Passphrase Input
                OutlinedTextField(
                    value = decryptPassphrase,
                    onValueChange = { decryptPassphrase = it },
                    label = { Text("Private Key or Symmetric Passphrase") },
                    placeholder = { Text("Leave empty if key has no passphrase") },
                    singleLine = true,
                    visualTransformation = if (decryptPassphraseVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { decryptPassphraseVisible = !decryptPassphraseVisible }) {
                            Icon(
                                imageVector = if (decryptPassphraseVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("decrypt_passphrase_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.decryptText(textToDecrypt, decryptPassphrase)
                    },
                    enabled = textToDecrypt.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().testTag("submit_decrypt_button")
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Decrypt Payload")
                }

                // Decryption Results
                lastDecryptedResult?.let { result ->
                    Spacer(modifier = Modifier.height(20.dp))

                    if (result.isSuccess) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "DECRYPTED PLAINTEXT",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row {
                                        IconButton(onClick = { copyToClipboard(context, "Decrypted Text", result.decryptedText) }) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { shareText(context, "Decrypted Content", result.decryptedText) }) {
                                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                SelectionContainer {
                                    Text(
                                        text = result.decryptedText,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                if (result.decryptedByKeyId.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Decrypted with: ${result.decryptedByKeyId}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Signature verification if attached
                        result.signatureResult?.let { sigResult ->
                            Spacer(modifier = Modifier.height(12.dp))
                            VerificationResultCard(sigResult)
                        }

                    } else {
                        // Error Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Decryption Failed",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = result.errorMessage ?: "Unknown error occurred.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(72.dp))
        }
    }
}
