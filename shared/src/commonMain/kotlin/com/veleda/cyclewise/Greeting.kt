package com.veleda.cyclewise

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class Greeting {
    private val platform = getPlatform()
    private val rocketComponent = RocketComponent()

    fun greet(): Flow<String> = flow {
        emit (if (Random.nextBoolean()) "Hi!" else "Yo!")

        emit ("Guess what this is! > ${platform.name.reversed()}!")

        emit (daysPhrase())

        emit(rocketComponent.launchPhrase())

        emit("Hello my ragtime gal!")
    }
}