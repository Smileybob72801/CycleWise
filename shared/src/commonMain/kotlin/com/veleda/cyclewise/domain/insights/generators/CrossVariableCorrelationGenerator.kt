package com.veleda.cyclewise.domain.insights.generators

import com.veleda.cyclewise.domain.insights.CrossVariableCorrelation
import com.veleda.cyclewise.domain.insights.Insight
import com.veleda.cyclewise.domain.insights.analysis.CorrelationEngine

/**
 * Generates cross-variable correlation insights using the [CorrelationEngine].
 *
 * Runs Spearman rank correlations on all metric pairs and produces one
 * [CrossVariableCorrelation] insight per significant finding.
 */
class CrossVariableCorrelationGenerator(
    private val correlationEngine: CorrelationEngine = CorrelationEngine(),
) : InsightGenerator {

    override fun generate(data: InsightData): List<Insight> {
        return correlationEngine.computeAllCorrelations(
            logs = data.allLogs,
            waterIntakes = data.waterIntakes,
        ).map { result ->
            CrossVariableCorrelation(
                variableA = result.variableA,
                variableB = result.variableB,
                direction = result.direction,
                strength = result.strength,
                coefficient = result.coefficient,
                sampleSize = result.sampleSize,
            )
        }
    }
}
