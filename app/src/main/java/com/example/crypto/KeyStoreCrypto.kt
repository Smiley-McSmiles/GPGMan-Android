package com.example.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Hardware-backed Android KeyStore AES-256-GCM encryption for securing
 * GPG private keys and vault secrets stored locally in the database.
 */
object KeyStoreCrypto {
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "gpgman_hardware_master_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH = 12

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEY_STORE
            )
            val spec = KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(MASTER_KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
    }

    /**
     * Encrypts plaintext string using AES-256-GCM.
     * Returns a Base64-encoded string containing [12-byte IV + ciphertext + GCM auth tag].
     */
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypts Base64-encoded ciphertext using AES-256-GCM and the hardware-backed key.
     */
    fun decrypt(base64CipherText: String): String {
        if (base64CipherText.isEmpty()) return ""
        val combined = Base64.decode(base64CipherText, Base64.NO_WRAP)
        if (combined.size < IV_LENGTH) {
            throw IllegalArgumentException("Invalid encrypted payload size")
        }
        val iv = ByteArray(IV_LENGTH)
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH)
        val cipherText = ByteArray(combined.size - IV_LENGTH)
        System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.size)

        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        val plainBytes = cipher.doFinal(cipherText)
        return String(plainBytes, Charsets.UTF_8)
    }

    /**
     * Encrypts raw byte array using AES-256-GCM.
     */
    fun encryptBytes(data: ByteArray): ByteArray {
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(data)
        val combined = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
        return combined
    }

    /**
     * Decrypts raw byte array using AES-256-GCM.
     */
    fun decryptBytes(encryptedBytes: ByteArray): ByteArray {
        if (encryptedBytes.size < IV_LENGTH) {
            throw IllegalArgumentException("Invalid encrypted data size")
        }
        val iv = ByteArray(IV_LENGTH)
        System.arraycopy(encryptedBytes, 0, iv, 0, IV_LENGTH)
        val cipherBytes = ByteArray(encryptedBytes.size - IV_LENGTH)
        System.arraycopy(encryptedBytes, IV_LENGTH, cipherBytes, 0, cipherBytes.size)

        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(cipherBytes)
    }
}
