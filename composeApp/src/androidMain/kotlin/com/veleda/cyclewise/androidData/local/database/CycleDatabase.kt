package com.veleda.cyclewise.androidData.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.veleda.cyclewise.androidData.local.dao.CycleDao
import com.veleda.cyclewise.androidData.local.entities.CycleEntity
import com.veleda.cyclewise.androidData.local.entities.Converters
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [CycleEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class CycleDatabase : RoomDatabase() {
    abstract fun cycleDao(): CycleDao

    companion object {
        /**
         * Create the database, encrypted with SQLCipher using the given passphrase.
         */
        fun create(context: Context, passphrase: ByteArray): CycleDatabase {
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(
                context,
                CycleDatabase::class.java,
                "cyclewise.db"
            )
                .openHelperFactory(factory)
                .build()
        }
    }
}