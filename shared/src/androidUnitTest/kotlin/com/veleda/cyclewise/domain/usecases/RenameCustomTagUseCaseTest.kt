package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.testutil.buildCustomTag
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

/**
 * Unit tests for [RenameCustomTagUseCase].
 */
@OptIn(ExperimentalTime::class)
class RenameCustomTagUseCaseTest {

    private val mockRepository: PeriodRepository = mockk(relaxed = true)
    private val useCase = RenameCustomTagUseCase(mockRepository)

    private val workout = buildCustomTag(id = "t1", name = "Workout")
    private val meditation = buildCustomTag(id = "t2", name = "Meditation")
    private val library = listOf(workout, meditation)

    @Test
    fun `invoke WHEN newNameIsBlank THEN returnsBlankName`() = runTest {
        // WHEN
        val result = useCase("t1", "   ", library)

        // THEN
        assertIs<RenameResult.BlankName>(result)
        coVerify(exactly = 0) { mockRepository.renameCustomTag(any(), any()) }
    }

    @Test
    fun `invoke WHEN newNameConflicts THEN returnsNameAlreadyExists`() = runTest {
        // WHEN — try to rename Workout to "meditation" (conflicts with Meditation)
        val result = useCase("t1", "meditation", library)

        // THEN
        assertIs<RenameResult.NameAlreadyExists>(result)
        coVerify(exactly = 0) { mockRepository.renameCustomTag(any(), any()) }
    }

    @Test
    fun `invoke WHEN validName THEN callsRepositoryAndReturnsSuccess`() = runTest {
        // WHEN
        val result = useCase("t1", "Yoga", library)

        // THEN
        assertIs<RenameResult.Success>(result)
        coVerify(exactly = 1) { mockRepository.renameCustomTag("t1", "Yoga") }
    }

    @Test
    fun `invoke WHEN caseOnlyChange THEN returnsSuccess`() = runTest {
        // WHEN — rename "Workout" to "WORKOUT" (same ID, case-only change)
        val result = useCase("t1", "WORKOUT", library)

        // THEN — should not be a conflict since it's the same item
        assertIs<RenameResult.Success>(result)
        coVerify(exactly = 1) { mockRepository.renameCustomTag("t1", "WORKOUT") }
    }
}
