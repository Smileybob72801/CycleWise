package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.CycleLengthAverage
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CycleLengthAverageGeneratorTest {

    private lateinit var generator: CycleLengthAverageGenerator

    @BeforeTest
    fun setUp() {
        generator = CycleLengthAverageGenerator()
    }

    @Test
    fun generate_WHEN_averageCycleLengthIsNull_THEN_returnsEmptyList() {
        // ARRANGE
        val data = InsightData(
            allPeriods = emptyList(),
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = null,
            topSymptomsCount = 3
        )

        // ACT
        val result = generator.generate(data)

        // ASSERT
        assertTrue(result.isEmpty())
    }

    @Test
    fun generate_WHEN_averageCycleLengthIsAvailable_THEN_returnsCycleLengthAverageInsight() {
        // ARRANGE
        val data = InsightData(
            allPeriods = emptyList(),
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = 28.5,
            topSymptomsCount = 3
        )

        // ACT
        val result = generator.generate(data)

        // ASSERT
        assertEquals(1, result.size)
        val insight = result.first()
        assertIs<CycleLengthAverage>(insight)
        assertEquals(28.5, insight.averageDays)
    }

    @Test
    fun generate_WHEN_averageCycleLengthIsWholeNumber_THEN_returnsCycleLengthAverageWithCorrectValue() {
        // ARRANGE
        val data = InsightData(
            allPeriods = emptyList(),
            allLogs = emptyList(),
            symptomLibrary = emptyList(),
            averageCycleLength = 30.0,
            topSymptomsCount = 3
        )

        // ACT
        val result = generator.generate(data)

        // ASSERT
        assertEquals(1, result.size)
        val insight = result.first()
        assertIs<CycleLengthAverage>(insight)
        assertEquals(30.0, insight.averageDays)
        assertEquals("CYCLE_LENGTH_AVERAGE", insight.id)
        assertEquals(100, insight.priority)
    }
}
