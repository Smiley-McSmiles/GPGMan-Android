package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PgpKeyEntity::class, VaultItemEntity::class, AuditLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class GpgDatabase : RoomDatabase() {
    abstract fun gpgDao(): GpgDao

    companion object {
        @Volatile
        private var INSTANCE: GpgDatabase? = null

        fun getInstance(context: Context): GpgDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GpgDatabase::class.java,
                    "gpgman_database.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
