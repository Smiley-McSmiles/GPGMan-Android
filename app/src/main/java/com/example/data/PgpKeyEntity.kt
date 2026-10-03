package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pgp_keys")
data class PgpKeyEntity(
    @PrimaryKey
    val fingerprint: String, // 40-character hex fingerprint
    val keyIdHex: String, // e.g. "0x78AB34CD"
    val userId: String, // Full user ID, e.g. "Alice <alice@example.com>"
    val name: String,
    val email: String,
    val comment: String,
    val isSecretKey: Boolean,
    val hasPassphrase: Boolean,
    val algorithm: String,
    val keyBits: Int,
    val creationDate: Long,
    val expiryDate: Long?,
    val isDefault: Boolean = false,
    val armoredPublicKey: String,
    val encryptedArmoredPrivateKey: String? = null,
    val trustLevel: String = "ULTIMATE" // ULTIMATE, FULL, MARGINAL, UNKNOWN
)
