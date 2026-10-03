package com.example.crypto

data class ParsedKeyInfo(
    val fingerprint: String,
    val keyIdHex: String,
    val userId: String,
    val name: String,
    val email: String,
    val comment: String,
    val isSecretKey: Boolean,
    val hasPassphrase: Boolean,
    val algorithm: String,
    val bitStrength: Int,
    val creationDate: Long,
    val expiryDate: Long?,
    val armoredPublicKey: String,
    val armoredPrivateKey: String? = null
)

data class SignatureVerificationResult(
    val isValid: Boolean,
    val keyIdHex: String,
    val signerUserId: String?,
    val signerFingerprint: String?,
    val signatureDate: Long?,
    val isKeyFoundInKeyring: Boolean,
    val message: String
)

data class DecryptionResult(
    val isSuccess: Boolean,
    val decryptedText: String = "",
    val decryptedBytes: ByteArray? = null,
    val originalFileName: String = "",
    val isSymmetricallyEncrypted: Boolean = false,
    val decryptedByKeyId: String = "",
    val signatureResult: SignatureVerificationResult? = null,
    val errorMessage: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DecryptionResult
        if (isSuccess != other.isSuccess) return false
        if (decryptedText != other.decryptedText) return false
        if (decryptedBytes != null) {
            if (other.decryptedBytes == null) return false
            if (!decryptedBytes.contentEquals(other.decryptedBytes)) return false
        } else if (other.decryptedBytes != null) return false
        return true
    }

    override fun hashCode(): Int {
        var result = isSuccess.hashCode()
        result = 31 * result + decryptedText.hashCode()
        result = 31 * result + (decryptedBytes?.contentHashCode() ?: 0)
        return result
    }
}
