package com.veleda.cyclewise.domain.models

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class BackupMetadataTest {

    private val json = Json { encodeDefaults = true }

    // region Serialization roundtrip

    @Test
    fun serializationRoundtrip_GIVEN_backupMetadata_THEN_decodedEqualsOriginal() {
        // GIVEN — a fully populated BackupMetadata
        val original = BackupMetadata(
            appVersionName = "1.2.0",
            appVersionCode = 12,
            schemaVersion = 5,
            exportDateUtc = "2026-03-19T14:30:22Z",
        )

        // WHEN — encoded to JSON and decoded back
        val encoded = json.encodeToString(BackupMetadata.serializer(), original)
        val decoded = json.decodeFromString(BackupMetadata.serializer(), encoded)

        // THEN — the decoded instance equals the original
        assertEquals(original, decoded)
    }

    // endregion

    // region Deserialization from known JSON

    @Test
    fun deserialization_GIVEN_knownJsonString_THEN_producesExpectedValues() {
        // GIVEN — a known JSON string
        val knownJson = """
            {
                "appVersionName": "2.0.1",
                "appVersionCode": 21,
                "schemaVersion": 8,
                "exportDateUtc": "2026-03-22T09:15:00Z"
            }
        """.trimIndent()

        // WHEN — deserialized
        val metadata = json.decodeFromString(BackupMetadata.serializer(), knownJson)

        // THEN — each field matches the expected value
        assertEquals("2.0.1", metadata.appVersionName)
        assertEquals(21, metadata.appVersionCode)
        assertEquals(8, metadata.schemaVersion)
        assertEquals("2026-03-22T09:15:00Z", metadata.exportDateUtc)
    }

    // endregion
}
