package com.veleda.cyclewise

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module

/**
 * A JUnit TestRule that handles the Koin lifecycle for tests.
 * It ensures that Koin is started before each test with the provided modules
 * and stopped after each test, preventing state leakage.
 */
class KoinTestRule(
    private val modules: List<Module>
) : TestRule {
    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                try {
                    // Start Koin before the test runs
                    startKoin {
                        modules(modules)
                    }
                    // Run the actual test
                    base.evaluate()
                } finally {
                    // Always stop Koin after the test, even if it fails
                    stopKoin()
                }
            }
        }
    }
}