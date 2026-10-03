package com.example.repository

import com.example.crypto.DecryptionResult
import com.example.crypto.KeyStoreCrypto
import com.example.crypto.ParsedKeyInfo
import com.example.crypto.PgpEngine
import com.example.crypto.SignatureVerificationResult
import com.example.data.AuditLogEntity
import com.example.data.GpgDao
import com.example.data.PgpKeyEntity
import com.example.data.VaultItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPSecretKeyRing
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class GpgRepository(private val dao: GpgDao) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Flows
    val allKeys: Flow<List<PgpKeyEntity>> = dao.getAllKeys()
    val secretKeys: Flow<List<PgpKeyEntity>> = dao.getSecretKeys()
    val publicKeys: Flow<List<PgpKeyEntity>> = dao.getPublicKeysOnly()
    val allVaultItems: Flow<List<VaultItemEntity>> = dao.getAllVaultItems()
    val auditLogs: Flow<List<AuditLogEntity>> = dao.getAuditLogs()

    suspend fun getVaultItemsByCategory(category: String): Flow<List<VaultItemEntity>> {
        return dao.getVaultItemsByCategory(category)
    }

    // --- Key Management ---

    suspend fun generateKeyPair(
        algorithmType: String = "RSA",
        name: String,
        email: String,
        comment: String,
        keyBits: Int,
        passphrase: String,
        expiryDays: Int?
    ): Result<PgpKeyEntity> = withContext(Dispatchers.IO) {
        try {
            val (armoredPub, armoredPriv) = PgpEngine.generateKeyPair(
                algorithmType = algorithmType,
                name = name,
                email = email,
                comment = comment,
                keyBits = keyBits,
                passphrase = passphrase,
                expiryDays = expiryDays
            )

            val parsedList = PgpEngine.parseKeyBlock(armoredPriv)
            val parsed = parsedList.firstOrNull { it.isSecretKey }
                ?: parsedList.firstOrNull()
                ?: throw IllegalStateException("Could not parse generated keypair")

            // Encrypt private key with Android KeyStore master key
            val encryptedPriv = KeyStoreCrypto.encrypt(armoredPriv)

            val isFirstKey = (dao.getAllKeys().firstOrNull()?.isEmpty() == true)

            val entity = PgpKeyEntity(
                fingerprint = parsed.fingerprint,
                keyIdHex = parsed.keyIdHex,
                userId = parsed.userId,
                name = parsed.name,
                email = parsed.email,
                comment = parsed.comment,
                isSecretKey = true,
                hasPassphrase = passphrase.isNotEmpty(),
                algorithm = parsed.algorithm,
                keyBits = parsed.bitStrength,
                creationDate = parsed.creationDate,
                expiryDate = parsed.expiryDate,
                isDefault = isFirstKey,
                armoredPublicKey = armoredPub,
                encryptedArmoredPrivateKey = encryptedPriv,
                trustLevel = "ULTIMATE"
            )

            dao.insertKey(entity)
            dao.insertAuditLog(
                AuditLogEntity(
                    action = "KEY_GENERATE",
                    details = "Generated ${parsed.algorithm} ${parsed.bitStrength}-bit keypair for ${parsed.userId} (${parsed.keyIdHex})",
                    status = "SUCCESS"
                )
            )

            Result.success(entity)
        } catch (e: Exception) {
            dao.insertAuditLog(
                AuditLogEntity(
                    action = "KEY_GENERATE",
                    details = "Failed to generate key: ${e.message}",
                    status = "FAILED"
                )
            )
            Result.failure(e)
        }
    }

    suspend fun importKeyBlock(armoredText: String): Result<List<PgpKeyEntity>> = withContext(Dispatchers.IO) {
        try {
            val parsedList = PgpEngine.parseKeyBlock(armoredText)
            if (parsedList.isEmpty()) {
                throw IllegalArgumentException("No valid OpenPGP keys found in the provided block.")
            }

            val savedKeys = mutableListOf<PgpKeyEntity>()
            for (p in parsedList) {
                // If private key block is present, encrypt with KeyStore
                val encryptedPriv = if (p.armoredPrivateKey != null) {
                    KeyStoreCrypto.encrypt(p.armoredPrivateKey)
                } else null

                val entity = PgpKeyEntity(
                    fingerprint = p.fingerprint,
                    keyIdHex = p.keyIdHex,
                    userId = p.userId,
                    name = p.name,
                    email = p.email,
                    comment = p.comment,
                    isSecretKey = p.isSecretKey,
                    hasPassphrase = p.hasPassphrase,
                    algorithm = p.algorithm,
                    keyBits = p.bitStrength,
                    creationDate = p.creationDate,
                    expiryDate = p.expiryDate,
                    isDefault = false,
                    armoredPublicKey = p.armoredPublicKey,
                    encryptedArmoredPrivateKey = encryptedPriv,
                    trustLevel = if (p.isSecretKey) "ULTIMATE" else "FULL"
                )
                dao.insertKey(entity)
                savedKeys.add(entity)

                dao.insertAuditLog(
                    AuditLogEntity(
                        action = "KEY_IMPORT",
                        details = "Imported ${if (p.isSecretKey) "SECRET" else "PUBLIC"} key ${p.keyIdHex} (${p.userId})",
                        status = "SUCCESS"
                    )
                )
            }

            Result.success(savedKeys)
        } catch (e: Exception) {
            dao.insertAuditLog(
                AuditLogEntity(
                    action = "KEY_IMPORT",
                    details = "Key import error: ${e.message}",
                    status = "FAILED"
                )
            )
            Result.failure(e)
        }
    }

    suspend fun deleteKey(key: PgpKeyEntity) = withContext(Dispatchers.IO) {
        dao.deleteKey(key)
        dao.insertAuditLog(
            AuditLogEntity(
                action = "KEY_DELETE",
                details = "Deleted key ${key.keyIdHex} (${key.userId})",
                status = "SUCCESS"
            )
        )
    }

    suspend fun setDefaultKey(fingerprint: String) = withContext(Dispatchers.IO) {
        dao.clearDefaultKey()
        dao.setDefaultKey(fingerprint)
    }

    suspend fun updateTrustLevel(fingerprint: String, trustLevel: String) = withContext(Dispatchers.IO) {
        val existing = dao.getKeyByFingerprint(fingerprint)
        if (existing != null) {
            dao.updateKey(existing.copy(trustLevel = trustLevel))
        }
    }

    suspend fun getExportablePrivateKey(key: PgpKeyEntity): String? = withContext(Dispatchers.IO) {
        val encrypted = key.encryptedArmoredPrivateKey ?: return@withContext null
        try {
            KeyStoreCrypto.decrypt(encrypted)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getAllExportablePrivateKeys(): String = withContext(Dispatchers.IO) {
        val allKeys = dao.getAllKeys().firstOrNull() ?: emptyList()
        val secretKeys = allKeys.filter { it.isSecretKey && it.encryptedArmoredPrivateKey != null }
        val sb = StringBuilder()
        for (k in secretKeys) {
            val decrypted = getExportablePrivateKey(k)
            if (decrypted != null) {
                if (sb.isNotEmpty()) sb.append("\n\n")
                sb.append(decrypted)
            }
        }
        dao.insertAuditLog(
            AuditLogEntity(
                action = "BACKUP_SECRET_KEYS",
                details = "Exported armored private key bundle (${secretKeys.size} key(s))",
                status = "SUCCESS"
            )
        )
        sb.toString()
    }

    // --- Encryption / Decryption ---

    suspend fun encryptText(
        plainText: String,
        recipientFingerprints: List<String>,
        symmetricPassphrase: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val publicKeys = mutableListOf<PGPPublicKey>()
            for (fp in recipientFingerprints) {
                val entity = dao.getKeyByFingerprint(fp)
                if (entity != null) {
                    val pk = PgpEngine.readPublicKey(entity.armoredPublicKey)
                    if (pk != null) publicKeys.add(pk)
                }
            }

            if (publicKeys.isEmpty() && symmetricPassphrase.isEmpty()) {
                throw IllegalArgumentException("Please select at least one recipient key or provide a symmetric passphrase.")
            }

            val encryptedBytes = PgpEngine.encryptData(
                plainBytes = plainText.toByteArray(StandardCharsets.UTF_8),
                filename = "message.txt",
                recipientPublicKeys = publicKeys,
                symmetricPassphrase = symmetricPassphrase,
                armorOutput = true
            )

            val armoredResult = String(encryptedBytes, StandardCharsets.UTF_8)
            dao.insertAuditLog(
                AuditLogEntity(
                    action = "ENCRYPT_TEXT",
                    details = "Encrypted text (${plainText.length} chars) with ${publicKeys.size} recipients ${if (symmetricPassphrase.isNotEmpty()) "+ Password" else ""}",
                    status = "SUCCESS"
                )
            )

            Result.success(armoredResult)
        } catch (e: Exception) {
            dao.insertAuditLog(
                AuditLogEntity(
                    action = "ENCRYPT_TEXT",
                    details = "Encryption failed: ${e.message}",
                    status = "FAILED"
                )
            )
            Result.failure(e)
        }
    }

    suspend fun decryptText(
        encryptedArmoredText: String,
        passphrase: String
    ): DecryptionResult = withContext(Dispatchers.IO) {
        try {
            // Load all secret keyrings from database
            val secretEntities = dao.getSecretKeys().firstOrNull() ?: emptyList()
            val secretKeyRings = mutableListOf<PGPSecretKeyRing>()

            for (entity in secretEntities) {
                val encPriv = entity.encryptedArmoredPrivateKey ?: continue
                try {
                    val rawArmoredPriv = KeyStoreCrypto.decrypt(encPriv)
                    val ring = PgpEngine.readSecretKeyRing(rawArmoredPriv)
                    if (ring != null) secretKeyRings.add(ring)
                } catch (_: Exception) {}
            }

            // Load all public keys for signature check
            val allKeyEntities = dao.getAllKeys().firstOrNull() ?: emptyList()
            val allPublicKeys = mutableListOf<PGPPublicKey>()
            for (entity in allKeyEntities) {
                allPublicKeys.addAll(PgpEngine.readAllPublicKeys(entity.armoredPublicKey))
            }

            val result = PgpEngine.decryptData(
                encryptedBytes = encryptedArmoredText.toByteArray(StandardCharsets.UTF_8),
                secretKeyRings = secretKeyRings,
                passphrase = passphrase,
                keyringPublicKeys = allPublicKeys
            )

            dao.insertAuditLog(
                AuditLogEntity(
                    action = "DECRYPT_TEXT",
                    details = if (result.isSuccess) "Decrypted message (${result.decryptedText.length} chars) via ${result.decryptedByKeyId}" else "Decryption failed: ${result.errorMessage}",
                    status = if (result.isSuccess) "SUCCESS" else "FAILED"
                )
            )

            result
        } catch (e: Exception) {
            dao.insertAuditLog(
                AuditLogEntity(
                    action = "DECRYPT_TEXT",
                    details = "Decryption error: ${e.message}",
                    status = "FAILED"
                )
            )
            DecryptionResult(
                isSuccess = false,
                errorMessage = e.message ?: "Decryption failed"
            )
        }
    }

    suspend fun encryptFile(
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String,
        recipientFingerprints: List<String>,
        symmetricPassphrase: String,
        saveToVault: Boolean,
        vaultTitle: String
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val publicKeys = mutableListOf<PGPPublicKey>()
            for (fp in recipientFingerprints) {
                val entity = dao.getKeyByFingerprint(fp)
                if (entity != null) {
                    val pk = PgpEngine.readPublicKey(entity.armoredPublicKey)
                    if (pk != null) publicKeys.add(pk)
                }
            }

            if (publicKeys.isEmpty() && symmetricPassphrase.isEmpty()) {
                throw IllegalArgumentException("Please select at least one recipient key or provide a symmetric passphrase.")
            }

            val encryptedBytes = PgpEngine.encryptData(
                plainBytes = fileBytes,
                filename = fileName,
                recipientPublicKeys = publicKeys,
                symmetricPassphrase = symmetricPassphrase,
                armorOutput = true
            )

            val armoredText = String(encryptedBytes, StandardCharsets.UTF_8)

            if (saveToVault) {
                val title = vaultTitle.ifEmpty { fileName }
                val keyInfo = if (publicKeys.isNotEmpty()) {
                    publicKeys.joinToString { String.format("0x%08X", it.keyID) }
                } else {
                    "Symmetric Passphrase"
                }

                val vaultItem = VaultItemEntity(
                    title = title,
                    category = "FILE",
                    originalFileName = fileName,
                    mimeType = mimeType,
                    encryptedPayload = armoredText,
                    fileSizeBytes = fileBytes.size.toLong(),
                    encryptedWithKeyId = keyInfo
                )
                dao.insertVaultItem(vaultItem)
            }

            dao.insertAuditLog(
                AuditLogEntity(
                    action = "ENCRYPT_FILE",
                    details = "Encrypted file '$fileName' (${fileBytes.size} bytes) ${if (saveToVault) "and saved to Vault" else ""}",
                    status = "SUCCESS"
                )
            )

            Result.success(encryptedBytes)
        } catch (e: Exception) {
            dao.insertAuditLog(
                AuditLogEntity(
                    action = "ENCRYPT_FILE",
                    details = "File encryption failed: ${e.message}",
                    status = "FAILED"
                )
            )
            Result.failure(e)
        }
    }

    suspend fun decryptVaultItem(
        item: VaultItemEntity,
        passphrase: String
    ): DecryptionResult = withContext(Dispatchers.IO) {
        val result = decryptText(item.encryptedPayload, passphrase)
        if (result.isSuccess) {
            dao.insertAuditLog(
                AuditLogEntity(
                    action = "DECRYPT_VAULT",
                    details = "Decrypted vault item '${item.title}'",
                    status = "SUCCESS"
                )
            )
        }
        result
    }

    // --- Cleartext Signature & Verification ---

    suspend fun signText(
        text: String,
        secretKeyFingerprint: String,
        passphrase: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val keyEntity = dao.getKeyByFingerprint(secretKeyFingerprint)
                ?: throw IllegalArgumentException("Secret key not found for fingerprint: $secretKeyFingerprint")

            val encPriv = keyEntity.encryptedArmoredPrivateKey
                ?: throw IllegalStateException("Key has no private key data available")

            val rawPriv = KeyStoreCrypto.decrypt(encPriv)
            val ring = PgpEngine.readSecretKeyRing(rawPriv)
                ?: throw IllegalStateException("Failed to parse private key ring")

            val signedMessage = PgpEngine.createCleartextSignature(text, ring, passphrase)

            dao.insertAuditLog(
                AuditLogEntity(
                    action = "SIGN_DATA",
                    details = "Created cleartext PGP signature using ${keyEntity.keyIdHex} (${keyEntity.name})",
                    status = "SUCCESS"
                )
            )

            Result.success(signedMessage)
        } catch (e: Exception) {
            dao.insertAuditLog(
                AuditLogEntity(
                    action = "SIGN_DATA",
                    details = "Signing failed: ${e.message}",
                    status = "FAILED"
                )
            )
            Result.failure(e)
        }
    }

    suspend fun verifyCleartextSignature(cleartextSignedMessage: String): SignatureVerificationResult = withContext(Dispatchers.IO) {
        val allKeyEntities = dao.getAllKeys().firstOrNull() ?: emptyList()
        val allPublicKeys = mutableListOf<PGPPublicKey>()
        for (entity in allKeyEntities) {
            allPublicKeys.addAll(PgpEngine.readAllPublicKeys(entity.armoredPublicKey))
        }

        val result = PgpEngine.verifyCleartextSignature(cleartextSignedMessage, allPublicKeys)

        dao.insertAuditLog(
            AuditLogEntity(
                action = "VERIFY_SIGNATURE",
                details = if (result.isValid) "Verified signature from ${result.signerUserId} (${result.keyIdHex})" else "Signature check failed: ${result.message}",
                status = if (result.isValid) "SUCCESS" else if (!result.isKeyFoundInKeyring) "WARNING" else "FAILED"
            )
        )

        result
    }

    // --- Vault Secure Notes & Passwords ---

    suspend fun saveVaultSecureNote(
        title: String,
        category: String,
        plainNote: String,
        recipientFingerprints: List<String>,
        symmetricPassphrase: String
    ): Result<VaultItemEntity> = withContext(Dispatchers.IO) {
        try {
            val encResult = encryptText(plainNote, recipientFingerprints, symmetricPassphrase)
            val armoredCiphertext = encResult.getOrThrow()

            val keyInfo = if (recipientFingerprints.isNotEmpty()) {
                recipientFingerprints.map { fp ->
                    dao.getKeyByFingerprint(fp)?.keyIdHex ?: fp.take(8)
                }.joinToString()
            } else {
                "Symmetric Passphrase"
            }

            val item = VaultItemEntity(
                title = title,
                category = category,
                originalFileName = null,
                mimeType = "text/plain",
                encryptedPayload = armoredCiphertext,
                fileSizeBytes = plainNote.toByteArray(StandardCharsets.UTF_8).size.toLong(),
                encryptedWithKeyId = keyInfo
            )

            val id = dao.insertVaultItem(item)
            val saved = item.copy(id = id)

            dao.insertAuditLog(
                AuditLogEntity(
                    action = "VAULT_CREATE",
                    details = "Created encrypted $category '$title' in Vault",
                    status = "SUCCESS"
                )
            )

            Result.success(saved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteVaultItem(item: VaultItemEntity) = withContext(Dispatchers.IO) {
        dao.deleteVaultItem(item)
        dao.insertAuditLog(
            AuditLogEntity(
                action = "VAULT_DELETE",
                details = "Deleted vault item '${item.title}'",
                status = "SUCCESS"
            )
        )
    }

    // --- Remote Key Services (GitHub & Keyserver) ---

    suspend fun fetchGitHubPublicKeys(username: String): Result<List<PgpKeyEntity>> = withContext(Dispatchers.IO) {
        val cleanUser = username.trim().removePrefix("@")
        if (cleanUser.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Username cannot be empty"))
        }

        val url = "https://github.com/$cleanUser.gpg"
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "gpgman-Android")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("GitHub returned HTTP ${response.code}. No GPG keys found for user '$cleanUser'.")
                )
            }

            val body = response.body?.string()
            if (body.isNullOrBlank()) {
                return@withContext Result.failure(IOException("No GPG keys found for GitHub user '$cleanUser'."))
            }

            importKeyBlock(body)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchKeyserverKey(query: String): Result<List<PgpKeyEntity>> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Query cannot be empty"))
        }

        // Try keys.openpgp.org VKS API
        val url = if (cleanQuery.contains("@")) {
            "https://keys.openpgp.org/vks/v1/by-email/${cleanQuery}"
        } else if (cleanQuery.startsWith("0x") || cleanQuery.length in listOf(8, 16)) {
            val cleanKeyId = cleanQuery.removePrefix("0x")
            "https://keys.openpgp.org/vks/v1/by-keyid/$cleanKeyId"
        } else {
            val cleanFp = cleanQuery.replace(" ", "")
            "https://keys.openpgp.org/vks/v1/by-fingerprint/$cleanFp"
        }

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "gpgman-Android")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IOException("Keyserver returned HTTP ${response.code}. Key not found for '$cleanQuery'.")
                )
            }

            val body = response.body?.string()
            if (body.isNullOrBlank()) {
                return@withContext Result.failure(IOException("Keyserver returned empty response."))
            }

            importKeyBlock(body)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAuditLogs() = withContext(Dispatchers.IO) {
        dao.clearAuditLogs()
    }
}
