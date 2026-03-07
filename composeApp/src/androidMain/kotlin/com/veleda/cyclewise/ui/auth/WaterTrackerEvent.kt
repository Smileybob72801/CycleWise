package com.veleda.cyclewise.ui.auth

/**
 * Defines all user interactions for the pre-authentication water tracker on the lock screen.
 */
sealed interface WaterTrackerEvent {
    /** The user tapped the "+" button to add a cup of water. */
    data object Increment : WaterTrackerEvent

    /** The user tapped the "−" button to remove a cup of water. */
    data object Decrement : WaterTrackerEvent
}
