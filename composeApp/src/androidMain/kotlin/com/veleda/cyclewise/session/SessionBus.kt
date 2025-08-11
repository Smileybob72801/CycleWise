package com.veleda.cyclewise.session

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Tiny event bus for session-level signals (e.g., logout).
 * Keeps UI decoupled from lifecycle observers and DI internals.
 */
class SessionBus {
    private val _logout = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val logout: SharedFlow<Unit> = _logout

    fun emitLogout() {
        _logout.tryEmit(Unit)
    }
}