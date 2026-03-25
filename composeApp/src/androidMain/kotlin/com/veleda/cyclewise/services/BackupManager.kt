package com.veleda.cyclewise.services

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.sqlite.db.SupportSQLiteDatabase
import com.veleda.cyclewise.BuildConfig
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import com.veleda.cyclewise.domain.models.BackupMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Manages export and import of `.rwbackup` archives for device migration.
 *
 * A `.rwbackup` file is a standard ZIP archive containing:
 * - `cyclewise.db` — the SQLCipher-encrypted Room database file.
 * - `salt.txt` — the Base64-encoded Argon2id salt that was used to derive the encryption key.
 * - `metadata.json` — a [BackupMetadata] JSON object with app version, schema version,
 *   and export timestamp.
 *
 * **Singleton-scoped** because import must work before a session is unlocked (from the
 * unlock screen or first-time setup screen). Export requires an active session; the caller
 * passes the open [SupportSQLiteDatabase] for WAL checkpoint.
 *
 * @param context           Application context for resolving file paths and content URIs.
 * @param saltStorage       Provides the current salt for export, and receives the imported
 *                          salt during import via [SaltStorage.setSalt].
 */
class BackupManager(
    private val context: Context,
    private val saltStorage: SaltStorage,
) {

    private val json = Json { prettyPrint = true }

    /**
     * Exports the encrypted database, salt, and metadata to a `.rwbackup` ZIP archive
     * at the user-selected [outputUri].
     *
     * **Requires an active session.** The caller must pass the open [writableDb] so that
     * the WAL can be checkpointed before copying the database file.
     *
     * @param outputUri  SAF-provided URI for the output file.
     * @param writableDb The open [SupportSQLiteDatabase] from the active session, used
     *                   to execute `PRAGMA wal_checkpoint(TRUNCATE)` before export.
     * @throws BackupException.IoError on any I/O failure.
     */
    suspend fun exportBackup(outputUri: Uri, writableDb: SupportSQLiteDatabase) {
        withContext(Dispatchers.IO) {
            // Flush WAL into main database file for a consistent snapshot
            writableDb.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }

            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) {
                throw BackupException.IoError("Database file not found")
            }

            val saltBase64 = saltStorage.getSaltBase64()
                ?: throw BackupException.IoError("No salt found — database has never been unlocked")

            val metadata = BackupMetadata(
                appVersionName = BuildConfig.VERSION_NAME,
                appVersionCode = BuildConfig.VERSION_CODE,
                schemaVersion = PeriodDatabase.SCHEMA_VERSION,
                exportDateUtc = Instant.now().atOffset(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            )

            val metadataJson = json.encodeToString(metadata)

            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zip ->
                    // metadata.json
                    zip.putNextEntry(ZipEntry(ENTRY_METADATA))
                    zip.write(metadataJson.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()

                    // cyclewise.db
                    zip.putNextEntry(ZipEntry(ENTRY_DATABASE))
                    FileInputStream(dbFile).use { it.copyTo(zip) }
                    zip.closeEntry()

                    // salt.txt
                    zip.putNextEntry(ZipEntry(ENTRY_SALT))
                    zip.write(saltBase64.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
            } ?: throw BackupException.IoError("Could not open output stream for backup URI")
        }
    }

    /**
     * Validates a `.rwbackup` archive and returns its [BackupMetadata].
     *
     * Checks that the ZIP contains all three required entries (`metadata.json`,
     * `cyclewise.db`, `salt.txt`), parses the metadata, and verifies the schema
     * version is not newer than the current app.
     *
     * Does **not** require an active session.
     *
     * @param inputUri SAF-provided URI for the `.rwbackup` file.
     * @return the parsed [BackupMetadata].
     * @throws BackupException.InvalidArchive if the ZIP structure is invalid or entries are missing.
     * @throws BackupException.SchemaVersionTooNew if the backup's schema version exceeds the app's.
     */
    suspend fun validateBackup(inputUri: Uri): BackupMetadata = withContext(Dispatchers.IO) {
        val foundEntries = mutableSetOf<String>()
        var metadataJsonString: String? = null

        try {
            context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        foundEntries.add(entry.name)
                        if (entry.name == ENTRY_METADATA) {
                            metadataJsonString = zip.readBytes().toString(Charsets.UTF_8)
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: throw BackupException.InvalidArchive()
        } catch (e: BackupException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            throw BackupException.InvalidArchive()
        }

        val requiredEntries = setOf(ENTRY_METADATA, ENTRY_DATABASE, ENTRY_SALT)
        val missing = requiredEntries - foundEntries
        if (missing.isNotEmpty()) {
            throw BackupException.InvalidArchive()
        }

        val metadata = try {
            json.decodeFromString<BackupMetadata>(
                metadataJsonString ?: throw BackupException.InvalidArchive(),
            )
        } catch (e: BackupException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            throw BackupException.InvalidArchive()
        }

        if (metadata.schemaVersion > PeriodDatabase.SCHEMA_VERSION) {
            throw BackupException.SchemaVersionTooNew(
                backup = metadata.schemaVersion,
                current = PeriodDatabase.SCHEMA_VERSION,
            )
        }

        metadata
    }

    /**
     * Verifies that [passphrase] can decrypt the database inside the backup archive.
     *
     * Extracts the salt and database to temporary files, derives a key using Argon2id
     * with the **imported** salt (not the device's current salt), and attempts to open
     * the database with raw SQLCipher. If the open succeeds, the passphrase is correct.
     *
     * Does **not** require an active session.
     *
     * @param inputUri   SAF-provided URI for the `.rwbackup` file.
     * @param passphrase The passphrase to verify against the backup.
     * @return `true` if the passphrase is correct.
     * @throws BackupException.WrongPassphrase if the passphrase does not match.
     * @throws BackupException.InvalidArchive if required entries are missing.
     */
    suspend fun verifyPassphrase(inputUri: Uri, passphrase: String): Boolean =
        withContext(Dispatchers.IO) {
            var tempDbFile: File? = null
            var key: ByteArray? = null

            try {
                val (saltBytes, dbBytes) = extractSaltAndDb(inputUri)

                // Derive key using the imported salt (not the device's current salt)
                key = deriveKeyWithSalt(passphrase, saltBytes)

                // Write DB to temp file for SQLCipher verification
                tempDbFile = File(context.cacheDir, "verify_backup.db")
                FileOutputStream(tempDbFile).use { it.write(dbBytes) }

                // Attempt to open with raw SQLCipher
                net.sqlcipher.database.SQLiteDatabase.loadLibs(context)
                val rawDb = net.sqlcipher.database.SQLiteDatabase.openDatabase(
                    tempDbFile.absolutePath,
                    key.copyOf(),
                    null,
                    net.sqlcipher.database.SQLiteDatabase.OPEN_READONLY,
                    null,
                    null,
                )
                try {
                    // Verify we can actually read data
                    rawDb.rawQuery("SELECT count(*) FROM sqlite_master", null)
                        .use { it.moveToFirst() }
                } finally {
                    rawDb.close()
                }

                true
            } catch (e: BackupException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                throw BackupException.WrongPassphrase()
            } finally {
                key?.fill(0)
                tempDbFile?.delete()
                // Also clean up any -wal/-shm temp files SQLCipher may create
                tempDbFile?.let {
                    File(it.absolutePath + "-wal").delete()
                    File(it.absolutePath + "-shm").delete()
                }
            }
        }

    /**
     * Imports the database and salt from a `.rwbackup` archive, replacing the current data.
     *
     * Before replacing, creates rollback copies of the current database and salt. If any
     * failure occurs after replacement begins, the rollback copies are restored.
     *
     * Does **not** require an active session — the caller must close any active session
     * before calling this method.
     *
     * @param inputUri SAF-provided URI for the validated `.rwbackup` file.
     * @throws BackupException.RollbackRestored if import fails but the previous data was
     *         successfully restored from the rollback backup.
     * @throws BackupException.IoError on unrecoverable I/O failure.
     */
    suspend fun importBackup(inputUri: Uri) {
        withContext(Dispatchers.IO) {
            val (saltBytes, dbBytes) = extractSaltAndDb(inputUri)

            val dbFile = context.getDatabasePath(DB_NAME)
            val walFile = File(dbFile.absolutePath + "-wal")
            val shmFile = File(dbFile.absolutePath + "-shm")
            val rollbackDbFile = File(dbFile.absolutePath + ".rollback")
            val rollbackSaltBase64 = saltStorage.getSaltBase64()
            var rollbackCreated = false

            try {
                // Create rollback backup if existing data is present
                if (dbFile.exists()) {
                    dbFile.copyTo(rollbackDbFile, overwrite = true)
                    rollbackCreated = true
                }

                // Ensure parent directory exists
                dbFile.parentFile?.mkdirs()

                // Replace database file
                FileOutputStream(dbFile).use { it.write(dbBytes) }

                // Delete stale WAL and SHM files to prevent journal conflicts
                walFile.delete()
                shmFile.delete()

                // Replace salt
                saltStorage.setSalt(saltBytes)

                // Success — clean up rollback files
                rollbackDbFile.delete()
            } catch (e: BackupException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                // Attempt to restore from rollback
                if (rollbackCreated) {
                    try {
                        rollbackDbFile.copyTo(dbFile, overwrite = true)
                        if (rollbackSaltBase64 != null) {
                            val rollbackSalt = Base64.decode(rollbackSaltBase64, Base64.DEFAULT)
                            saltStorage.setSalt(rollbackSalt)
                        }
                        rollbackDbFile.delete()
                    } catch (@Suppress("TooGenericExceptionCaught") rollbackError: Exception) {
                        throw BackupException.IoError(
                            "Import failed and rollback also failed: ${rollbackError.message}",
                        )
                    }
                    throw BackupException.RollbackRestored()
                }
                throw BackupException.IoError("Import failed: ${e.message}")
            }
        }
    }

    /**
     * Extracts the salt bytes and database bytes from a `.rwbackup` ZIP archive.
     *
     * @return A [Pair] of (saltBytes, dbBytes).
     * @throws BackupException.InvalidArchive if required entries are missing.
     */
    private fun extractSaltAndDb(inputUri: Uri): Pair<ByteArray, ByteArray> {
        var saltBase64String: String? = null
        var dbBytes: ByteArray? = null

        context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        ENTRY_SALT -> saltBase64String = zip.readBytes().toString(Charsets.UTF_8)
                        ENTRY_DATABASE -> dbBytes = zip.readBytes()
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: throw BackupException.InvalidArchive()

        val saltBytes = Base64.decode(
            saltBase64String ?: throw BackupException.InvalidArchive(),
            Base64.DEFAULT,
        )
        return Pair(saltBytes, dbBytes ?: throw BackupException.InvalidArchive())
    }

    /**
     * Derives a 32-byte AES key from [passphrase] using Argon2id with the provided [salt].
     *
     * Uses the same parameters as [PassphraseServiceAndroid] (parallelism=1, memory=64MB,
     * iterations=3) but with an explicit salt rather than the device's stored salt. This
     * allows passphrase verification against an imported backup without modifying the
     * device's [SaltStorage].
     *
     * **Coupling note:** The Argon2 parameters here must stay in sync with
     * [PassphraseServiceAndroid.deriveKey]. If those parameters change, update both.
     *
     * @param passphrase the raw user-entered secret.
     * @param salt       the 16-byte salt extracted from the backup archive.
     * @return a fresh 32-byte array containing the derived key. Caller must zeroize.
     */
    internal fun deriveKeyWithSalt(passphrase: String, salt: ByteArray): ByteArray {
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withSalt(salt)
            .withParallelism(1)
            .withMemoryAsKB(ARGON2_MEMORY_KB)
            .withIterations(ARGON2_ITERATIONS)
            .build()

        val generator = Argon2BytesGenerator().apply { init(params) }
        val key = ByteArray(KEY_LENGTH)
        generator.generateBytes(passphrase.toCharArray(), key, 0, KEY_LENGTH)
        return key
    }

    companion object {
        internal const val DB_NAME = "cyclewise.db"
        internal const val ENTRY_METADATA = "metadata.json"
        internal const val ENTRY_DATABASE = "cyclewise.db"
        internal const val ENTRY_SALT = "salt.txt"
        private const val KEY_LENGTH = 32
        private const val ARGON2_MEMORY_KB = 64 * 1024
        private const val ARGON2_ITERATIONS = 3

        /**
         * Generates the suggested filename for a backup export.
         *
         * Format: `RhythmWise_<yyyy-MM-dd>_<HHmmss>.rwbackup`
         */
        fun suggestedFilename(): String {
            val now = Instant.now().atOffset(ZoneOffset.UTC)
            val date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val time = now.format(DateTimeFormatter.ofPattern("HHmmss"))
            return "RhythmWise_${date}_$time.rwbackup"
        }
    }
}

/**
 * Structured exception types for backup operations.
 *
 * Each subclass represents a distinct failure mode with a user-facing message.
 * The sealed hierarchy ensures exhaustive handling via `when` expressions.
 */
sealed class BackupException(message: String) : Exception(message) {
    /** The ZIP structure is invalid, missing required entries, or `metadata.json` is unparseable. */
    class InvalidArchive : BackupException("Invalid or corrupted backup file")

    /**
     * The backup's schema version is greater than the current app's schema version.
     *
     * @property backup  the schema version in the backup archive.
     * @property current the current app's schema version.
     */
    class SchemaVersionTooNew(
        val backup: Int,
        val current: Int,
    ) : BackupException(
        "Backup schema version ($backup) is newer than current ($current). Update the app.",
    )

    /** The passphrase does not match the encryption key used to create the backup. */
    class WrongPassphrase : BackupException("Incorrect passphrase")

    /** Import failed after replacement began, but previous data was restored from rollback. */
    class RollbackRestored : BackupException(
        "Import failed. Your previous data has been restored.",
    )

    /** Generic I/O failure during backup operations. */
    class IoError(msg: String) : BackupException(msg)
}
