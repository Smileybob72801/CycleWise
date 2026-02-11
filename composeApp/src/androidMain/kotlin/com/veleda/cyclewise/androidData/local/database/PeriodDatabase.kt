package com.veleda.cyclewise.androidData.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.veleda.cyclewise.androidData.local.dao.PeriodDao
import com.veleda.cyclewise.androidData.local.dao.DailyEntryDao
import com.veleda.cyclewise.androidData.local.dao.MedicationDao
import com.veleda.cyclewise.androidData.local.dao.MedicationLogDao
import com.veleda.cyclewise.androidData.local.dao.PeriodLogDao
import com.veleda.cyclewise.androidData.local.dao.SymptomDao
import com.veleda.cyclewise.androidData.local.dao.SymptomLogDao
import com.veleda.cyclewise.androidData.local.dao.WaterIntakeDao
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_1_2
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_2_3
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_3_4
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_4_5
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_5_6
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_6_7
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_7_8
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_8_9
import com.veleda.cyclewise.androidData.local.entities.PeriodEntity
import com.veleda.cyclewise.androidData.local.entities.Converters
import com.veleda.cyclewise.androidData.local.entities.DailyEntryEntity
import com.veleda.cyclewise.androidData.local.entities.MedicationEntity
import com.veleda.cyclewise.androidData.local.entities.MedicationLogEntity
import com.veleda.cyclewise.androidData.local.entities.SymptomEntity
import com.veleda.cyclewise.androidData.local.entities.SymptomLogEntity
import com.veleda.cyclewise.androidData.local.entities.PeriodLogEntity
import com.veleda.cyclewise.androidData.local.entities.WaterIntakeEntity
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        PeriodEntity::class,
        DailyEntryEntity::class,
        SymptomEntity::class,
        MedicationEntity::class,
        MedicationLogEntity::class,
        SymptomLogEntity::class,
        PeriodLogEntity::class,
        WaterIntakeEntity::class
    ],
    version = 9,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PeriodDatabase : RoomDatabase() {
    abstract fun periodDao(): PeriodDao
    abstract fun dailyEntryDao(): DailyEntryDao
    abstract fun symptomDao(): SymptomDao
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationLogDao(): MedicationLogDao
    abstract fun symptomLogDao(): SymptomLogDao
    abstract fun periodLogDao(): PeriodLogDao
    abstract fun waterIntakeDao(): WaterIntakeDao

    companion object {
        fun create(
            context: Context,
            passphrase: ByteArray,
            dbName: String = "cyclewise.db"
        ): PeriodDatabase {
            val factory = SupportFactory(passphrase)
            return Room.databaseBuilder(
                context,
                PeriodDatabase::class.java,
                dbName
            )
                .openHelperFactory(factory)
                .addMigrations(
                    Migration_1_2,
                    Migration_2_3,
                    Migration_3_4,
                    Migration_4_5,
                    Migration_5_6,
                    Migration_6_7,
                    Migration_7_8,
                    Migration_8_9
                )
                .build()
        }
    }
}