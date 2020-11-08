package com.example.flowdelegate.internal

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.onCompletion


internal fun <T> Flow<T>.concatWith(nextFlow: Flow<T>) = this.onCompletion {
    emitAll(nextFlow)
}