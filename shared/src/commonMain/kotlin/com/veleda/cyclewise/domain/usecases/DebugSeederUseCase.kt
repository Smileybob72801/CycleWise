package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.PeriodRepository

/**
 * A developer-only use case to seed the database with realistic test data.
 */
class DebugSeederUseCase(private val periodRepository: PeriodRepository) {
    /**
     * Deletes all existing user data and seeds the database with several
     * months of generated cycles and daily logs.
     */
    suspend operator fun invoke() {
        periodRepository.seedDatabaseForDebug()
    }
}