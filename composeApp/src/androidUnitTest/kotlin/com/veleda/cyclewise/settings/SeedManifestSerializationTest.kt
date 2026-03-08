package com.veleda.cyclewise.settings

import com.veleda.cyclewise.domain.usecases.SeedManifest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Unit tests for [SeedManifest] JSON serialisation helpers ([toJson] / [parseSeedManifest]).
 *
 * Uses Robolectric because [toJson]/[parseSeedManifest] depend on `org.json.JSONObject`
 * which is part of the Android framework.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = com.veleda.cyclewise.RobolectricTestApp::class)
class SeedManifestSerializationTest {

    @Test
    fun `toJson then parseSeedManifest round-trips correctly`() {
        // GIVEN
        val original = SeedManifest(
            periodUuids = listOf("uuid-1", "uuid-2", "uuid-3"),
            dailyEntryIds = listOf("entry-a", "entry-b", "entry-c"),
            waterIntakeDates = listOf("2026-01-10", "2026-01-11", "2026-01-12"),
        )

        // WHEN
        val json = original.toJson()
        val parsed = parseSeedManifest(json)

        // THEN
        assertNotNull(parsed)
        assertEquals(original, parsed)
    }

    @Test
    fun `parseSeedManifest WHEN emptyString THEN returnsNull`() {
        // GIVEN / WHEN
        val result = parseSeedManifest("")

        // THEN
        assertNull(result)
    }

    @Test
    fun `parseSeedManifest WHEN blankString THEN returnsNull`() {
        // GIVEN / WHEN
        val result = parseSeedManifest("   ")

        // THEN
        assertNull(result)
    }

    @Test
    fun `parseSeedManifest WHEN invalidJson THEN returnsNull`() {
        // GIVEN / WHEN
        val result = parseSeedManifest("{not valid json")

        // THEN
        assertNull(result)
    }

    @Test
    fun `parseSeedManifest WHEN emptyArrays THEN returnsEmptyManifest`() {
        // GIVEN
        val json = """{"periodUuids":[],"dailyEntryIds":[],"waterIntakeDates":[]}"""

        // WHEN
        val result = parseSeedManifest(json)

        // THEN
        assertNotNull(result)
        assertEquals(emptyList(), result.periodUuids)
        assertEquals(emptyList(), result.dailyEntryIds)
        assertEquals(emptyList(), result.waterIntakeDates)
    }
}
