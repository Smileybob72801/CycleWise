package com.veleda.cyclewise.services

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.veleda.cyclewise.RobolectricTestApp
import com.veleda.cyclewise.androidData.local.database.PeriodDatabase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(application = RobolectricTestApp::class)
class BackupManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var backupManager: BackupManager
    private lateinit var mockSaltStorage: SaltStorage

    private val validMetadataJson = """
        {
            "appVersionName": "1.0.0",
            "appVersionCode": 2,
            "schemaVersion": 13,
            "exportDateUtc": "2026-03-19T14:30:22Z"
        }
    """.trimIndent()

    @Before
    fun setUp() {
        mockSaltStorage = mockk<SaltStorage>()
        val context = ApplicationProvider.getApplicationContext<RobolectricTestApp>()
        backupManager = BackupManager(context, mockSaltStorage)
    }

    // --- validateBackup tests ---

    @Test
    fun validateBackup_WHEN_validZipWithAllEntries_THEN_returnsBackupMetadata() = runTest {
        // ARRANGE
        val zipFile = createZipFile(
            BackupManager.ENTRY_METADATA to validMetadataJson.toByteArray(),
            BackupManager.ENTRY_DATABASE to "fake-db-content".toByteArray(),
            BackupManager.ENTRY_SALT to "c2FsdF9BXzE2X2J5dGVz".toByteArray(),
        )
        val uri = Uri.fromFile(zipFile)

        // ACT
        val metadata = backupManager.validateBackup(uri)

        // ASSERT
        assertEquals("1.0.0", metadata.appVersionName)
        assertEquals(2, metadata.appVersionCode)
        assertEquals(13, metadata.schemaVersion)
        assertEquals("2026-03-19T14:30:22Z", metadata.exportDateUtc)
    }

    @Test(expected = BackupException.InvalidArchive::class)
    fun validateBackup_WHEN_missingMetadataJson_THEN_throwsInvalidArchive() = runTest {
        // ARRANGE — ZIP without metadata.json
        val zipFile = createZipFile(
            BackupManager.ENTRY_DATABASE to "fake-db-content".toByteArray(),
            BackupManager.ENTRY_SALT to "c2FsdF9BXzE2X2J5dGVz".toByteArray(),
        )
        val uri = Uri.fromFile(zipFile)

        // ACT
        backupManager.validateBackup(uri)
    }

    @Test(expected = BackupException.InvalidArchive::class)
    fun validateBackup_WHEN_missingDatabase_THEN_throwsInvalidArchive() = runTest {
        // ARRANGE — ZIP without cyclewise.db
        val zipFile = createZipFile(
            BackupManager.ENTRY_METADATA to validMetadataJson.toByteArray(),
            BackupManager.ENTRY_SALT to "c2FsdF9BXzE2X2J5dGVz".toByteArray(),
        )
        val uri = Uri.fromFile(zipFile)

        // ACT
        backupManager.validateBackup(uri)
    }

    @Test(expected = BackupException.InvalidArchive::class)
    fun validateBackup_WHEN_missingSalt_THEN_throwsInvalidArchive() = runTest {
        // ARRANGE — ZIP without salt.txt
        val zipFile = createZipFile(
            BackupManager.ENTRY_METADATA to validMetadataJson.toByteArray(),
            BackupManager.ENTRY_DATABASE to "fake-db-content".toByteArray(),
        )
        val uri = Uri.fromFile(zipFile)

        // ACT
        backupManager.validateBackup(uri)
    }

    @Test(expected = BackupException.SchemaVersionTooNew::class)
    fun validateBackup_WHEN_schemaVersionTooNew_THEN_throwsSchemaVersionTooNew() = runTest {
        // ARRANGE — schema version higher than current
        val futureSchemaVersion = PeriodDatabase.SCHEMA_VERSION + 1
        val futureMetadataJson = """
            {
                "appVersionName": "2.0.0",
                "appVersionCode": 10,
                "schemaVersion": $futureSchemaVersion,
                "exportDateUtc": "2026-03-19T14:30:22Z"
            }
        """.trimIndent()

        val zipFile = createZipFile(
            BackupManager.ENTRY_METADATA to futureMetadataJson.toByteArray(),
            BackupManager.ENTRY_DATABASE to "fake-db-content".toByteArray(),
            BackupManager.ENTRY_SALT to "c2FsdF9BXzE2X2J5dGVz".toByteArray(),
        )
        val uri = Uri.fromFile(zipFile)

        // ACT
        backupManager.validateBackup(uri)
    }

    @Test
    fun validateBackup_WHEN_schemaVersionMatchesCurrent_THEN_succeeds() = runTest {
        // ARRANGE — schema version exactly matches current
        val metadataJson = """
            {
                "appVersionName": "1.0.0",
                "appVersionCode": 2,
                "schemaVersion": ${PeriodDatabase.SCHEMA_VERSION},
                "exportDateUtc": "2026-03-19T14:30:22Z"
            }
        """.trimIndent()

        val zipFile = createZipFile(
            BackupManager.ENTRY_METADATA to metadataJson.toByteArray(),
            BackupManager.ENTRY_DATABASE to "fake-db-content".toByteArray(),
            BackupManager.ENTRY_SALT to "c2FsdF9BXzE2X2J5dGVz".toByteArray(),
        )
        val uri = Uri.fromFile(zipFile)

        // ACT
        val metadata = backupManager.validateBackup(uri)

        // ASSERT
        assertEquals(PeriodDatabase.SCHEMA_VERSION, metadata.schemaVersion)
    }

    @Test
    fun validateBackup_WHEN_olderSchemaVersion_THEN_succeeds() = runTest {
        // ARRANGE — schema version lower than current (forward-compatible)
        val olderSchemaVersion = PeriodDatabase.SCHEMA_VERSION - 1
        val metadataJson = """
            {
                "appVersionName": "0.9.0",
                "appVersionCode": 1,
                "schemaVersion": $olderSchemaVersion,
                "exportDateUtc": "2026-03-19T14:30:22Z"
            }
        """.trimIndent()

        val zipFile = createZipFile(
            BackupManager.ENTRY_METADATA to metadataJson.toByteArray(),
            BackupManager.ENTRY_DATABASE to "fake-db-content".toByteArray(),
            BackupManager.ENTRY_SALT to "c2FsdF9BXzE2X2J5dGVz".toByteArray(),
        )
        val uri = Uri.fromFile(zipFile)

        // ACT
        val metadata = backupManager.validateBackup(uri)

        // ASSERT
        assertEquals(olderSchemaVersion, metadata.schemaVersion)
    }

    @Test(expected = BackupException.InvalidArchive::class)
    fun validateBackup_WHEN_emptyZip_THEN_throwsInvalidArchive() = runTest {
        // ARRANGE — ZIP with no entries at all
        val zipFile = createZipFile()
        val uri = Uri.fromFile(zipFile)

        // ACT
        backupManager.validateBackup(uri)
    }

    // --- suggestedFilename tests ---

    @Test
    fun suggestedFilename_THEN_matchesExpectedFormat() {
        // ACT
        val filename = BackupManager.suggestedFilename()

        // ASSERT — format: RhythmWise_yyyy-MM-dd_HHmmss.rwbackup
        assertTrue(
            "Filename '$filename' should match RhythmWise_yyyy-MM-dd_HHmmss.rwbackup format",
            filename.matches(Regex("""RhythmWise_\d{4}-\d{2}-\d{2}_\d{6}\.rwbackup""")),
        )
    }

    @Test
    fun suggestedFilename_THEN_startsWithRhythmWise() {
        // ACT
        val filename = BackupManager.suggestedFilename()

        // ASSERT
        assertTrue(
            "Filename should start with 'RhythmWise_'",
            filename.startsWith("RhythmWise_"),
        )
    }

    @Test
    fun suggestedFilename_THEN_endsWithRwbackupExtension() {
        // ACT
        val filename = BackupManager.suggestedFilename()

        // ASSERT
        assertTrue(
            "Filename should end with '.rwbackup'",
            filename.endsWith(".rwbackup"),
        )
    }

    // --- deriveKeyWithSalt tests ---

    @Test
    fun deriveKeyWithSalt_THEN_returns32ByteKey() {
        // ARRANGE
        val passphrase = "test_passphrase"
        val salt = "salt_A_16_bytes!".toByteArray() // 16 bytes

        // ACT
        val key = backupManager.deriveKeyWithSalt(passphrase, salt)

        // ASSERT
        assertEquals(
            "Derived key should be exactly 32 bytes (256 bits)",
            32,
            key.size,
        )
    }

    @Test
    fun deriveKeyWithSalt_WHEN_sameInputs_THEN_producesSameKey() {
        // ARRANGE
        val passphrase = "deterministic_passphrase"
        val salt = "salt_A_16_bytes!".toByteArray()

        // ACT
        val key1 = backupManager.deriveKeyWithSalt(passphrase, salt)
        val key2 = backupManager.deriveKeyWithSalt(passphrase, salt)

        // ASSERT
        assertTrue(
            "Same passphrase and salt should produce identical keys",
            key1.contentEquals(key2),
        )
    }

    @Test
    fun deriveKeyWithSalt_WHEN_differentSalt_THEN_producesDifferentKey() {
        // ARRANGE
        val passphrase = "test_passphrase"
        val saltA = "salt_A_16_bytes!".toByteArray()
        val saltB = "salt_B_16_bytes!".toByteArray()

        // ACT
        val key1 = backupManager.deriveKeyWithSalt(passphrase, saltA)
        val key2 = backupManager.deriveKeyWithSalt(passphrase, saltB)

        // ASSERT
        assertTrue(
            "Different salts should produce different keys",
            !key1.contentEquals(key2),
        )
    }

    @Test
    fun deriveKeyWithSalt_WHEN_differentPassphrase_THEN_producesDifferentKey() {
        // ARRANGE
        val salt = "salt_A_16_bytes!".toByteArray()

        // ACT
        val key1 = backupManager.deriveKeyWithSalt("passphrase_one", salt)
        val key2 = backupManager.deriveKeyWithSalt("passphrase_two", salt)

        // ASSERT
        assertTrue(
            "Different passphrases should produce different keys",
            !key1.contentEquals(key2),
        )
    }

    // --- Helpers ---

    /**
     * Creates a temporary ZIP file with the given entries.
     *
     * @param entries Pairs of (entry name, entry content bytes).
     * @return A [File] pointing to the created ZIP in the [tempFolder].
     */
    private fun createZipFile(vararg entries: Pair<String, ByteArray>): File {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            for ((name, content) in entries) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content)
                zip.closeEntry()
            }
        }
        val file = tempFolder.newFile("test_backup.rwbackup")
        file.writeBytes(baos.toByteArray())
        return file
    }
}
