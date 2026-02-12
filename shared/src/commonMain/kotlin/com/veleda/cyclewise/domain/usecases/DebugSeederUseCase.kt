package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.PeriodRepository

/**
 * **Developer-only** use case that seeds the database with realistic test data.
 *
 * **Destructive:** invoking this permanently deletes all existing user data before
 * inserting generated cycles, daily logs, symptoms, and medications.
 */
class DebugSeederUseCase(private val periodRepository: PeriodRepository) {
    /**
     * Wipes all data and inserts 6 completed periods with daily entries spanning several months.
     */
    suspend operator fun invoke() {
        periodRepository.seedDatabaseForDebug()
    }
}