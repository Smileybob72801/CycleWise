package com.veleda.cyclewise.domain.models

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class DayDetailsTest {

    @Test
    fun defaults_THEN_hasNotesIsFalse() {
        // GIVEN — default construction
        val details = DayDetails()

        // THEN — hasNotes defaults to false
        assertFalse(details.hasNotes)
    }

    @Test
    fun copy_WHEN_hasNotesSetTrue_THEN_preservedInCopy() {
        // GIVEN — a default DayDetails
        val original = DayDetails()

        // WHEN — copied with hasNotes = true
        val copied = original.copy(hasNotes = true)

        // THEN — the copy has hasNotes = true and other defaults preserved
        assertTrue(copied.hasNotes)
        assertFalse(copied.isPeriodDay)
        assertFalse(copied.hasLoggedSymptoms)
        assertFalse(copied.hasLoggedMedications)
        assertEquals(null, copied.cyclePhase)
    }
}
