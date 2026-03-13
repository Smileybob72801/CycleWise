package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.testutil.buildSymptom
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

/**
 * Unit tests for [RenameSymptomUseCase].
 */
@OptIn(ExperimentalTime::class)
class RenameSymptomUseCaseTest {

    private val mockRepository: PeriodRepository = mockk(relaxed = true)
    private val useCase = RenameSymptomUseCase(mockRepository)

    private val headache = buildSymptom(id = "s1", name = "Headache")
    private val cramps = buildSymptom(id = "s2", name = "Cramps")
    private val library = listOf(headache, cramps)

    @Test
    fun `invoke WHEN blankName THEN returnsBlankName`() = runTest {
        // WHEN
        val result = useCase("s1", "   ", library)

        // THEN
        assertIs<RenameResult.BlankName>(result)
        coVerify(exactly = 0) { mockRepository.renameSymptom(any(), any()) }
    }

    @Test
    fun `invoke WHEN caseInsensitiveConflict THEN returnsNameAlreadyExists`() = runTest {
        // WHEN — try to rename Headache to "cramps" (conflicts with Cramps)
        val result = useCase("s1", "cramps", library)

        // THEN
        assertIs<RenameResult.NameAlreadyExists>(result)
        coVerify(exactly = 0) { mockRepository.renameSymptom(any(), any()) }
    }

    @Test
    fun `invoke WHEN validName THEN callsRepositoryAndReturnsSuccess`() = runTest {
        // WHEN
        val result = useCase("s1", "Migraine", library)

        // THEN
        assertIs<RenameResult.Success>(result)
        coVerify(exactly = 1) { mockRepository.renameSymptom("s1", "Migraine") }
    }

    @Test
    fun `invoke WHEN sameIdDifferentCase THEN allowsRenameAndReturnsSuccess`() = runTest {
        // WHEN — rename "Headache" to "HEADACHE" (same ID, case-only change)
        val result = useCase("s1", "HEADACHE", library)

        // THEN — should not be a conflict since it's the same item
        assertIs<RenameResult.Success>(result)
        coVerify(exactly = 1) { mockRepository.renameSymptom("s1", "HEADACHE") }
    }

    @Test
    fun `invoke WHEN nameHasLeadingTrailingSpaces THEN trims`() = runTest {
        // WHEN
        val result = useCase("s1", "  Migraine  ", library)

        // THEN
        assertIs<RenameResult.Success>(result)
        coVerify(exactly = 1) { mockRepository.renameSymptom("s1", "Migraine") }
    }
}
