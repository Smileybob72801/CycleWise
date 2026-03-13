package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.PeriodRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [DeleteSymptomUseCase].
 */
class DeleteSymptomUseCaseTest {

    private val mockRepository: PeriodRepository = mockk(relaxed = true)
    private val useCase = DeleteSymptomUseCase(mockRepository)

    @Test
    fun `getLogCount THEN delegatesToRepository`() = runTest {
        // GIVEN
        coEvery { mockRepository.getSymptomLogCount("s1") } returns 5

        // WHEN
        val count = useCase.getLogCount("s1")

        // THEN
        assertEquals(5, count)
        coVerify(exactly = 1) { mockRepository.getSymptomLogCount("s1") }
    }

    @Test
    fun `invoke THEN callsDeleteSymptom`() = runTest {
        // WHEN
        useCase("s1")

        // THEN
        coVerify(exactly = 1) { mockRepository.deleteSymptom("s1") }
    }
}
