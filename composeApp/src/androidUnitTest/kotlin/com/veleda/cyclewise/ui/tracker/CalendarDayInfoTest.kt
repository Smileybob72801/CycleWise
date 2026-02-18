package com.veleda.cyclewise.ui.tracker

import com.veleda.cyclewise.domain.models.CyclePhase
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CalendarDayInfoTest {

    @Test
    fun defaults_THEN_allFlagsAreFalse() {
        // GIVEN — default construction
        val info = CalendarDayInfo()

        // THEN — every boolean flag is false and phase is null
        assertFalse(info.isPeriodDay)
        assertFalse(info.hasSymptoms)
        assertFalse(info.hasMedications)
        assertFalse(info.hasNotes)
        assertNull(info.cyclePhase)
    }

    @Test
    fun construction_WHEN_hasNotesTrue_THEN_propertyReflects() {
        // GIVEN — construction with hasNotes = true
        val info = CalendarDayInfo(hasNotes = true)

        // THEN — hasNotes is true, other flags remain default
        assertTrue(info.hasNotes)
        assertFalse(info.isPeriodDay)
        assertFalse(info.hasSymptoms)
        assertFalse(info.hasMedications)
    }

    @Test
    fun construction_WHEN_allFieldsSet_THEN_allPropertiesReflect() {
        // GIVEN — construction with all fields explicitly set
        val info = CalendarDayInfo(
            isPeriodDay = true,
            hasSymptoms = true,
            hasMedications = true,
            hasNotes = true,
            cyclePhase = CyclePhase.OVULATION
        )

        // THEN — all properties match the supplied values
        assertTrue(info.isPeriodDay)
        assertTrue(info.hasSymptoms)
        assertTrue(info.hasMedications)
        assertTrue(info.hasNotes)
        assertEquals(CyclePhase.OVULATION, info.cyclePhase)
    }
}
