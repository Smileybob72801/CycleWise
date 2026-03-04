package com.veleda.cyclewise.settings

import com.veleda.cyclewise.domain.usecases.SeedManifest
import com.veleda.cyclewise.domain.usecases.TutorialCleanupUseCase
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [runSeedCleanupIfNeeded].
 *
 * Uses Robolectric because [parseSeedManifest] depends on `org.json.JSONObject`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = com.veleda.cyclewise.RobolectricTestApp::class)
class RunSeedCleanupIfNeededTest {

    private val manifest = SeedManifest(
        periodUuids = listOf("uuid-1"),
        dailyEntryIds = listOf("entry-1"),
        waterIntakeDates = listOf("2026-01-10"),
    )

    @Test
    fun `WHEN manifestJson is empty THEN cleanup is not called`() = runTest {
        // GIVEN
        val appSettings = mockk<AppSettings>()
        val cleanupUseCase = mockk<TutorialCleanupUseCase>()
        every { appSettings.seedManifestJson } returns flowOf("")

        // WHEN
        runSeedCleanupIfNeeded(appSettings, cleanupUseCase)

        // THEN
        coVerify(exactly = 0) { cleanupUseCase(any()) }
        coVerify(exactly = 0) { appSettings.clearSeedManifest() }
    }

    @Test
    fun `WHEN manifestJson is valid THEN cleanup is called and manifest is cleared`() = runTest {
        // GIVEN
        val appSettings = mockk<AppSettings>(relaxUnitFun = true)
        val cleanupUseCase = mockk<TutorialCleanupUseCase>(relaxUnitFun = true)
        val json = manifest.toJson()
        every { appSettings.seedManifestJson } returns flowOf(json)

        // WHEN
        runSeedCleanupIfNeeded(appSettings, cleanupUseCase)

        // THEN
        coVerify(exactly = 1) { cleanupUseCase(manifest) }
        coVerify(exactly = 1) { appSettings.clearSeedManifest() }
    }

    @Test
    fun `WHEN manifestJson is malformed THEN cleanup is not called but manifest is still cleared`() = runTest {
        // GIVEN — malformed JSON that parseSeedManifest returns null for
        val appSettings = mockk<AppSettings>(relaxUnitFun = true)
        val cleanupUseCase = mockk<TutorialCleanupUseCase>()
        every { appSettings.seedManifestJson } returns flowOf("{not valid json")

        // WHEN
        runSeedCleanupIfNeeded(appSettings, cleanupUseCase)

        // THEN — cleanup use case is skipped, but manifest is always cleared
        // so the bottom nav re-enables even if the JSON was corrupt.
        coVerify(exactly = 0) { cleanupUseCase(any()) }
        coVerify(exactly = 1) { appSettings.clearSeedManifest() }
    }
}
