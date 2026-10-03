package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String, // "FILE", "NOTE", "PASSWORD", "CREDENTIAL"
    val originalFileName: String? = null,
    val mimeType: String? = null,
    val encryptedPayload: String, // PGP Armored ciphertext or Base64 PGP binary
    val fileSizeBytes: Long = 0,
    val encryptedWithKeyId: String, // Key ID or "Symmetric Passphrase"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
