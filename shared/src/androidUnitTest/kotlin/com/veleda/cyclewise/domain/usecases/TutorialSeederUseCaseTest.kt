package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.models.FlowIntensity
import com.veleda.cyclewise.domain.models.FullDailyLog
import com.veleda.cyclewise.domain.models.Period
import com.veleda.cyclewise.domain.models.SymptomCategory
import com.veleda.cyclewise.domain.models.WaterIntake
import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.testutil.TestData
import com.veleda.cyclewise.testutil.buildMedication
import com.veleda.cyclewise.testutil.buildSymptom
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * A [Clock] that always returns a fixed [Instant], useful for deterministic tests.
 */
@ExperimentalTime
private class FixedClock(private val instant: Instant) : Clock {
    override fun now(): Instant = instant
}

/**
 * Unit tests for [TutorialSeederUseCase].
 *
 * Uses a [FixedClock] with a fixed instant so date calculations are deterministic,
 * and a mock [PeriodRepository] to verify the seeder's behaviour without a real database.
 */
@OptIn(ExperimentalTime::class)
class TutorialSeederUseCaseTest {

    private lateinit var repository: PeriodRepository
    private lateinit var clock: Clock
    private lateinit var useCase: TutorialSeederUseCase

    private val fixedInstant = Instant.fromEpochMilliseconds(1750000000000L)

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        clock = FixedClock(fixedInstant)
        useCase = TutorialSeederUseCase(repository, clock)
    }

    @Test
    fun `invoke WHEN periodsExist THEN doesNotSeed`() = runTest {
        // GIVEN — repository already has periods
        coEvery { repository.getAllPeriods() } returns flowOf(
            listOf(
                com.veleda.cyclewise.testutil.buildPeriod(
                    startDate = LocalDate(2025, 6, 1),
                    endDate = LocalDate(2025, 6, 5),
                )
            )
        )

        // WHEN
        useCase()

        // THEN — no seeding methods called
        coVerify(exactly = 0) { repository.createCompletedPeriod(any(), any()) }
        coVerify(exactly = 0) { repository.saveFullLog(any()) }
    }

    @Test
    fun `invoke WHEN noPeriods THEN creates3Periods`() = runTest {
        // GIVEN — empty database
        coEvery { repository.getAllPeriods() } returns flowOf(emptyList())
        stubLibraryMethods()

        // WHEN
        useCase()

        // THEN — exactly 3 periods created
        coVerify(exactly = 3) { repository.createCompletedPeriod(any(), any()) }
    }

    @Test
    fun `invoke WHEN noPeriods THEN seedsPeriodDayDailyEntries`() = runTest {
        // GIVEN — empty database
        coEvery { repository.getAllPeriods() } returns flowOf(emptyList())
        stubLibraryMethods()

        val savedLogs = mutableListOf<FullDailyLog>()
        coEvery { repository.saveFullLog(capture(savedLogs)) } returns Unit

        // WHEN
        useCase()

        // THEN — 14 period-day entries (5 + 4 + 5) plus gap-day entries
        val periodDayLogs = savedLogs.filter { it.periodLog != null }
        assertEquals(14, periodDayLogs.size, "Should have 14 period-day entries (5+4+5)")
    }

    @Test
    fun `invoke WHEN noPeriods THEN createsSymptomAndMedicationLibraryEntries`() = runTest {
        // GIVEN — empty database
        coEvery { repository.getAllPeriods() } returns flowOf(emptyList())
        stubLibraryMethods()

        // WHEN
        useCase()

        // THEN — 4 symptoms and 1 medication created
        coVerify { repository.createOrGetSymptomInLibrary("Cramps", SymptomCategory.PAIN) }
        coVerify { repository.createOrGetSymptomInLibrary("Bloating", SymptomCategory.DIGESTIVE) }
        coVerify { repository.createOrGetSymptomInLibrary("Headache", SymptomCategory.PAIN) }
        coVerify { repository.createOrGetSymptomInLibrary("Fatigue", SymptomCategory.ENERGY) }
        coVerify { repository.createOrGetMedicationInLibrary("Ibuprofen") }
    }

    @Test
    fun `invoke WHEN noPeriods THEN seedsWaterIntakeForEachEntry`() = runTest {
        // GIVEN — empty database
        coEvery { repository.getAllPeriods() } returns flowOf(emptyList())
        stubLibraryMethods()

        val savedWater = mutableListOf<WaterIntake>()
        coEvery { repository.upsertWaterIntake(capture(savedWater)) } returns Unit

        val savedLogs = mutableListOf<FullDailyLog>()
        coEvery { repository.saveFullLog(capture(savedLogs)) } returns Unit

        // WHEN
        useCase()

        // THEN — every saved log also has a water intake entry
        assertEquals(savedLogs.size, savedWater.size,
            "Water intake count should match saved log count")

        // All water values should be in realistic range (5-8 cups)
        assertTrue(savedWater.all { it.cups in 5..8 },
            "All water intake values should be between 5 and 8 cups")
    }

    /**
     * Stubs the library creation methods to return deterministic domain objects.
     */
    private fun stubLibraryMethods() {
        coEvery { repository.createOrGetSymptomInLibrary("Cramps", SymptomCategory.PAIN) } returns
            buildSymptom(id = "cramps-id", name = "Cramps", category = SymptomCategory.PAIN)
        coEvery { repository.createOrGetSymptomInLibrary("Bloating", SymptomCategory.DIGESTIVE) } returns
            buildSymptom(id = "bloating-id", name = "Bloating", category = SymptomCategory.DIGESTIVE)
        coEvery { repository.createOrGetSymptomInLibrary("Headache", SymptomCategory.PAIN) } returns
            buildSymptom(id = "headache-id", name = "Headache", category = SymptomCategory.PAIN)
        coEvery { repository.createOrGetSymptomInLibrary("Fatigue", SymptomCategory.ENERGY) } returns
            buildSymptom(id = "fatigue-id", name = "Fatigue", category = SymptomCategory.ENERGY)
        coEvery { repository.createOrGetMedicationInLibrary("Ibuprofen") } returns
            buildMedication(id = "ibuprofen-id", name = "Ibuprofen")
        coEvery { repository.createCompletedPeriod(any(), any()) } returns
            com.veleda.cyclewise.testutil.buildPeriod()
    }
}
