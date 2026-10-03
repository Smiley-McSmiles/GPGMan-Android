package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.GpgViewModel
import com.example.ui.MainTab
import com.example.ui.components.LoadingOverlay
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.EncryptDecryptScreen
import com.example.ui.screens.KeyringScreen
import com.example.ui.screens.SettingsToolsScreen
import com.example.ui.screens.SignVerifyScreen
import com.example.ui.screens.VaultScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                GpgApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpgApp(viewModel: GpgViewModel = viewModel()) {
    val isAppLocked by viewModel.isAppLocked.collectAsState()
    val currentTab by viewModel.currentTab.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val loadingText by viewModel.loadingText.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messageEvent.collect { msg ->
            snackbarHostState.showSnackbar(msg.text)
        }
    }

    if (isAppLocked) {
        AppLockScreen(viewModel = viewModel)
        return
    }

    // Handle back button on sub-tabs
    BackHandler(enabled = currentTab != MainTab.KEYS) {
        viewModel.setTab(MainTab.KEYS)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("main_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "GPGMan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = when (currentTab) {
                                    MainTab.KEYS -> "Keyring & Identities"
                                    MainTab.ENCRYPT_DECRYPT -> "Encryption & Decryption"
                                    MainTab.SIGN_VERIFY -> "Digital Signatures"
                                    MainTab.VAULT -> "Secure Encrypted Vault"
                                    MainTab.SETTINGS -> "Security & Tools"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (viewModel.hasMasterPin()) {
                        IconButton(
                            onClick = { viewModel.lockAppNow() },
                            modifier = Modifier.testTag("app_bar_lock_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock App")
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = (currentTab == MainTab.KEYS),
                    onClick = { viewModel.setTab(MainTab.KEYS) },
                    icon = { Icon(Icons.Default.Key, contentDescription = "Keys") },
                    label = { Text("Keys") },
                    modifier = Modifier.testTag("nav_keys")
                )
                NavigationBarItem(
                    selected = (currentTab == MainTab.ENCRYPT_DECRYPT),
                    onClick = { viewModel.setTab(MainTab.ENCRYPT_DECRYPT) },
                    icon = { Icon(Icons.Default.Lock, contentDescription = "Encrypt") },
                    label = { Text("Encrypt") },
                    modifier = Modifier.testTag("nav_encrypt")
                )
                NavigationBarItem(
                    selected = (currentTab == MainTab.SIGN_VERIFY),
                    onClick = { viewModel.setTab(MainTab.SIGN_VERIFY) },
                    icon = { Icon(Icons.Default.Draw, contentDescription = "Sign") },
                    label = { Text("Sign") },
                    modifier = Modifier.testTag("nav_sign")
                )
                NavigationBarItem(
                    selected = (currentTab == MainTab.VAULT),
                    onClick = { viewModel.setTab(MainTab.VAULT) },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Vault") },
                    label = { Text("Vault") },
                    modifier = Modifier.testTag("nav_vault")
                )
                NavigationBarItem(
                    selected = (currentTab == MainTab.SETTINGS),
                    onClick = { viewModel.setTab(MainTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Tools") },
                    label = { Text("Tools") },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tab_transition"
            ) { targetTab ->
                when (targetTab) {
                    MainTab.KEYS -> KeyringScreen(viewModel = viewModel)
                    MainTab.ENCRYPT_DECRYPT -> EncryptDecryptScreen(viewModel = viewModel)
                    MainTab.SIGN_VERIFY -> SignVerifyScreen(viewModel = viewModel)
                    MainTab.VAULT -> VaultScreen(viewModel = viewModel)
                    MainTab.SETTINGS -> SettingsToolsScreen(viewModel = viewModel)
                }
            }

            if (isLoading) {
                LoadingOverlay(message = loadingText)
            }
        }
    }
}
