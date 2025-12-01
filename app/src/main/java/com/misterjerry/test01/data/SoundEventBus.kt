package com.misterjerry.test01.data

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object SoundEventBus {
    private val _soundEvents = MutableStateFlow<List<SoundEvent>>(emptyList())
    val soundEvents: StateFlow<List<SoundEvent>> = _soundEvents.asStateFlow()
    
    var isForeground: Boolean = false
    
    private val _eventFlow = MutableSharedFlow<SoundEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val eventFlow: SharedFlow<SoundEvent> = _eventFlow.asSharedFlow()

    fun addEvent(event: SoundEvent) {
        val currentEvents = _soundEvents.value
        val oneHourAgo = System.currentTimeMillis() - 3600000 // 1 hour in millis
        val updatedEvents = (listOf(event) + currentEvents).filter { it.id > oneHourAgo }
        _soundEvents.value = updatedEvents
        
        _eventFlow.tryEmit(event)
    }

    fun clearEvents() {
        _soundEvents.value = emptyList()
    }
    
    // Keep this for backward compatibility if needed, or remove if fully refactored.
    // For now, we'll focus on the list state.
    private val _classificationFlow = MutableSharedFlow<Pair<String, Float>>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val classificationFlow: SharedFlow<Pair<String, Float>> = _classificationFlow.asSharedFlow()

    fun emitEvent(label: String, direction: Float) {
        _classificationFlow.tryEmit(label to direction)
    }
}
