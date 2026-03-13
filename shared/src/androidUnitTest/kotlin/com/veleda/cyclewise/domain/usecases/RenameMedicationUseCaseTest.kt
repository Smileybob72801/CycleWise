package com.veleda.cyclewise.domain.usecases

import com.veleda.cyclewise.domain.repository.PeriodRepository
import com.veleda.cyclewise.testutil.buildMedication
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertIs
import kotlin.time.ExperimentalTime

/**
 * Unit tests for [RenameMedicationUseCase].
 */
@OptIn(ExperimentalTime::class)
class RenameMedicationUseCaseTest {

    private val mockRepository: PeriodRepository = mockk(relaxed = true)
    private val useCase = RenameMedicationUseCase(mockRepository)

    private val ibuprofen = buildMedication(id = "m1", name = "Ibuprofen")
    private val aspirin = buildMedication(id = "m2", name = "Aspirin")
    private val library = listOf(ibuprofen, aspirin)

    @Test
    fun `invoke WHEN blankName THEN returnsBlankName`() = runTest {
        val result = useCase("m1", "   ", library)
        assertIs<RenameResult.BlankName>(result)
        coVerify(exactly = 0) { mockRepository.renameMedication(any(), any()) }
    }

    @Test
    fun `invoke WHEN caseInsensitiveConflict THEN returnsNameAlreadyExists`() = runTest {
        val result = useCase("m1", "aspirin", library)
        assertIs<RenameResult.NameAlreadyExists>(result)
        coVerify(exactly = 0) { mockRepository.renameMedication(any(), any()) }
    }

    @Test
    fun `invoke WHEN validName THEN callsRepositoryAndReturnsSuccess`() = runTest {
        val result = useCase("m1", "Naproxen", library)
        assertIs<RenameResult.Success>(result)
        coVerify(exactly = 1) { mockRepository.renameMedication("m1", "Naproxen") }
    }

    @Test
    fun `invoke WHEN sameIdDifferentCase THEN allowsRenameAndReturnsSuccess`() = runTest {
        val result = useCase("m1", "IBUPROFEN", library)
        assertIs<RenameResult.Success>(result)
        coVerify(exactly = 1) { mockRepository.renameMedication("m1", "IBUPROFEN") }
    }
}
