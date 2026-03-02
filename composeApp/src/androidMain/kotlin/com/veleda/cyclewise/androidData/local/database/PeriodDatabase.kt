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
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_9_10
import com.veleda.cyclewise.androidData.local.database.migrations.Migration_10_11
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

/**
 * SQLCipher-encrypted Room database for all user health data.
 *
 * Opened via [create] with a passphrase-derived key. The caller (session scope) is
 * responsible for zeroizing the passphrase [ByteArray] after the database is opened.
 *
 * **Security:** All data at rest is AES-256-GCM encrypted via SQLCipher.
 * The database file (`cyclewise.db`) is unreadable without the correct passphrase.
 *
 * **Schema version:** 11. All migrations are registered in [create] and tested individually.
 */
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
    version = 11,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class PeriodDatabase : RoomDatabase() {
    /** Returns the [PeriodDao] for CRUD operations on the `periods` table. */
    abstract fun periodDao(): PeriodDao

    /** Returns the [DailyEntryDao] for CRUD operations on the `daily_entries` table. */
    abstract fun dailyEntryDao(): DailyEntryDao

    /** Returns the [SymptomDao] for CRUD operations on the `symptom_library` table. */
    abstract fun symptomDao(): SymptomDao

    /** Returns the [MedicationDao] for CRUD operations on the `medication_library` table. */
    abstract fun medicationDao(): MedicationDao

    /** Returns the [MedicationLogDao] for CRUD operations on the `medication_logs` table. */
    abstract fun medicationLogDao(): MedicationLogDao

    /** Returns the [SymptomLogDao] for CRUD operations on the `symptom_logs` table. */
    abstract fun symptomLogDao(): SymptomLogDao

    /** Returns the [PeriodLogDao] for CRUD operations on the `period_logs` table. */
    abstract fun periodLogDao(): PeriodLogDao

    /** Returns the [WaterIntakeDao] for CRUD operations on the `water_intake` table. */
    abstract fun waterIntakeDao(): WaterIntakeDao

    /**
     * Re-encrypts the database with a new passphrase-derived key using SQLCipher's `PRAGMA rekey`.
     *
     * The database must already be open (via [openHelper.writableDatabase]) before calling this
     * method. The rekey operation replaces the encryption key in-place without closing or
     * re-creating the database file.
     *
     * **Security:** The [newKey] array is zeroized in a `finally` block after the PRAGMA
     * executes, regardless of success or failure. The caller should also zeroize any copies
     * of the key it holds.
     *
     * @param newKey 32-byte AES key derived from the new passphrase.
     *               **Consumed and zeroized by this method** — do not reuse after calling.
     * @throws android.database.SQLException if the rekey operation fails.
     */
    fun changeEncryptionKey(newKey: ByteArray) {
        val hex = newKey.joinToString("") { "%02x".format(it) }
        try {
            openHelper.writableDatabase.execSQL("PRAGMA rekey = \"x'$hex'\"")
        } finally {
            newKey.fill(0)
        }
    }

    companion object {
        /**
         * Creates (or opens) the encrypted database backed by SQLCipher.
         *
         * ## Security: SupportFactory reference semantics
         *
         * [SupportFactory] stores the [passphrase] array **by reference**, not by copy.
         * Meanwhile, `Room.databaseBuilder().build()` returns **without** opening the
         * underlying SQLCipher file — the actual file open is deferred until the first
         * DAO query or an explicit call to [openHelper.writableDatabase].
         *
         * This creates a dangerous ordering constraint: if the caller zeros the
         * [passphrase] array between `build()` and the first real database open,
         * [SupportFactory] will read an all-zeros key and SQLCipher will silently
         * encrypt with the wrong key (or open an empty database), making **every**
         * passphrase appear valid.
         *
         * To avoid this, callers must do **one** of the following:
         * 1. Pass `passphrase.copyOf()` so the factory has its own independent array
         *    (the approach used by [createDatabaseAndZeroizeKey]).
         * 2. Call `db.openHelper.writableDatabase` **before** zeroing the original
         *    array, which forces SQLCipher to consume the key immediately.
         *
         * Note: [SupportFactory]'s default `clearPassphrase = true` constructor
         * parameter causes it to zero **its own** array after
         * `getWritableDatabase()` succeeds, so the copy is cleaned up automatically.
         *
         * @param context    application context for the Room builder.
         * @param passphrase 32-byte AES key derived from the user's passphrase.
         *                   **Caller must zeroize this array after this method returns.**
         *                   See the security section above for ordering constraints.
         * @param dbName     database file name (default: "cyclewise.db").
         * @return the [PeriodDatabase] instance. **Not yet opened** — the caller must
         *         call `db.openHelper.writableDatabase` to force SQLCipher to consume
         *         the key before any DAO access.
         */
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
                    Migration_8_9,
                    Migration_9_10,
                    Migration_10_11
                )
                .build()
        }
    }
}