package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.PgpEngine
import com.example.data.PgpKeyEntity
import com.example.ui.GpgViewModel
import com.example.ui.components.KeyTypeChip

@Composable
fun KeyringScreen(viewModel: GpgViewModel) {
    val allKeys by viewModel.allKeys.collectAsState()
    val selectedKey by viewModel.selectedKey.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableIntStateOf(0) } // 0: All, 1: Secret, 2: Public
    var showGenerateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    // Filter keys
    val filteredKeys = remember(allKeys, searchQuery, filterMode) {
        allKeys.filter { key ->
            val matchesFilter = when (filterMode) {
                1 -> key.isSecretKey
                2 -> !key.isSecretKey
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                key.name.contains(searchQuery, ignoreCase = true) ||
                key.email.contains(searchQuery, ignoreCase = true) ||
                key.keyIdHex.contains(searchQuery, ignoreCase = true) ||
                key.fingerprint.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    Box(modifier = Modifier.fillMaxSize().testTag("keyring_screen")) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, email, Key ID, or fingerprint...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("key_search_input")
                )
            }

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = (filterMode == 0),
                    onClick = { filterMode = 0 },
                    label = { Text("All Keys (${allKeys.size})") },
                    modifier = Modifier.testTag("filter_all_keys")
                )
                FilterChip(
                    selected = (filterMode == 1),
                    onClick = { filterMode = 1 },
                    label = { Text("Secret (${allKeys.count { it.isSecretKey }})") },
                    modifier = Modifier.testTag("filter_secret_keys")
                )
                FilterChip(
                    selected = (filterMode == 2),
                    onClick = { filterMode = 2 },
                    label = { Text("Public (${allKeys.count { !it.isSecretKey }})") },
                    modifier = Modifier.testTag("filter_public_keys")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Keys List or Empty State
            if (filteredKeys.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching keys found" else "Keyring is Empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try a different search term." else "Generate your personal GPG keypair or import public keys from GitHub or keyservers.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ExtendedFloatingActionButton(
                                onClick = { showGenerateDialog = true },
                                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                                text = { Text("Generate Key") },
                                modifier = Modifier.testTag("empty_generate_key_button")
                            )
                            ExtendedFloatingActionButton(
                                onClick = { showImportDialog = true },
                                icon = { Icon(Icons.Default.Download, contentDescription = null) },
                                text = { Text("Import") },
                                modifier = Modifier.testTag("empty_import_key_button")
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().testTag("keys_list"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredKeys, key = { it.fingerprint }) { key ->
                        KeyCardItem(
                            key = key,
                            onClick = { viewModel.selectKey(key) }
                        )
                    }
                }
            }
        }

        // Floating Action Buttons (Generate + Import)
        if (filteredKeys.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { showImportDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.testTag("fab_import_key")
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Import Key")
                }
                ExtendedFloatingActionButton(
                    onClick = { showGenerateDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Key") },
                    modifier = Modifier.testTag("fab_generate_key")
                )
            }
        }
    }

    // Detail Dialog
    selectedKey?.let { key ->
        KeyDetailDialog(
            key = key,
            viewModel = viewModel,
            onDismiss = { viewModel.selectKey(null) }
        )
    }

    // Generate Dialog
    if (showGenerateDialog) {
        GenerateKeyDialog(
            viewModel = viewModel,
            onDismiss = { showGenerateDialog = false }
        )
    }

    // Import Dialog
    if (showImportDialog) {
        ImportKeyDialog(
            viewModel = viewModel,
            onDismiss = { showImportDialog = false }
        )
    }
}

@Composable
fun KeyCardItem(
    key: PgpKeyEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("key_item_${key.keyIdHex}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (key.isSecretKey) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (key.isSecretKey) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.secondaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (key.isSecretKey) Icons.Default.Key else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (key.isSecretKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = key.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (key.email.isNotEmpty()) {
                            Text(
                                text = key.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                KeyTypeChip(isSecret = key.isSecretKey, isDefault = key.isDefault)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Key ID & Fingerprint line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = key.keyIdHex,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "${key.algorithm} ${key.keyBits}-bit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Shortened formatted fingerprint
            Text(
                text = "FP: ${PgpEngine.formatFingerprint(key.fingerprint).take(24)}...",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
