package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.PeriodRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [DeleteCustomTagUseCase].
 */
class DeleteCustomTagUseCaseTest {

    private val mockRepository: PeriodRepository = mockk(relaxed = true)
    private val useCase = DeleteCustomTagUseCase(mockRepository)

    @Test
    fun `getLogCount WHEN called THEN delegatesToRepository`() = runTest {
        // GIVEN
        coEvery { mockRepository.getCustomTagLogCount("t1") } returns 5

        // WHEN
        val count = useCase.getLogCount("t1")

        // THEN
        assertEquals(5, count)
        coVerify(exactly = 1) { mockRepository.getCustomTagLogCount("t1") }
    }

    @Test
    fun `invoke WHEN called THEN delegatesToRepository`() = runTest {
        // WHEN
        useCase("t1")

        // THEN
        coVerify(exactly = 1) { mockRepository.deleteCustomTag("t1") }
    }
}
