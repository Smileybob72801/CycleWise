package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.PeriodRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [DeleteMedicationUseCase].
 */
class DeleteMedicationUseCaseTest {

    private val mockRepository: PeriodRepository = mockk(relaxed = true)
    private val useCase = DeleteMedicationUseCase(mockRepository)

    @Test
    fun `getLogCount THEN delegatesToRepository`() = runTest {
        // GIVEN
        coEvery { mockRepository.getMedicationLogCount("m1") } returns 3

        // WHEN
        val count = useCase.getLogCount("m1")

        // THEN
        assertEquals(3, count)
        coVerify(exactly = 1) { mockRepository.getMedicationLogCount("m1") }
    }

    @Test
    fun `invoke THEN callsDeleteMedication`() = runTest {
        // WHEN
        useCase("m1")

        // THEN
        coVerify(exactly = 1) { mockRepository.deleteMedication("m1") }
    }
}
