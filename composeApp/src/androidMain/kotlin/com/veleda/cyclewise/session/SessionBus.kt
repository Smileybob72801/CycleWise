package com.veleda.cyclewise.session

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Singleton event bus for session-level signals (e.g., logout).
 *
 * Keeps UI decoupled from lifecycle observers and DI internals. Collectors on the
 * [logout] flow are notified when the session scope should be torn down.
 *
 * @property logout [SharedFlow] with replay = 0 and capacity 1 (drops oldest on overflow).
 *                  Emits [Unit] when the user logs out or autolock triggers.
 */
class SessionBus {
    private val _logout = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val logout: SharedFlow<Unit> = _logout

    /** Fires a logout signal. Non-suspending (uses [MutableSharedFlow.tryEmit]). */
    fun emitLogout() {
        _logout.tryEmit(Unit)
    }
}