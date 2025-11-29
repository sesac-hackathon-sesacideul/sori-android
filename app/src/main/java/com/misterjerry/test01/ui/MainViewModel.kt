package com.misterjerry.test01.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.misterjerry.test01.data.ConversationItem
import com.misterjerry.test01.data.ConversationRepository
import com.misterjerry.test01.data.SoundEvent
import com.misterjerry.test01.data.SoundRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MainUiState(
    val soundEvents: List<SoundEvent> = emptyList(),
    val conversationHistory: List<ConversationItem> = emptyList()
)

class MainViewModel : ViewModel() {
    private val soundRepository = SoundRepository()
    private val conversationRepository = ConversationRepository()

    private val _conversationHistory = MutableStateFlow<List<ConversationItem>>(emptyList())

    val uiState: StateFlow<MainUiState> = combine(
        soundRepository.getSoundEvents(),
        conversationRepository.getConversationStream(),
        _conversationHistory
    ) { sounds, newConversationItem, history ->
        // Append new conversation item to history if it's new (simple check by ID)
        val updatedHistory = if (history.none { it.id == newConversationItem.id }) {
            history + newConversationItem
        } else {
            history
        }
        
        // Keep history size manageable
        val trimmedHistory = if (updatedHistory.size > 50) updatedHistory.takeLast(50) else updatedHistory
        
        // Side effect: update the history flow (a bit hacky for this simple combine, but works for mock)
        if (updatedHistory != history) {
            _conversationHistory.value = trimmedHistory
        }

        MainUiState(
            soundEvents = sounds,
            conversationHistory = trimmedHistory
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )
}
