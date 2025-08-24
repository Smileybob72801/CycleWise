package com.veleda.cyclewise.androidData.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.veleda.cyclewise.androidData.local.dao.CycleDao
import com.veleda.cyclewise.androidData.local.dao.DailyEntryDao
import com.veleda.cyclewise.androidData.local.dao.MedicationDao
import com.veleda.cyclewise.androidData.local.dao.MedicationLogDao
import com.veleda.cyclewise.androidData.local.dao.SymptomDao
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_1_2
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_2_3
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_3_4
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_4_5
import com.veleda.cyclewise.androidData.local.entities.CycleEntity
import com.veleda.cyclewise.androidData.local.entities.Converters
import com.veleda.cyclewise.androidData.local.entities.DailyEntryEntity
import com.veleda.cyclewise.androidData.local.entities.MedicationEntity
import com.veleda.cyclewise.androidData.local.entities.MedicationLogEntity
import com.veleda.cyclewise.androidData.local.entities.SymptomEntity
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        CycleEntity::class,
        DailyEntryEntity::class,
        SymptomEntity::class,
        MedicationEntity::class,
        MedicationLogEntity::class,
    ],
    version = 5,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class CycleDatabase : RoomDatabase() {
    abstract fun cycleDao(): CycleDao
    abstract fun dailyEntryDao(): DailyEntryDao
    abstract fun symptomDao(): SymptomDao
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationLogDao(): MedicationLogDao

    companion object {
        fun create(context: Context, passphrase: ByteArray): CycleDatabase {
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(
                context,
                CycleDatabase::class.java,
                "cyclewise.db"
            )
                .openHelperFactory(factory)
                .addMigrations(
                    Migration_1_2,
                    Migration_2_3,
                    Migration_3_4,
                    Migration_4_5
                )
                .build()
        }
    }
}