package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.PeriodRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class DebugSeederUseCaseTest {

    private lateinit var mockRepository: PeriodRepository
    private lateinit var useCase: DebugSeederUseCase

    @BeforeTest
    fun setUp() {
        mockRepository = mockk(relaxed = true)
        useCase = DebugSeederUseCase(mockRepository)
    }

    @Test
    fun invoke_WHEN_called_THEN_delegatesToRepositorySeedDatabaseForDebug() = runTest {
        // ACT
        useCase()

        // ASSERT
        coVerify(exactly = 1) { mockRepository.seedDatabaseForDebug() }
    }
}
