package com.veleda.cyclewise.androidData.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.veleda.cyclewise.androidData.local.dao.CycleDao
import com.veleda.cyclewise.androidData.local.dao.DailyEntryDao
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_1_2
import com.veleda.cyclewise.androidData.local.entities.CycleEntity
import com.veleda.cyclewise.androidData.local.entities.Converters
import com.veleda.cyclewise.androidData.local.entities.DailyEntryEntity
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [CycleEntity::class, DailyEntryEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class CycleDatabase : RoomDatabase() {
    abstract fun cycleDao(): CycleDao
    abstract fun dailyEntryDao(): DailyEntryDao

    companion object {
        fun create(context: Context, passphrase: ByteArray): CycleDatabase {
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(
                context,
                CycleDatabase::class.java,
                "cyclewise.db"
            )
                .openHelperFactory(factory)
                .addMigrations(Migration_1_2)
                .build()
        }
    }
}