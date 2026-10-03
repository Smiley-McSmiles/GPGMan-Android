package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GpgDao {
    // --- Keys ---
    @Query("SELECT * FROM pgp_keys ORDER BY isDefault DESC, creationDate DESC")
    fun getAllKeys(): Flow<List<PgpKeyEntity>>

    @Query("SELECT * FROM pgp_keys WHERE isSecretKey = 1 ORDER BY isDefault DESC, creationDate DESC")
    fun getSecretKeys(): Flow<List<PgpKeyEntity>>

    @Query("SELECT * FROM pgp_keys WHERE isSecretKey = 0 ORDER BY creationDate DESC")
    fun getPublicKeysOnly(): Flow<List<PgpKeyEntity>>

    @Query("SELECT * FROM pgp_keys WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun getKeyByFingerprint(fingerprint: String): PgpKeyEntity?

    @Query("SELECT * FROM pgp_keys WHERE keyIdHex LIKE '%' || :keyIdHex || '%' LIMIT 1")
    suspend fun findKeyByKeyId(keyIdHex: String): PgpKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: PgpKeyEntity)

    @Update
    suspend fun updateKey(key: PgpKeyEntity)

    @Delete
    suspend fun deleteKey(key: PgpKeyEntity)

    @Query("UPDATE pgp_keys SET isDefault = 0")
    suspend fun clearDefaultKey()

    @Query("UPDATE pgp_keys SET isDefault = 1 WHERE fingerprint = :fingerprint")
    suspend fun setDefaultKey(fingerprint: String)

    // --- Vault Items ---
    @Query("SELECT * FROM vault_items ORDER BY updatedAt DESC")
    fun getAllVaultItems(): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE category = :category ORDER BY updatedAt DESC")
    fun getVaultItemsByCategory(category: String): Flow<List<VaultItemEntity>>

    @Query("SELECT * FROM vault_items WHERE id = :id LIMIT 1")
    suspend fun getVaultItemById(id: Long): VaultItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: VaultItemEntity): Long

    @Update
    suspend fun updateVaultItem(item: VaultItemEntity)

    @Delete
    suspend fun deleteVaultItem(item: VaultItemEntity)

    // --- Audit Logs ---
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Query("DELETE FROM audit_logs")
    suspend fun clearAuditLogs()
}
