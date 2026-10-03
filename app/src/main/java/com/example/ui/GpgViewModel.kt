package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.DecryptionResult
import com.example.crypto.PgpEngine
import com.example.crypto.SignatureVerificationResult
import com.example.data.AuditLogEntity
import com.example.data.GpgDatabase
import com.example.data.PgpKeyEntity
import com.example.data.VaultItemEntity
import com.example.repository.GpgRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
    KEYS,
    ENCRYPT_DECRYPT,
    SIGN_VERIFY,
    VAULT,
    SETTINGS
}

data class UiMessage(
    val text: String,
    val isError: Boolean = false
)

class GpgViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GpgRepository

    init {
        val database = GpgDatabase.getInstance(application)
        repository = GpgRepository(database.gpgDao())
    }

    private val prefs = application.getSharedPreferences("gpgman_prefs", Context.MODE_PRIVATE)

    // Current Main Tab
    private val _currentTab = MutableStateFlow(MainTab.KEYS)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    // Keyring flows
    val allKeys: StateFlow<List<PgpKeyEntity>> = repository.allKeys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val secretKeys: StateFlow<List<PgpKeyEntity>> = repository.secretKeys
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultItems: StateFlow<List<VaultItemEntity>> = repository.allVaultItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selection & Dialogs
    private val _selectedKey = MutableStateFlow<PgpKeyEntity?>(null)
    val selectedKey: StateFlow<PgpKeyEntity?> = _selectedKey.asStateFlow()

    // Operation states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingText = MutableStateFlow("Processing...")
    val loadingText: StateFlow<String> = _loadingText.asStateFlow()

    // Results
    private val _lastEncryptedText = MutableStateFlow<String?>(null)
    val lastEncryptedText: StateFlow<String?> = _lastEncryptedText.asStateFlow()

    private val _lastDecryptedResult = MutableStateFlow<DecryptionResult?>(null)
    val lastDecryptedResult: StateFlow<DecryptionResult?> = _lastDecryptedResult.asStateFlow()

    private val _lastSignedMessage = MutableStateFlow<String?>(null)
    val lastSignedMessage: StateFlow<String?> = _lastSignedMessage.asStateFlow()

    private val _lastVerifyResult = MutableStateFlow<SignatureVerificationResult?>(null)
    val lastVerifyResult: StateFlow<SignatureVerificationResult?> = _lastVerifyResult.asStateFlow()

    // Notifications / Snackbars
    private val _messageEvent = MutableSharedFlow<UiMessage>()
    val messageEvent: SharedFlow<UiMessage> = _messageEvent.asSharedFlow()

    // App Lock
    private val _isAppLocked = MutableStateFlow(prefs.getString("master_pin", null)?.isNotEmpty() == true)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    fun setTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun selectKey(key: PgpKeyEntity?) {
        _selectedKey.value = key
    }

    fun postMessage(msg: String, isError: Boolean = false) {
        viewModelScope.launch {
            _messageEvent.emit(UiMessage(msg, isError))
        }
    }

    // --- Key Generation ---
    fun generateKey(
        algorithmType: String = "Ed25519",
        name: String,
        email: String,
        comment: String,
        bits: Int,
        passphrase: String,
        expiryDays: Int?,
        onSuccess: () -> Unit = {}
    ) {
        if (name.isBlank()) {
            postMessage("Name is required to generate a key", isError = true)
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Generating $algorithmType OpenPGP Key Pair..."
            val result = repository.generateKeyPair(
                algorithmType = algorithmType,
                name = name,
                email = email,
                comment = comment,
                keyBits = bits,
                passphrase = passphrase,
                expiryDays = expiryDays
            )
            _isLoading.value = false
            result.onSuccess {
                postMessage("Successfully generated key for ${it.name} (${it.keyIdHex})")
                onSuccess()
            }.onFailure {
                postMessage("Key generation failed: ${it.message}", isError = true)
            }
        }
    }

    // --- Key Import ---
    fun importKey(armoredText: String, onSuccess: () -> Unit = {}) {
        if (armoredText.isBlank()) {
            postMessage("Please paste or load key content", isError = true)
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Parsing and importing OpenPGP keys..."
            val result = repository.importKeyBlock(armoredText)
            _isLoading.value = false
            result.onSuccess { keys ->
                postMessage("Imported ${keys.size} key(s) successfully!")
                onSuccess()
            }.onFailure {
                postMessage("Import failed: ${it.message}", isError = true)
            }
        }
    }

    // --- Key Delete ---
    fun deleteKey(key: PgpKeyEntity) {
        viewModelScope.launch {
            repository.deleteKey(key)
            if (_selectedKey.value?.fingerprint == key.fingerprint) {
                _selectedKey.value = null
            }
            postMessage("Key ${key.keyIdHex} deleted.")
        }
    }

    // --- Set Default Key ---
    fun setDefaultKey(fingerprint: String) {
        viewModelScope.launch {
            repository.setDefaultKey(fingerprint)
            postMessage("Default signing key updated.")
        }
    }

    // --- Update Trust Level ---
    fun updateTrustLevel(fingerprint: String, trustLevel: String) {
        viewModelScope.launch {
            repository.updateTrustLevel(fingerprint, trustLevel)
            postMessage("Trust level updated to $trustLevel.")
        }
    }

    suspend fun getExportablePrivateKey(key: PgpKeyEntity): String? {
        return repository.getExportablePrivateKey(key)
    }

    // --- Text Encryption ---
    fun encryptText(
        plainText: String,
        recipientFingerprints: List<String>,
        symmetricPassphrase: String
    ) {
        if (plainText.isBlank()) {
            postMessage("Message text cannot be empty", isError = true)
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Encrypting message..."
            val result = repository.encryptText(plainText, recipientFingerprints, symmetricPassphrase)
            _isLoading.value = false
            result.onSuccess { armored ->
                _lastEncryptedText.value = armored
                postMessage("Text encrypted successfully!")
            }.onFailure {
                postMessage("Encryption failed: ${it.message}", isError = true)
            }
        }
    }

    fun clearEncryptedText() {
        _lastEncryptedText.value = null
    }

    // --- Text Decryption ---
    fun decryptText(armoredCiphertext: String, passphrase: String) {
        if (armoredCiphertext.isBlank()) {
            postMessage("Encrypted text cannot be empty", isError = true)
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Decrypting message..."
            val result = repository.decryptText(armoredCiphertext, passphrase)
            _isLoading.value = false
            _lastDecryptedResult.value = result
            if (result.isSuccess) {
                postMessage("Message successfully decrypted!")
            } else {
                postMessage(result.errorMessage ?: "Decryption failed", isError = true)
            }
        }
    }

    fun clearDecryptedResult() {
        _lastDecryptedResult.value = null
    }

    // --- File Encryption ---
    fun encryptFile(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        recipientFingerprints: List<String>,
        symmetricPassphrase: String,
        saveToVault: Boolean,
        vaultTitle: String,
        onSuccess: (ByteArray) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Encrypting file '$fileName'..."
            val result = repository.encryptFile(
                fileBytes = fileBytes,
                fileName = fileName,
                mimeType = mimeType,
                recipientFingerprints = recipientFingerprints,
                symmetricPassphrase = symmetricPassphrase,
                saveToVault = saveToVault,
                vaultTitle = vaultTitle
            )
            _isLoading.value = false
            result.onSuccess { bytes ->
                postMessage("File encrypted (${bytes.size} bytes)${if (saveToVault) " & saved to Vault" else ""}!")
                onSuccess(bytes)
            }.onFailure {
                postMessage("File encryption failed: ${it.message}", isError = true)
            }
        }
    }

    // --- Vault Item Decryption ---
    fun decryptVaultItem(
        item: VaultItemEntity,
        passphrase: String,
        onResult: (DecryptionResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Decrypting vault item '${item.title}'..."
            val result = repository.decryptVaultItem(item, passphrase)
            _isLoading.value = false
            _lastDecryptedResult.value = result
            if (result.isSuccess) {
                postMessage("Vault item '${item.title}' decrypted!")
            } else {
                postMessage(result.errorMessage ?: "Failed to decrypt vault item", isError = true)
            }
            onResult(result)
        }
    }

    // --- Save Vault Secure Note ---
    fun saveVaultNote(
        title: String,
        category: String,
        content: String,
        recipientFingerprints: List<String>,
        symmetricPassphrase: String,
        onSuccess: () -> Unit = {}
    ) {
        if (title.isBlank() || content.isBlank()) {
            postMessage("Title and content cannot be blank", isError = true)
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Encrypting and storing in Vault..."
            val result = repository.saveVaultSecureNote(
                title = title,
                category = category,
                plainNote = content,
                recipientFingerprints = recipientFingerprints,
                symmetricPassphrase = symmetricPassphrase
            )
            _isLoading.value = false
            result.onSuccess {
                postMessage("Saved '$title' to secure Vault!")
                onSuccess()
            }.onFailure {
                postMessage("Failed to save note: ${it.message}", isError = true)
            }
        }
    }

    // --- Delete Vault Item ---
    fun deleteVaultItem(item: VaultItemEntity) {
        viewModelScope.launch {
            repository.deleteVaultItem(item)
            postMessage("Deleted '${item.title}' from Vault.")
        }
    }

    // --- Digital Signatures ---
    fun signText(text: String, secretKeyFingerprint: String, passphrase: String) {
        if (text.isBlank()) {
            postMessage("Message text cannot be empty", isError = true)
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Creating PGP signature..."
            val result = repository.signText(text, secretKeyFingerprint, passphrase)
            _isLoading.value = false
            result.onSuccess { signed ->
                _lastSignedMessage.value = signed
                postMessage("Message signed successfully!")
            }.onFailure {
                postMessage("Signing failed: ${it.message}", isError = true)
            }
        }
    }

    fun clearSignedMessage() {
        _lastSignedMessage.value = null
    }

    fun verifySignature(signedText: String) {
        if (signedText.isBlank()) {
            postMessage("Signed message cannot be empty", isError = true)
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Verifying cryptographic signature..."
            val result = repository.verifyCleartextSignature(signedText)
            _isLoading.value = false
            _lastVerifyResult.value = result
            if (result.isValid) {
                postMessage("Signature is VALID and authentic!")
            } else {
                postMessage(result.message, isError = !result.isKeyFoundInKeyring)
            }
        }
    }

    fun clearVerifyResult() {
        _lastVerifyResult.value = null
    }

    // --- Remote Services: GitHub & Keyserver ---
    fun fetchGitHubKeys(username: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Fetching GPG keys from GitHub for @$username..."
            val result = repository.fetchGitHubPublicKeys(username)
            _isLoading.value = false
            result.onSuccess { keys ->
                postMessage("Fetched ${keys.size} public key(s) from GitHub for @$username!")
                onSuccess()
            }.onFailure {
                postMessage("GitHub fetch failed: ${it.message}", isError = true)
            }
        }
    }

    fun fetchKeyserverKey(query: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Searching keys.openpgp.org for '$query'..."
            val result = repository.fetchKeyserverKey(query)
            _isLoading.value = false
            result.onSuccess { keys ->
                postMessage("Found and imported ${keys.size} key(s) from keyserver!")
                onSuccess()
            }.onFailure {
                postMessage("Keyserver query failed: ${it.message}", isError = true)
            }
        }
    }

    // --- Backup Private Keys ---
    suspend fun getSecretKeysBackupBundle(): String {
        return repository.getAllExportablePrivateKeys()
    }

    // --- App Lock (PIN / PASSWORD / PATTERN) ---
    fun getLockType(): String {
        return prefs.getString("lock_type", "PIN") ?: "PIN"
    }

    fun setLockType(type: String) {
        prefs.edit().putString("lock_type", type).apply()
    }

    fun unlockApp(secret: String): Boolean {
        val savedSecret = prefs.getString("master_secret", null)
            ?: prefs.getString("master_pin", "")
        if (savedSecret.isNullOrEmpty() || secret == savedSecret) {
            _isAppLocked.value = false
            return true
        }
        val type = getLockType()
        val label = when (type) {
            "PASSWORD" -> "Incorrect Password"
            "PATTERN" -> "Incorrect Pattern"
            else -> "Incorrect PIN"
        }
        postMessage(label, isError = true)
        return false
    }

    fun setMasterLock(secret: String, type: String = "PIN") {
        if (secret.isEmpty()) {
            prefs.edit()
                .remove("master_secret")
                .remove("master_pin")
                .remove("lock_type")
                .apply()
            _isAppLocked.value = false
            postMessage("App lock removed.")
        } else {
            prefs.edit()
                .putString("master_secret", secret)
                .putString("master_pin", secret)
                .putString("lock_type", type)
                .apply()
            postMessage("App lock updated ($type).")
        }
    }

    fun setMasterPin(newPin: String) {
        setMasterLock(newPin, "PIN")
    }

    fun hasMasterPin(): Boolean {
        return (prefs.getString("master_secret", null)?.isNotEmpty() == true) ||
                (prefs.getString("master_pin", null)?.isNotEmpty() == true)
    }

    fun lockAppNow() {
        if (hasMasterPin()) {
            _isAppLocked.value = true
        }
    }

    // --- Audit Logs ---
    fun clearAuditLogs() {
        viewModelScope.launch {
            repository.clearAuditLogs()
            postMessage("Security audit logs cleared.")
        }
    }
}
