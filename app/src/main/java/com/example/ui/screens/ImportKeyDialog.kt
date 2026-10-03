package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.GpgViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportKeyDialog(
    viewModel: GpgViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var armoredText by remember { mutableStateOf("") }
    var githubUser by remember { mutableStateOf("") }
    var keyserverQuery by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val sb = StringBuilder()
            var count = 0
            for (uri in uris) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val content = stream.bufferedReader().readText()
                        if (content.isNotBlank()) {
                            if (sb.isNotEmpty()) sb.append("\n\n")
                            sb.append(content.trim())
                            count++
                        }
                    }
                } catch (e: Exception) {
                    // Handled
                }
            }
            if (sb.isNotEmpty()) {
                armoredText = if (armoredText.isBlank()) sb.toString() else "$armoredText\n\n$sb"
                selectedTab = 0 // Switch to text tab
                viewModel.postMessage("Loaded $count key file(s). Tap 'Import Keys' below.")
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
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
                    Text(
                        text = "Import GPG Keys",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_import_dialog")) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs: Text, GitHub, Keyserver
                PrimaryTabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = (selectedTab == 0),
                        onClick = { selectedTab = 0 },
                        text = { Text("Paste Text", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = (selectedTab == 1),
                        onClick = { selectedTab = 1 },
                        text = { Text("GitHub", fontSize = 12.sp) }
                    )
                    Tab(
                        selected = (selectedTab == 2),
                        onClick = { selectedTab = 2 },
                        text = { Text("Keyserver", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> {
                        // Paste Text Tab
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Paste ASCII Armored Key Block:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = clipboard.primaryClip
                                        if (clip != null && clip.itemCount > 0) {
                                            armoredText = clip.getItemAt(0).text?.toString() ?: ""
                                        }
                                    },
                                    modifier = Modifier.height(32.dp).testTag("paste_clipboard_key_button")
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Paste", fontSize = 11.sp)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                OutlinedButton(
                                    onClick = { filePicker.launch("*/*") },
                                    modifier = Modifier.height(32.dp).testTag("pick_key_file_button")
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Files (Multi)", fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = armoredText,
                            onValueChange = { armoredText = it },
                            placeholder = {
                                Text(
                                    "-----BEGIN PGP PUBLIC KEY BLOCK-----\n...\n-----END PGP PUBLIC KEY BLOCK-----",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .testTag("import_key_text_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.importKey(armoredText, onSuccess = onDismiss)
                            },
                            enabled = armoredText.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().testTag("submit_import_key_button")
                        ) {
                            Text("Import Key")
                        }
                    }

                    1 -> {
                        // GitHub Tab
                        Text(
                            text = "Fetch public GPG keys registered with a GitHub user account directly from GitHub.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = githubUser,
                            onValueChange = { githubUser = it },
                            label = { Text("GitHub Username") },
                            placeholder = { Text("e.g. torvalds or octocat") },
                            singleLine = true,
                            leadingIcon = { Text("@", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                            modifier = Modifier.fillMaxWidth().testTag("github_username_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.fetchGitHubKeys(githubUser, onSuccess = onDismiss)
                            },
                            enabled = githubUser.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().testTag("submit_github_fetch_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Fetch from GitHub")
                        }
                    }

                    2 -> {
                        // Keyserver Tab
                        Text(
                            text = "Query keys.openpgp.org by email address, 8/16-char Key ID (e.g. 0x12345678), or 40-char fingerprint.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = keyserverQuery,
                            onValueChange = { keyserverQuery = it },
                            label = { Text("Email, Key ID, or Fingerprint") },
                            placeholder = { Text("e.g. user@example.com or 0xABCD1234") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("keyserver_query_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                viewModel.fetchKeyserverKey(keyserverQuery, onSuccess = onDismiss)
                            },
                            enabled = keyserverQuery.isNotBlank(),
                            modifier = Modifier.fillMaxWidth().testTag("submit_keyserver_query_button")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Query keys.openpgp.org")
                        }
                    }
                }
            }
        }
    }
}
